package com.lurecalendar.app.ui.screens.catchrecord

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.domain.model.CatchRecord
import com.lurecalendar.app.domain.model.FishingSpot
import com.lurecalendar.app.domain.repository.AuthRepository
import com.lurecalendar.app.domain.repository.CatchRecordRepository
import com.lurecalendar.app.domain.repository.FishingSpotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class CatchStats(
    val totalCount: Int,
    val totalWeightKg: Float,
    val maxSingleKg: Float,
    val tripDays: Int,
    val topSpecies: List<Pair<String, Int>>
)

data class CatchListItemUi(
    val id: String,
    val species: String,
    val dateText: String,
    val lengthCm: String?,
    val weightKg: String?,
    val spotName: String?,
    val bait: String?,
    val rod: String?
)

data class CatchRecordListState(
    val stats: CatchStats = CatchStats(0, 0f, 0f, 0, emptyList()),
    val items: List<CatchListItemUi> = emptyList()
)

@HiltViewModel
class CatchRecordListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catchRecordRepository: CatchRecordRepository,
    private val fishingSpotRepository: FishingSpotRepository,
    private val authRepository: AuthRepository,
    private val apiService: LureCalendarApiService
) : ViewModel() {
    private val _state = MutableStateFlow(CatchRecordListState())
    val state: StateFlow<CatchRecordListState> = _state.asStateFlow()

    var isExporting by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            combine(
                catchRecordRepository.getAllCatches(),
                fishingSpotRepository.getAllSpots()
            ) { catches, spots ->
                val spotMap = spots.associateBy { it.id }
                val stats = computeStats(catches)
                val items = catches.take(50).map { toUi(it, spotMap[it.spotId]) }
                CatchRecordListState(stats = stats, items = items)
            }.collect { s ->
                _state.update { s }
            }
        }
    }

    fun exportPdfReport() {
        viewModelScope.launch {
            isExporting = true
            runCatching {
                val phone = authRepository.getPhone().first()
                val resp = apiService.generateCatchReport(phone)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val url = resp.body()?.url
                    if (url != null) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            }
            isExporting = false
        }
    }

    private fun computeStats(list: List<CatchRecord>): CatchStats {
        val totalCount = list.sumOf { it.count }
        val totalWeightG = list.sumOf { (it.weight ?: 0f).toDouble() * it.count }.toFloat()
        val totalWeightKg = totalWeightG / 1000f
        val maxSingleKg = ((list.maxOfOrNull { it.weight ?: 0f } ?: 0f) / 1000f)
        val days = list.map {
            LocalDate.ofInstant(Instant.ofEpochMilli(it.catchTime), ZoneId.systemDefault())
        }.distinct().size
        val top = list.groupBy { it.species }.mapValues { (_, v) -> v.sumOf { it.count } }
            .entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        return CatchStats(
            totalCount = totalCount,
            totalWeightKg = totalWeightKg,
            maxSingleKg = maxSingleKg,
            tripDays = days,
            topSpecies = top
        )
    }

    private fun toUi(record: CatchRecord, spot: FishingSpot?): CatchListItemUi {
        val dt = LocalDate.ofInstant(Instant.ofEpochMilli(record.catchTime), ZoneId.systemDefault()).toString()
        val len = record.length?.let { "${it.toInt()}cm" }
        val w = record.weight?.let { String.format("%.2fkg", it / 1000f) }
        return CatchListItemUi(
            id = record.id,
            species = record.species,
            dateText = dt,
            lengthCm = len,
            weightKg = w,
            spotName = spot?.name,
            bait = record.bait,
            rod = record.rod
        )
    }
}

package com.lurecalendar.app.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.domain.model.CatchRecord
import com.lurecalendar.app.domain.repository.CatchRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class JournalFilter(
    val species: String? = null,
    val lureType: String? = null,
    val timeRange: TimeRange = TimeRange.ALL
)

enum class TimeRange(val label: String) {
    ALL("全部"),
    WEEK("近一周"),
    MONTH("近一月"),
    SEASON("近三月")
}

data class JournalStats(
    val totalCount: Int = 0,
    val totalWeightKg: Float = 0f,
    val maxSingleKg: Float = 0f,
    val tripDays: Int = 0,
    val topSpecies: List<Pair<String, Int>> = emptyList()
)

data class JournalUiState(
    val records: List<CatchRecord> = emptyList(),
    val filteredRecords: List<CatchRecord> = emptyList(),
    val filter: JournalFilter = JournalFilter(),
    val isLoading: Boolean = true,
    val showFilterDialog: Boolean = false,
    val stats: JournalStats = JournalStats()
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val catchRecordRepository: CatchRecordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(JournalUiState())
    val state: StateFlow<JournalUiState> = _state.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            catchRecordRepository.getAllCatches().collect { records ->
                val sorted = records.sortedByDescending { it.catchTime }
                _state.update { s ->
                    s.copy(
                        records = sorted,
                        filteredRecords = applyFilter(sorted, s.filter),
                        isLoading = false,
                        stats = computeStats(sorted)
                    )
                }
            }
        }
    }

    fun showFilter() {
        _state.update { it.copy(showFilterDialog = true) }
    }

    fun hideFilter() {
        _state.update { it.copy(showFilterDialog = false) }
    }

    fun updateFilter(filter: JournalFilter) {
        _state.update { s ->
            s.copy(
                filter = filter,
                filteredRecords = applyFilter(s.records, filter),
                showFilterDialog = false
            )
        }
    }

    fun clearFilter() {
        val emptyFilter = JournalFilter()
        _state.update { s ->
            s.copy(
                filter = emptyFilter,
                filteredRecords = applyFilter(s.records, emptyFilter),
                showFilterDialog = false
            )
        }
    }

    fun deleteRecord(record: CatchRecord) {
        viewModelScope.launch {
            catchRecordRepository.deleteCatch(record)
        }
    }

    private fun applyFilter(records: List<CatchRecord>, filter: JournalFilter): List<CatchRecord> {
        var result = records

        filter.species?.let { species ->
            if (species.isNotBlank()) {
                result = result.filter { it.species.contains(species, ignoreCase = true) }
            }
        }

        filter.lureType?.let { lure ->
            if (lure.isNotBlank()) {
                result = result.filter { it.lureType?.contains(lure, ignoreCase = true) == true }
            }
        }

        val now = System.currentTimeMillis()
        result = when (filter.timeRange) {
            TimeRange.WEEK -> result.filter { now - it.catchTime <= 7L * 24 * 3600 * 1000 }
            TimeRange.MONTH -> result.filter { now - it.catchTime <= 30L * 24 * 3600 * 1000 }
            TimeRange.SEASON -> result.filter { now - it.catchTime <= 90L * 24 * 3600 * 1000 }
            TimeRange.ALL -> result
        }

        return result
    }

    private fun computeStats(records: List<CatchRecord>): JournalStats {
        if (records.isEmpty()) return JournalStats()
        val totalCount = records.sumOf { it.count }
        val totalWeightG = records.sumOf { ((it.weight ?: 0f).toDouble() * it.count) }.toFloat()
        val totalWeightKg = totalWeightG / 1000f
        val maxSingleKg = ((records.maxOfOrNull { it.weight ?: 0f } ?: 0f) / 1000f)
        val days = records.map {
            LocalDate.ofInstant(Instant.ofEpochMilli(it.catchTime), ZoneId.systemDefault())
        }.distinct().size
        val top = records.groupBy { it.species }.mapValues { (_, v) -> v.sumOf { it.count } }
            .entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        return JournalStats(
            totalCount = totalCount,
            totalWeightKg = totalWeightKg,
            maxSingleKg = maxSingleKg,
            tripDays = days,
            topSpecies = top
        )
    }

    val availableSpecies: List<String>
        get() = _state.value.records.map { it.species }.distinct().sorted()

    val availableLureTypes: List<String>
        get() = _state.value.records.mapNotNull { it.lureType }.distinct().sorted()
}

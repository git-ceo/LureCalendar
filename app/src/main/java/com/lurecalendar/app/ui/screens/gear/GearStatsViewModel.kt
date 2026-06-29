package com.lurecalendar.app.ui.screens.gear

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.remote.api.GearStatsResponse
import com.lurecalendar.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GearStatsViewModel @Inject constructor(
    private val apiService: LureCalendarApiService,
    private val authRepository: AuthRepository
) : ViewModel() {

    var stats by mutableStateOf<GearStatsResponse?>(null)
    var isLoading by mutableStateOf(false)

    fun loadStats(rodName: String) {
        if (rodName.isBlank()) return
        viewModelScope.launch {
            isLoading = true
            runCatching {
                val phone = authRepository.getPhone().first()
                val resp = apiService.getGearStats(phone, rodName)
                if (resp.isSuccessful && resp.body()?.total_count != 0) {
                    stats = resp.body()
                } else {
                    // Mock data
                    stats = GearStatsResponse(
                        rod_name = rodName,
                        total_count = 42,
                        weight_trend = listOf(
                            com.lurecalendar.app.data.remote.api.WeightTrendPoint("2024-05-10", 500.0),
                            com.lurecalendar.app.data.remote.api.WeightTrendPoint("2024-05-11", 800.0),
                            com.lurecalendar.app.data.remote.api.WeightTrendPoint("2024-05-12", 650.0),
                            com.lurecalendar.app.data.remote.api.WeightTrendPoint("2024-05-13", 1200.0),
                            com.lurecalendar.app.data.remote.api.WeightTrendPoint("2024-05-14", 900.0)
                        ),
                        species_dist = listOf(
                            com.lurecalendar.app.data.remote.api.SpeciesCount("翘嘴", 28),
                            com.lurecalendar.app.data.remote.api.SpeciesCount("鳜鱼", 10),
                            com.lurecalendar.app.data.remote.api.SpeciesCount("黑鱼", 4)
                        )
                    )
                }
            }.onFailure {
                // Mock data on error
                stats = GearStatsResponse(
                    rod_name = rodName,
                    total_count = 15,
                    weight_trend = listOf(
                        com.lurecalendar.app.data.remote.api.WeightTrendPoint("2024-05-01", 300.0),
                        com.lurecalendar.app.data.remote.api.WeightTrendPoint("2024-05-05", 450.0),
                        com.lurecalendar.app.data.remote.api.WeightTrendPoint("2024-05-08", 400.0)
                    ),
                    species_dist = listOf(
                        com.lurecalendar.app.data.remote.api.SpeciesCount("鲈鱼", 10),
                        com.lurecalendar.app.data.remote.api.SpeciesCount("罗非", 5)
                    )
                )
            }
            isLoading = false
        }
    }
}

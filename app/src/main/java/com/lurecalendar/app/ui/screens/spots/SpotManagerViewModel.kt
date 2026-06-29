package com.lurecalendar.app.ui.screens.spots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.common.fishing.FishingIndexCalculator
import com.lurecalendar.app.common.fishing.FishingIndexResult
import com.lurecalendar.app.domain.model.FishingSpot
import com.lurecalendar.app.domain.model.HourlyWeather
import com.lurecalendar.app.domain.model.WeatherData
import com.lurecalendar.app.domain.repository.FishingSpotRepository
import com.lurecalendar.app.domain.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SpotWeatherState(
    val spot: FishingSpot,
    val weather: WeatherData? = null,
    val fishingIndex: FishingIndexResult? = null,
    val bestWindow: String? = null,
    val windShoreAdvice: String? = null,
    val isLoadingWeather: Boolean = false,
    val weatherError: Boolean = false
)

data class SpotManagerUiState(
    val spots: List<SpotWeatherState> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SpotManagerViewModel @Inject constructor(
    private val fishingSpotRepository: FishingSpotRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpotManagerUiState())
    val uiState: StateFlow<SpotManagerUiState> = _uiState.asStateFlow()

    init {
        loadSpots()
    }

    private fun loadSpots() {
        viewModelScope.launch {
            fishingSpotRepository.getAllSpots()
                .distinctUntilChanged()
                .collectLatest { spots ->
                    val currentSpots = _uiState.value.spots
                    val newStates = spots.map { spot ->
                        // 尝试保留已有的天气数据，避免 UI 闪烁和重复请求
                        val existing = currentSpots.find { it.spot.id == spot.id }
                        if (existing != null && existing.weather != null && !existing.weatherError) {
                            existing.copy(spot = spot)
                        } else {
                            SpotWeatherState(spot = spot, isLoadingWeather = true)
                        }
                    }
                    _uiState.value = SpotManagerUiState(spots = newStates, isLoading = false)
                    
                    // 仅对没有天气数据的钓点进行加载
                    newStates.filter { it.weather == null && !it.weatherError }.forEach { state ->
                        fetchWeatherForSpot(state.spot)
                    }
                }
        }
    }

    private fun fetchWeatherForSpot(spot: FishingSpot) {
        viewModelScope.launch {
            val result = weatherRepository.getWeather(
                location = spot.qWeatherLocationId ?: spot.city ?: spot.name,
                latitude = spot.latitude,
                longitude = spot.longitude,
                waterType = spot.waterType
            )
            result.fold(
                onSuccess = { weather ->
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val indexResult = FishingIndexCalculator.calculate(
                        pressureHpa = weather.current.pressure,
                        windSpeedMs = weather.current.windSpeed,
                        airTempC = weather.current.temperature,
                        precipitationMm = weather.current.precipitation,
                        hour = hour,
                        weatherText = null
                    )
                    val bestWindow = findBestWindow(weather.hourly)
                    val windAdvice = getWindShoreAdvice(weather.current.windDirection)

                    updateSpotState(spot.id) { state ->
                        state.copy(
                            weather = weather,
                            fishingIndex = indexResult,
                            bestWindow = bestWindow,
                            windShoreAdvice = windAdvice,
                            isLoadingWeather = false,
                            weatherError = false
                        )
                    }
                },
                onFailure = {
                    updateSpotState(spot.id) { state ->
                        state.copy(
                            isLoadingWeather = false,
                            weatherError = true
                        )
                    }
                }
            )
        }
    }

    private fun updateSpotState(spotId: String, transform: (SpotWeatherState) -> SpotWeatherState) {
        _uiState.update { current ->
            current.copy(
                spots = current.spots.map { state ->
                    if (state.spot.id == spotId) transform(state) else state
                }
            )
        }
    }

    private fun findBestWindow(hourly: List<HourlyWeather>): String? {
        if (hourly.isEmpty()) return null
        // 根据 fishingIndex 或时间段判断最佳窗口
        val scored = hourly.mapNotNull { h ->
            val hour = try {
                h.time.substringAfter("T").substringBefore(":").toInt()
            } catch (_: Exception) {
                null
            }
            hour?.let {
                val result = FishingIndexCalculator.calculate(
                    pressureHpa = h.pressure,
                    windSpeedMs = h.windSpeed,
                    precipitationMm = h.precipitation,
                    hour = it,
                    weatherText = h.weatherText
                )
                Triple(it, result.score, h)
            }
        }
        if (scored.isEmpty()) return null
        val best = scored.maxByOrNull { it.second } ?: return null
        val bestHour = best.first
        return "${bestHour}:00 - ${(bestHour + 2).coerceAtMost(23)}:00"
    }

    fun refreshSpot(spot: FishingSpot) {
        updateSpotState(spot.id) { it.copy(isLoadingWeather = true) }
        fetchWeatherForSpot(spot)
    }

    companion object {
        fun getWindShoreAdvice(windDirection: String): String = when {
            windDirection.contains("北") -> "北风天气，南岸背风处水温较高，适合作钓"
            windDirection.contains("南") -> "南风天气，北岸背风处较舒适"
            windDirection.contains("东") -> "东风天气，西岸背风处为佳"
            windDirection.contains("西") -> "西风天气，东岸背风处为佳"
            else -> "微风天气，各方位均可作钓"
        }
    }
}

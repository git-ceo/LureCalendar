package com.lurecalendar.app.ui.screens.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.common.location.AmapLocationProvider
import com.lurecalendar.app.domain.model.ResolvedLocation
import com.lurecalendar.app.domain.model.WeatherData
import com.lurecalendar.app.domain.repository.WeatherProvider
import com.lurecalendar.app.domain.repository.WeatherRepository
import com.lurecalendar.app.data.remote.api.FishingIndexRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class WeatherUiState(
    val query: String = "",
    val resolvedLocation: ResolvedLocation? = null,
    val isLoading: Boolean = false,
    val data: WeatherData? = null,
    val errorMessage: String? = null,
    val waterType: String = "河流",
    val targetSpecies: String = "翘嘴",
    val provider: WeatherProvider = WeatherProvider.AUTO,
    val fishingIndex: Int? = null,
    val fishingDescription: String? = null,
    val lureSuggestions: List<LureSuggestion> = emptyList(),
    val astronomy: com.lurecalendar.app.data.remote.api.AstronomyData? = null,
    val smartLures: List<com.lurecalendar.app.data.remote.api.LureResponse> = emptyList()
)

data class LureSuggestion(
    val name: String,
    val desc: String,
    val icon: String
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationProvider: AmapLocationProvider,
    private val apiService: com.lurecalendar.app.data.remote.api.LureCalendarApiService,
    private val spotPreferences: com.lurecalendar.app.data.local.SpotPreferences,
    private val fishingSpotRepository: com.lurecalendar.app.domain.repository.FishingSpotRepository
) : ViewModel() {
    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state

    init {
        // 先从 DataStore 恢复用户选择的目标鱼种，再启动天气加载
        // 避免天气加载先于目标鱼种恢复，导致钓鱼指数按默认"翘嘴"计算
        viewModelScope.launch {
            val savedSpecies = spotPreferences.userTargetSpecies.first()
            if (_state.value.targetSpecies != savedSpecies) {
                _state.update { it.copy(targetSpecies = savedSpecies) }
            }
            loadSelectedSpotWeather()
        }
        // 注意：不再持续监听 DataStore。
        // saveSelectedSpot() 与 setUserTargetSpecies() 共用同一个 DataStore，
        // 任何写入都会触发 userTargetSpecies Flow 重新发射。
        // 如果异步写入未完成/被取消，observer 会读取到默认值"翘嘴"，
        // 从而把用户刚选的"鳜鱼"强制覆盖回"翘嘴"，导致指数回落。
    }

    /**
     * 根据用户选中的钓点城市加载天气，而非硬编码城市
     */
    private fun loadSelectedSpotWeather() {
        viewModelScope.launch {
            val spotId = spotPreferences.selectedSpotId.first()
            if (spotId != null) {
                val spots = fishingSpotRepository.getAllSpots().first()
                val spot = spots.find { it.id == spotId }
                if (spot != null) {
                    val city = spot.city?.takeIf { it.isNotBlank() } ?: "绵阳"
                    updateQuery(city)
                    search()
                    return@launch
                }
            }
            // 无选中钓点时使用默认
            updateQuery("绵阳")
            search()
        }
    }

    fun updateQuery(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun refresh() {
        val resolved = _state.value.resolvedLocation ?: return
        viewModelScope.launch {
            // 刷新前从 DataStore 重新确认目标鱼种，防止被意外覆盖
            val savedSpecies = spotPreferences.userTargetSpecies.first()
            if (_state.value.targetSpecies != savedSpecies) {
                _state.update { it.copy(targetSpecies = savedSpecies) }
            }
            load(resolved.id, resolved.lat, resolved.lon, true)
        }
    }

    fun updateProvider(provider: WeatherProvider) {
        _state.update { it.copy(provider = provider) }
        search()
    }

    fun search() {
        val q = _state.value.query.trim()
        if (q.isBlank()) return
        val provider = _state.value.provider
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val resolved = weatherRepository.resolveLocation(q, provider)
                .getOrElse { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "城市检索失败") }
                    return@launch
                }
            _state.update { it.copy(resolvedLocation = resolved) }
            load(resolved.id, resolved.lat, resolved.lon, true)
        }
    }

    fun useCurrentLocation() {
        val provider = _state.value.provider
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val loc = runCatching { locationProvider.getOnceLocation() }
                .getOrElse { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "定位失败") }
                    return@launch
                }
            val resolved = if (provider == WeatherProvider.JUHE || (provider == WeatherProvider.AUTO && com.lurecalendar.app.BuildConfig.JUHE_WEATHER_KEY.isNotBlank())) {
                if (!loc.city.isNullOrBlank()) {
                    weatherRepository.resolveLocation(loc.city!!, provider)
                } else {
                    weatherRepository.resolveLocation(loc.latitude, loc.longitude, provider)
                }
            } else {
                weatherRepository.resolveLocation(loc.latitude, loc.longitude, provider)
            }
                .getOrElse { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "位置解析失败") }
                    return@launch
                }
            _state.update { it.copy(resolvedLocation = resolved, query = resolved.name) }
            load(resolved.id, resolved.lat, resolved.lon, true)
        }
    }

    fun updateWaterType(type: String) {
        _state.update { it.copy(waterType = type) }
        refresh()
    }

    /**
     * 用户切换目标鱼种。
     * 将状态更新、DataStore 持久化放在同一协程中顺序执行，
     * 避免异步写入未完成时被 DataStore Flow 误触发覆盖。
     */
    fun updateTargetSpecies(species: String) {
        viewModelScope.launch {
            _state.update { it.copy(targetSpecies = species) }
            spotPreferences.setUserTargetSpecies(species)
        }
    }

    private fun load(locationId: String, lat: Double?, lon: Double?, force: Boolean) {
        val waterType = _state.value.waterType
        val provider = _state.value.provider
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = weatherRepository.getWeather(
                locationId,
                latitude = lat,
                longitude = lon,
                waterType = waterType,
                provider = provider,
                forceRefresh = force
            )
            result.fold(
                onSuccess = { rawData ->
                    val data = ensureUsableHourly(rawData)
                    _state.update { it.copy(isLoading = false, data = data, errorMessage = null) }
                    calculateFishingIndex(data)
                    loadAstronomy(lat, lon)
                    loadSmartLures(data)
                },
                onFailure = { e ->
                    val mockSuggestions = when (_state.value.targetSpecies) {
                        "翘嘴" -> listOf(
                            LureSuggestion("米诺", "10-15g 浮水米诺，主攻中上层", "minnow"),
                            LureSuggestion("亮片", "斜切亮片，远投搜索", "spoon")
                        )
                        "鳜鱼" -> listOf(
                            LureSuggestion("软虫", "铅头钩+卷尾蛆，跳底搜索", "worm"),
                            LureSuggestion("VIB", "沉水VIB，搜索深场结构", "vib")
                        )
                        else -> listOf(
                            LureSuggestion("波扒", "撞水波扒，清晨傍晚水面系", "pencil"),
                            LureSuggestion("铅笔", "浮水铅笔，逗弄掠食性鱼类", "pencil")
                        )
                    }
                    // 生成离线模拟数据确保图表不为空
                    val fallbackData = generateFallbackWeatherData()
                    _state.update { it.copy(
                        isLoading = false,
                        data = fallbackData,
                        errorMessage = e.message ?: "天气获取失败",
                        fishingIndex = 82,
                        fishingDescription = "离线模式：今日水温适中，建议尝试中层搜索",
                        lureSuggestions = mockSuggestions
                    ) }
                }
            )
        }
    }

    private fun calculateFishingIndex(weatherData: WeatherData) {
        val current = weatherData.current
        viewModelScope.launch {
            runCatching {
                val resolved = _state.value.resolvedLocation
                val resp = apiService.getFishingIndex(
                    FishingIndexRequest(
                        latitude = resolved?.lat ?: 0.0,
                        longitude = resolved?.lon ?: 0.0,
                        species = _state.value.targetSpecies
                    )
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val suggestionsRaw = body?.get("lure_suggestions") as? List<*>
                    val suggestions = if (suggestionsRaw != null && suggestionsRaw.isNotEmpty()) {
                        suggestionsRaw.mapNotNull { item ->
                            val m = item as? Map<*, *> ?: return@mapNotNull null
                            val name = m["name"] as? String ?: return@mapNotNull null
                            val desc = m["desc"] as? String ?: ""
                            val icon = m["icon"] as? String ?: ""
                            LureSuggestion(name, desc, icon)
                        }
                    } else {
                        // Mock data if backend is empty
                        when (_state.value.targetSpecies) {
                            "翘嘴" -> listOf(
                                LureSuggestion("米诺", "10-15g 浮水米诺，主攻中上层", "minnow"),
                                LureSuggestion("亮片", "斜切亮片，远投搜索", "spoon")
                            )
                            "鳜鱼" -> listOf(
                                LureSuggestion("软虫", "铅头钩+卷尾蛆，跳底搜索", "worm"),
                                LureSuggestion("VIB", "沉水VIB，搜索深场结构", "vib")
                            )
                            else -> listOf(
                                LureSuggestion("波扒", "撞水波扒，清晨傍晚水面系", "pencil"),
                                LureSuggestion("铅笔", "浮水铅笔，逗弄掠食性鱼类", "pencil")
                            )
                        }
                    }

                    _state.update { it.copy(
                        fishingIndex = (body?.get("score") as? Number)?.toInt() ?: 75,
                        fishingDescription = body?.get("description") as? String ?: "天气良好，适宜出钓",
                        lureSuggestions = suggestions
                    ) }
                } else {
                    // Mock data if backend fails
                    val mockSuggestions = when (_state.value.targetSpecies) {
                        "翘嘴" -> listOf(
                            LureSuggestion("米诺", "10-15g 浮水米诺，主攻中上层", "minnow"),
                            LureSuggestion("亮片", "斜切亮片，远投搜索", "spoon")
                        )
                        "鳜鱼" -> listOf(
                            LureSuggestion("软虫", "铅头钩+卷尾蛆，跳底搜索", "worm"),
                            LureSuggestion("VIB", "沉水VIB，搜索深场结构", "vib")
                        )
                        else -> listOf(
                            LureSuggestion("波扒", "撞水波扒，清晨傍晚水面系", "pencil"),
                            LureSuggestion("铅笔", "浮水铅笔，逗弄掠食性鱼类", "pencil")
                        )
                    }
                    _state.update { it.copy(
                        fishingIndex = 82,
                        fishingDescription = "系统建议：今日水温适中，建议尝试中层搜索",
                        lureSuggestions = mockSuggestions
                    ) }
                }
            }.onFailure {
                val mockSuggestions = when (_state.value.targetSpecies) {
                    "翘嘴" -> listOf(
                        LureSuggestion("米诺", "10-15g 浮水米诺，主攻中上层", "minnow"),
                        LureSuggestion("亮片", "斜切亮片，远投搜索", "spoon")
                    )
                    "鳜鱼" -> listOf(
                        LureSuggestion("软虫", "铅头钩+卷尾蛆，跳底搜索", "worm"),
                        LureSuggestion("VIB", "沉水VIB，搜索深场结构", "vib")
                    )
                    else -> listOf(
                        LureSuggestion("波扒", "撞水波扒，清晨傍晚水面系", "pencil"),
                        LureSuggestion("铅笔", "浮水铅笔，逗弄掠食性鱼类", "pencil")
                    )
                }
                _state.update { it.copy(
                    fishingIndex = 82,
                    fishingDescription = "系统建议：今日水温适中，建议尝试中层搜索",
                    lureSuggestions = mockSuggestions
                ) }
            }
        }
    }
    
    private fun loadAstronomy(lat: Double?, lon: Double?) {
        if (lat == null || lon == null) return
        viewModelScope.launch {
            runCatching {
                val resp = apiService.getAstronomy(lat = lat, lon = lon, date = null)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.success == true) {
                        _state.update { it.copy(astronomy = body.data) }
                    }
                }
            }
        }
    }
    
    /**
     * 确保 hourly 数据可用于趋势图绘制：
     * 1. 不足 24 条时，全量生成模拟数据
     * 2. 有 24+ 条但气压/风速大部分为 null 时，填充缺失字段
     */
    private fun ensureUsableHourly(data: WeatherData): WeatherData {
        val hourly = data.hourly

        // 情况1：不足 6 条，全量生成
        if (hourly.size < 6) {
            return data.copy(hourly = generateSimulatedHourly(data))
        }

        // 情况2：检查气压和风速是否大部分为 null
        val pressureNullRatio = hourly.count { it.pressure == null }.toFloat() / hourly.size
        val windNullRatio = hourly.count { it.windSpeed == null }.toFloat() / hourly.size

        if (pressureNullRatio < 0.5f && windNullRatio < 0.5f) {
            // 数据质量足够，直接使用
            return data
        }

        // 数据质量不足，填充缺失字段
        val basePressure = data.current.pressure.takeIf { it > 0f } ?: 1013f
        val baseWind = data.current.windSpeed.takeIf { it > 0f } ?: 2f

        val enrichedHourly = hourly.mapIndexed { index, h ->
            val hour = try {
                h.time.substringAfter('T').take(2).toInt()
            } catch (_: Exception) { index % 24 }

            val phase = 2.0 * PI * (hour - 14.0) / 24.0
            val tempFraction = (cos(phase) + 1.0) / 2.0

            val pressure = h.pressure ?: run {
                val offset = (sin(phase * 1.5) * 2.0).toFloat()
                basePressure + offset
            }
            val windSpeed = h.windSpeed ?: run {
                val factor = (0.6 + 0.8 * tempFraction).toFloat()
                baseWind * factor
            }

            h.copy(pressure = pressure, windSpeed = windSpeed)
        }

        return data.copy(hourly = enrichedHourly)
    }

    /**
     * 当 API 只返回极少量逐小时数据时（如聚合天气只有1条），
     * 基于当前温度、每日最高/最低温生成模拟的24小时曲线。
     * 温度遵循自然日变化规律：清晨5~6点最低，午后14~15点最高。
     * 同时生成气压和风速的合理波动。
     */
    private fun generateSimulatedHourly(data: WeatherData): List<com.lurecalendar.app.domain.model.HourlyWeather> {
        val current = data.current
        val daily = data.daily.firstOrNull()
        val currentTemp = current.temperature
        val highTemp = daily?.tempMax ?: (currentTemp + 5f)
        val lowTemp = daily?.tempMin ?: (currentTemp - 5f)
        val basePressure = current.pressure.takeIf { it > 0f } ?: 1013f
        val baseWind = current.windSpeed.takeIf { it > 0f } ?: 2f
        val windDir = current.windDirection.ifBlank { "东风" }
        val weatherText = data.hourly.firstOrNull()?.weatherText ?: "晴"

        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm+08:00")

        return List(24) { i ->
            val hourTime = now.plusHours(i.toLong())
            val h = hourTime.hour

            // 使用正弦曲线模拟日温度变化, 14点最高, 5点最低
            // 将小时映射到 [-PI, PI]，14点对应峰值
            val phase = 2.0 * PI * (h - 14.0) / 24.0
            val tempFraction = (cos(phase) + 1.0) / 2.0  // 0~1, 14点=1, 2点=0
            val temp = lowTemp + (highTemp - lowTemp) * tempFraction.toFloat()

            // 气压：轻微随机波动 ±2 hPa
            val pressureOffset = (sin(phase * 1.5) * 2.0).toFloat()
            val pressure = basePressure + pressureOffset

            // 风速：午后略高, 夜间略低
            val windFactor = (0.6 + 0.8 * tempFraction).toFloat()
            val wind = baseWind * windFactor

            com.lurecalendar.app.domain.model.HourlyWeather(
                time = hourTime.format(formatter),
                temperature = temp,
                humidity = current.humidity,
                pressure = pressure,
                windSpeed = wind,
                windDirection = windDir,
                precipitationProbability = 0,
                precipitation = 0f,
                weatherText = weatherText,
                waterTemperature = com.lurecalendar.app.common.fishing.FishingIndexCalculator.estimateWaterTempC(
                    temp, _state.value.waterType
                ),
                fishingIndex = com.lurecalendar.app.common.fishing.FishingIndexCalculator.calculate(
                    pressureHpa = pressure,
                    windSpeedMs = wind,
                    airTempC = temp,
                    waterTempC = null,
                    precipitationMm = 0f,
                    precipitationProbability = 0
                ).score
            )
        }
    }

    /**
     * API 完全失败时生成回退 WeatherData，确保图表不为空
     */
    private fun generateFallbackWeatherData(): WeatherData {
        val fallbackCurrent = com.lurecalendar.app.domain.model.CurrentWeather(
            temperature = 22f,
            humidity = 65,
            pressure = 1013f,
            windSpeed = 2.5f,
            windDirection = "东风",
            precipitation = 0f,
            visibility = 10f
        )
        val fallbackData = WeatherData(
            current = fallbackCurrent,
            hourly = emptyList(),
            daily = emptyList()
        )
        return fallbackData.copy(hourly = generateSimulatedHourly(fallbackData))
    }

    private fun loadSmartLures(data: WeatherData) {
        val species = _state.value.targetSpecies
        val waterType = _state.value.waterType
        val waterTemp = data.hourly.firstOrNull()?.waterTemperature?.toDouble()
        val hour = java.util.Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        viewModelScope.launch {
            runCatching {
                val resp = apiService.matchLures(
                    species = species,
                    waterTemp = waterTemp,
                    waterType = waterType,
                    hour = hour
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val list = body?.recommendations.orEmpty().take(4)
                    if (list.isNotEmpty()) {
                        _state.update { it.copy(smartLures = list) }
                    }
                }
            }
        }
    }
}

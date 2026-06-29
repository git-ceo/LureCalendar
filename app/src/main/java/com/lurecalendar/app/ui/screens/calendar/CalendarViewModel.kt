package com.lurecalendar.app.ui.screens.calendar

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.common.fishing.FishingIndexCalculator
import com.lurecalendar.app.common.location.AmapLocationProvider
import com.lurecalendar.app.data.local.SpotPreferences
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.domain.model.FishingSpot
import com.lurecalendar.app.domain.model.WeatherData
import com.lurecalendar.app.domain.repository.FishingSpotRepository
import com.lurecalendar.app.domain.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val fishingSpotRepository: FishingSpotRepository,
    private val apiService: LureCalendarApiService,
    private val spotPreferences: SpotPreferences,
    private val locationProvider: AmapLocationProvider
) : ViewModel() {

    data class CalendarUiState(
        val currentSpotName: String = "未选择钓点",
        val cityName: String = "",
        val temperature: Float? = null,
        val weatherText: String = "",
        val lureActivityScore: Int = 0,
        val scoreLabel: String = "",
        val pressure: Float? = null,
        val pressureTrend: String = "稳定",
        val windSpeed: Float? = null,
        val windDirection: String = "",
        val moonPhase: String = "",
        val lunarDate: String = "",
        val sunrise: String = "",
        val sunset: String = "",
        val hourlyForecasts: List<HourlyForecast> = emptyList(),
        val dailyScores: List<DailyScore> = emptyList(),
        val reminders: List<String> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val spots: List<FishingSpot> = emptyList(),
        val errorMessage: String? = null,
        val favoriteSpotIds: Set<String> = emptySet(),
        val selectedTargetSpecies: String = "翘嘴",
        val availableSpecies: List<String> = listOf("翘嘴", "鲈鱼", "鳜鱼", "黑鱼", "马口", "鳡鱼", "军鱼")
    )

    data class HourlyForecast(
        val time: String,
        val temperature: Float,
        val weatherText: String,
        val fishingIndex: Int,
        val lureRecommend: String
    )

    data class DailyScore(
        val date: String,
        val dayOfWeek: String,
        val tempMax: Float,
        val tempMin: Float,
        val weatherText: String,
        val lureScore: Int,
        val bestWindow: String
    )

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state

    // --- 日历月/周视图新增状态 ---
    enum class CalendarViewMode { MONTH, WEEK }

    private val _selectedDay = MutableStateFlow<Int?>(null)
    val selectedDay: StateFlow<Int?> = _selectedDay.asStateFlow()

    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    private val _monthScores = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val monthScores: StateFlow<Map<Int, Int>> = _monthScores.asStateFlow()

    private val _weekDays = MutableStateFlow<List<DayInfo>>(emptyList())
    val weekDays: StateFlow<List<DayInfo>> = _weekDays.asStateFlow()

    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    // --- DayDetailSheet 数据 ---
    private val _dayDetailDate = MutableStateFlow("")
    val dayDetailDate: StateFlow<String> = _dayDetailDate.asStateFlow()

    private val _dayDetailScore = MutableStateFlow(0)
    val dayDetailScore: StateFlow<Int> = _dayDetailScore.asStateFlow()

    private val _dayDetailScoreLabel = MutableStateFlow("")
    val dayDetailScoreLabel: StateFlow<String> = _dayDetailScoreLabel.asStateFlow()

    private val _dayDetailPressureHistory = MutableStateFlow<List<Float>>(emptyList())
    val dayDetailPressureHistory: StateFlow<List<Float>> = _dayDetailPressureHistory.asStateFlow()

    private val _dayDetailPressureForecast = MutableStateFlow<List<Float>>(emptyList())
    val dayDetailPressureForecast: StateFlow<List<Float>> = _dayDetailPressureForecast.asStateFlow()

    private val _dayDetailBestWindows = MutableStateFlow<List<FishingWindow>>(emptyList())
    val dayDetailBestWindows: StateFlow<List<FishingWindow>> = _dayDetailBestWindows.asStateFlow()

    private val _dayDetailLureRecommends = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val dayDetailLureRecommends: StateFlow<Map<String, List<String>>> = _dayDetailLureRecommends.asStateFlow()

    private val _dayDetailTopSpecies = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val dayDetailTopSpecies: StateFlow<List<Pair<String, Int>>> = _dayDetailTopSpecies.asStateFlow()

    // 缓存最新天气数据，用于鱼种切换时重新计算
    private var cachedWeatherData: WeatherData? = null

    init {
        // 先从 DataStore 恢复用户选择的目标鱼种，再启动天气加载
        // 避免天气加载先于目标鱼种恢复，导致钓鱼指数按默认"翘嘴"计算
        viewModelScope.launch {
            val savedSpecies = spotPreferences.userTargetSpecies.first()
            if (_state.value.selectedTargetSpecies != savedSpecies) {
                _state.update { it.copy(selectedTargetSpecies = savedSpecies) }
            }
            loadSpots()
        }
        loadFavorites()
        loadMonthScores(_currentYear.value, _currentMonth.value)
        loadWeekDays()
        // 注意：不再持续监听 DataStore 的 userTargetSpecies。
        // saveSelectedSpot() 与 setUserTargetSpecies() 共用同一个 DataStore，
        // 任何写入都会触发 userTargetSpecies Flow 重新发射。
        // 如果异步写入未完成/被取消，observer 会读取到默认值"翘嘴"，
        // 从而把用户刚选的"鳜鱼"强制覆盖回"翘嘴"，导致指数回落。
    }

    /**
     * 用户切换目标鱼种。
     * 将状态更新、DataStore 持久化、指数重新计算放在同一协程中顺序执行，
     * 避免异步写入未完成时被 DataStore Flow 误触发覆盖。
     */
    fun selectTargetSpecies(species: String) {
        viewModelScope.launch {
            // 1. 先更新内存状态
            _state.update { it.copy(selectedTargetSpecies = species) }
            // 2. 同步持久化到 DataStore（确保写入完成）
            spotPreferences.setUserTargetSpecies(species)
            // 3. 写入完成后重新计算指数
            recalculateFishingIndex()
        }
    }

    private fun recalculateFishingIndex() {
        val data = cachedWeatherData ?: return
        processWeatherData(data)
        updateWeekDaysFromForecast(data)
        updateMonthScoresFromForecast(data)
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            fishingSpotRepository.getFavoriteSpotIds().collect { ids ->
                _state.update { it.copy(favoriteSpotIds = ids.toSet()) }
            }
        }
    }

    fun toggleFavorite(spotId: String) {
        viewModelScope.launch {
            val currentFavorites = _state.value.favoriteSpotIds
            if (currentFavorites.contains(spotId)) {
                fishingSpotRepository.removeFavorite(spotId)
            } else {
                fishingSpotRepository.addFavorite(spotId)
            }
        }
    }

    // 标记是否已经触发过初始钓点选择（避免每次 Flow 发射都重新选择）
    private var initialSpotSelected = false

    private fun loadSpots() {
        viewModelScope.launch {
            fishingSpotRepository.getAllSpots()
                .distinctUntilChanged()
                .collectLatest { spots ->
                    // 获取用户位置并按距离排序
                    val sortedSpots = try {
                        val deviceLocation = locationProvider.getOnceLocation()
                        spots.sortedBy { spot ->
                            calculateDistance(
                                deviceLocation.latitude, deviceLocation.longitude,
                                spot.latitude, spot.longitude
                            )
                        }
                    } catch (_: Exception) {
                        // 定位失败时按名称排序
                        spots.sortedBy { it.name }
                    }

                    _state.update { it.copy(spots = sortedSpots) }

                    // 仅在首次收到非空钓点列表时触发选择和天气加载
                    if (!initialSpotSelected && sortedSpots.isNotEmpty()) {
                        initialSpotSelected = true
                        val savedSpotId = spotPreferences.selectedSpotId.first()
                        val restoredSpot = if (savedSpotId != null) {
                            sortedSpots.find { it.id == savedSpotId }
                        } else null

                        selectSpot(restoredSpot ?: sortedSpots.first())
                    } else if (!initialSpotSelected && sortedSpots.isEmpty()) {
                        // 首次且列表为空，使用默认城市加载天气（后续 Flow 更新时会再处理）
                        loadWeatherForCity("绵阳")
                    }
                }
        }
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun selectSpot(spot: FishingSpot) {
        _state.update {
            it.copy(
                currentSpotName = spot.name,
                cityName = spot.city ?: ""
            )
        }
        // 持久化用户选择
        viewModelScope.launch {
            spotPreferences.saveSelectedSpot(spot.id, spot.name)
        }
        loadWeatherForSpot(spot)
    }

    fun refresh() {
        val spots = _state.value.spots
        val currentName = _state.value.currentSpotName
        val spot = spots.find { it.name == currentName }
        viewModelScope.launch {
            // 刷新前从 DataStore 重新确认目标鱼种，防止被意外覆盖
            val savedSpecies = spotPreferences.userTargetSpecies.first()
            if (_state.value.selectedTargetSpecies != savedSpecies) {
                _state.update { it.copy(selectedTargetSpecies = savedSpecies) }
            }
            _state.update { it.copy(isRefreshing = true) }
            if (spot != null) {
                loadWeatherForSpot(spot)
            } else {
                loadWeatherForCity("绵阳")
            }
        }
    }

    private fun loadWeatherForCity(city: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val resolved = weatherRepository.resolveLocation(city)
                .getOrElse { e ->
                    _state.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = e.message) }
                    loadFallbackData()
                    return@launch
                }
            _state.update { it.copy(cityName = resolved.name) }
            fetchWeather(resolved.id, resolved.lat, resolved.lon)
        }
    }

    private fun loadWeatherForSpot(spot: FishingSpot) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val locationId = spot.qWeatherLocationId
            if (!locationId.isNullOrBlank()) {
                fetchWeather(locationId, spot.latitude, spot.longitude)
            } else {
                val resolved = weatherRepository.resolveLocation(spot.latitude, spot.longitude)
                    .getOrElse { e ->
                        _state.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = e.message) }
                        loadFallbackData()
                        return@launch
                    }
                _state.update { it.copy(cityName = resolved.name) }
                fetchWeather(resolved.id, resolved.lat, resolved.lon)
            }
        }
    }

    private suspend fun fetchWeather(locationId: String, lat: Double?, lon: Double?) {
        val result = weatherRepository.getWeather(
            location = locationId,
            latitude = lat,
            longitude = lon,
            forceRefresh = true
        )
        result.fold(
            onSuccess = { data ->
                cachedWeatherData = data
                processWeatherData(data)
                updateWeekDaysFromForecast(data)
                updateMonthScoresFromForecast(data)
                loadAstronomy(lat, lon)
            },
            onFailure = { e ->
                _state.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = e.message) }
                loadFallbackData()
            }
        )
    }

    private fun processWeatherData(data: WeatherData) {
        val current = data.current
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val moonPhaseValue = _state.value.moonPhase.ifBlank { null }
        val pressureHistory = data.hourly.take(6).mapNotNull { it.pressure }

        // 计算当前钓鱼指数
        val species = _state.value.selectedTargetSpecies
        val indexResult = FishingIndexCalculator.calculate(
            pressureHpa = current.pressure,
            windSpeedMs = current.windSpeed,
            airTempC = current.temperature,
            precipitationMm = current.precipitation,
            moonPhase = moonPhaseValue,
            hour = hour,
            weatherText = data.hourly.firstOrNull()?.weatherText,
            pressureHistory = pressureHistory,
            targetSpecies = species
        )

        // 气压趋势判断
        val pressureTrend = if (pressureHistory.size >= 2) {
            val delta = pressureHistory.last() - pressureHistory.first()
            when {
                delta > 2f -> "上升"
                delta < -2f -> "下降"
                else -> "稳定"
            }
        } else "稳定"

        // 处理逐小时预测
        val hourlyForecasts = data.hourly.take(24).map { h ->
            val hIndex = FishingIndexCalculator.calculate(
                pressureHpa = h.pressure,
                windSpeedMs = h.windSpeed,
                airTempC = h.temperature,
                precipitationMm = h.precipitation,
                moonPhase = moonPhaseValue,
                hour = h.time.takeLast(5).take(2).toIntOrNull(),
                weatherText = h.weatherText,
                targetSpecies = species
            )
            val lureRec = when {
                hIndex.score >= 80 -> "水面系"
                hIndex.score >= 60 -> "米诺/亮片"
                hIndex.score >= 40 -> "软饵慢搜"
                else -> "暂不建议"
            }
            HourlyForecast(
                time = h.time.takeLast(5),
                temperature = h.temperature,
                weatherText = h.weatherText ?: "晴",
                fishingIndex = hIndex.score,
                lureRecommend = lureRec
            )
        }

        // 处理未来3天评分
        val dailyScores = data.daily.take(3).mapIndexed { index, d ->
            val dIndex = FishingIndexCalculator.calculate(
                pressureHpa = d.pressure,
                windSpeedMs = d.windSpeed,
                airTempC = (d.tempMax + d.tempMin) / 2f,
                precipitationMm = d.precipitation,
                moonPhase = moonPhaseValue,
                hour = 7,
                weatherText = d.weatherText,
                targetSpecies = species
            )
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, index) }
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.CHINA).format(cal.time)
            val dateStr = SimpleDateFormat("MM/dd", Locale.CHINA).format(cal.time)
            val bestWindow = when {
                dIndex.score >= 70 -> "清晨 05:00-07:00"
                dIndex.score >= 50 -> "傍晚 17:00-19:00"
                else -> "无明显窗口"
            }
            DailyScore(
                date = dateStr,
                dayOfWeek = dayOfWeek,
                tempMax = d.tempMax,
                tempMin = d.tempMin,
                weatherText = d.weatherText.ifBlank { "晴" },
                lureScore = dIndex.score,
                bestWindow = bestWindow
            )
        }

        // 生成提醒
        val reminders = mutableListOf<String>()
        if (hourlyForecasts.isNotEmpty()) {
            val bestHour = hourlyForecasts.maxByOrNull { it.fishingIndex }
            if (bestHour != null && bestHour.fishingIndex >= 70) {
                reminders.add("今日 ${bestHour.time} 为最佳窗口期，建议${bestHour.lureRecommend}")
            }
        }
        if (pressureTrend == "下降") {
            reminders.add("气压呈下降趋势，鱼口可能减弱")
        }
        if (dailyScores.size >= 2 && dailyScores[1].lureScore >= 75) {
            reminders.add("明天评分较高(${dailyScores[1].lureScore}分)，建议安排出钓")
        }
        if (reminders.isEmpty()) {
            reminders.add("今日路亚活跃度${indexResult.score}分，${getScoreLabel(indexResult.score)}")
        }

        _state.update {
            it.copy(
                temperature = current.temperature,
                weatherText = data.hourly.firstOrNull()?.weatherText
                    ?: data.daily.firstOrNull()?.weatherText?.ifBlank { null }
                    ?: "晴",
                lureActivityScore = indexResult.score,
                scoreLabel = getScoreLabel(indexResult.score),
                pressure = current.pressure,
                pressureTrend = pressureTrend,
                windSpeed = current.windSpeed,
                windDirection = current.windDirection,
                hourlyForecasts = hourlyForecasts,
                dailyScores = dailyScores,
                reminders = reminders.take(3),
                isLoading = false,
                isRefreshing = false,
                errorMessage = null
            )
        }
    }

    private fun loadAstronomy(lat: Double?, lon: Double?) {
        if (lat == null || lon == null) return
        viewModelScope.launch {
            runCatching {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
                    .format(Calendar.getInstance().time)
                val resp = apiService.getAstronomy(lat, lon, dateStr)
                if (resp.isSuccessful) {
                    val astro = resp.body()?.data
                    if (astro != null) {
                        _state.update {
                            it.copy(
                                moonPhase = astro.moonPhase ?: "未知",
                                lunarDate = astro.lunarDate ?: "",
                                sunrise = astro.sunrise ?: "06:00",
                                sunset = astro.sunset ?: "19:30"
                            )
                        }
                        // 月相更新后用最新数据重新计算钓鱼指数（月相影响评分）
                        recalculateFishingIndex()
                    }
                }
            }
        }
    }

    private fun loadFallbackData() {
        val fallbackData = generateFallbackWeatherData()
        cachedWeatherData = fallbackData
        // 用实际计算器计算，而非硬编码固定分值，确保与 recalculateFishingIndex() 结果一致
        val species = _state.value.selectedTargetSpecies
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val indexResult = FishingIndexCalculator.calculate(
            pressureHpa = fallbackData.current.pressure,
            windSpeedMs = fallbackData.current.windSpeed,
            airTempC = fallbackData.current.temperature,
            precipitationMm = fallbackData.current.precipitation,
            moonPhase = _state.value.moonPhase.ifBlank { null },
            hour = hour,
            weatherText = null,
            targetSpecies = species
        )
        _state.update {
            it.copy(
                temperature = 25f,
                weatherText = "晴",
                lureActivityScore = indexResult.score,
                scoreLabel = getScoreLabel(indexResult.score),
                pressure = 1013f,
                pressureTrend = "稳定",
                windSpeed = 2.5f,
                windDirection = "东南风",
                moonPhase = "上弦月",
                lunarDate = "四月十八",
                sunrise = "06:05",
                sunset = "19:45",
                hourlyForecasts = generateFallbackHourly(),
                dailyScores = generateFallbackDaily(),
                reminders = listOf(
                    "清晨为最佳窗口期，建议水面系",
                    "今日气压稳定，适宜路亚"
                ),
                isLoading = false,
                isRefreshing = false
            )
        }
        updateWeekDaysFromForecast(fallbackData)
        updateMonthScoresFromForecast(fallbackData)
    }

    private fun generateFallbackWeatherData(): WeatherData {
        val current = com.lurecalendar.app.domain.model.CurrentWeather(
            temperature = 25f,
            humidity = 60,
            pressure = 1013f,
            windSpeed = 2.5f,
            windDirection = "东南风",
            precipitation = 0f,
            visibility = 10f
        )
        // 简单生成3天预报
        val daily = (0..2).map { i ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, i) }
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(cal.time)
            com.lurecalendar.app.domain.model.DailyWeather(
                date = dateStr,
                tempMax = 28f + i,
                tempMin = 18f + i,
                humidity = 60,
                pressure = 1013f,
                windSpeed = 2.5f,
                precipitation = 0f,
                uvIndex = 5,
                weatherText = "晴"
            )
        }
        return WeatherData(current = current, hourly = emptyList(), daily = daily)
    }

    private fun generateFallbackHourly(): List<HourlyForecast> {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        return (0 until 24).map { offset ->
            val h = (currentHour + offset) % 24
            val temp = 20f + (if (h in 10..16) 8f else if (h in 6..9 || h in 17..20) 4f else 0f)
            val score = when (h) {
                in 5..7 -> 85
                in 17..19 -> 80
                in 8..9, in 15..16 -> 65
                in 10..14 -> 45
                else -> 35
            }
            HourlyForecast(
                time = String.format("%02d:00", h),
                temperature = temp,
                weatherText = "晴",
                fishingIndex = score,
                lureRecommend = when {
                    score >= 80 -> "水面系"
                    score >= 60 -> "米诺/亮片"
                    score >= 40 -> "软饵慢搜"
                    else -> "暂不建议"
                }
            )
        }
    }

    private fun generateFallbackDaily(): List<DailyScore> {
        val cal = Calendar.getInstance()
        return (0 until 3).map { i ->
            val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, i) }
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.CHINA).format(day.time)
            val dateStr = SimpleDateFormat("MM/dd", Locale.CHINA).format(day.time)
            DailyScore(
                date = dateStr,
                dayOfWeek = dayOfWeek,
                tempMax = 28f + i,
                tempMin = 18f + i,
                weatherText = if (i == 0) "晴" else if (i == 1) "多云" else "阴",
                lureScore = 72 - i * 8,
                bestWindow = if (i == 0) "清晨 05:00-07:00" else "傍晚 17:00-19:00"
            )
        }
    }

    private fun getScoreLabel(score: Int): String = when {
        score >= 85 -> "爆护期"
        score >= 70 -> "活跃"
        score >= 55 -> "一般"
        score >= 40 -> "低迷"
        else -> "不建议出钓"
    }

    // --- 日历月/周视图新增方法 ---

    fun selectDay(day: Int) {
        _selectedDay.value = day
        loadDayDetail(day)
    }

    fun dismissDayDetail() {
        _selectedDay.value = null
    }

    fun switchViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }

    fun loadMonthScores(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
        viewModelScope.launch {
            // 生成当月每天的模拟评分（基于已有 FishingIndexCalculator）
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val scores = mutableMapOf<Int, Int>()
            for (d in 1..daysInMonth) {
                // 根据日期生成一个模拟评分，后续可替换为真实 API 数据
                val dayScore = generateDailyScore(year, month, d)
                scores[d] = dayScore
            }
            _monthScores.value = scores
        }
    }

    /**
     * 用真实天气预报数据覆盖月视图中今天及未来日期的分数，
     * 确保月视图分数与顶部实时分数一致
     */
    private fun updateMonthScoresFromForecast(data: WeatherData) {
        if (data.daily.isEmpty()) return

        val currentYear = _currentYear.value
        val currentMonth = _currentMonth.value

        // 构建日期 -> 真实分数映射
        val realScores = mutableMapOf<Int, Int>()

        // 今天的分数使用与顶部相同的算法（基于当前实时天气）
        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH) + 1
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

        val species = _state.value.selectedTargetSpecies

        if (todayYear == currentYear && todayMonth == currentMonth) {
            val hour = todayCal.get(Calendar.HOUR_OF_DAY)
            val todayScore = FishingIndexCalculator.calculate(
                pressureHpa = data.current.pressure,
                windSpeedMs = data.current.windSpeed,
                airTempC = data.current.temperature,
                precipitationMm = data.current.precipitation,
                moonPhase = _state.value.moonPhase.ifBlank { null },
                hour = hour,
                weatherText = data.hourly.firstOrNull()?.weatherText,
                targetSpecies = species
            ).score
            realScores[todayDay] = todayScore
        }

        // 未来日期使用 daily 预报数据
        for (d in data.daily) {
            val parts = d.date.split("-")
            if (parts.size == 3) {
                val dYear = parts[0].toIntOrNull() ?: continue
                val dMonth = parts[1].toIntOrNull() ?: continue
                val dDay = parts[2].toIntOrNull() ?: continue
                if (dYear == currentYear && dMonth == currentMonth) {
                    val dScore = FishingIndexCalculator.calculate(
                        pressureHpa = d.pressure,
                        windSpeedMs = d.windSpeed,
                        airTempC = (d.tempMax + d.tempMin) / 2f,
                        precipitationMm = d.precipitation,
                        hour = 7,
                        weatherText = d.weatherText,
                        targetSpecies = species
                    ).score
                    realScores[dDay] = dScore
                }
            }
        }

        if (realScores.isNotEmpty()) {
            val updated = _monthScores.value.toMutableMap()
            updated.putAll(realScores)
            _monthScores.value = updated
        }
    }

    private fun loadWeekDays() {
        // 初始加载：使用占位数据，等天气 API 返回后会被 updateWeekDaysFromForecast 覆盖
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

            val days = mutableListOf<DayInfo>()
            val weekDayNames = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
            for (i in 0 until 7) {
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                val d = cal.get(Calendar.DAY_OF_MONTH)
                days.add(
                    DayInfo(
                        dayOfMonth = d,
                        date = "${m}月${d}日",
                        dayOfWeek = weekDayNames[i],
                        weatherText = "...",
                        tempMax = 0f,
                        tempMin = 0f,
                        lureScore = 0,
                        bestWindow = "加载中"
                    )
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            _weekDays.value = days
        }
    }

    /**
     * 当真实天气数据加载完成后，用 API 返回的 daily 预报更新周视图
     */
    private fun updateWeekDaysFromForecast(data: WeatherData) {
        if (data.daily.isEmpty()) return

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        val today = Calendar.getInstance()
        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(today.time)

        // 构建日期 -> DailyWeather 的映射
        val dailyMap = data.daily.associateBy { it.date }

        val days = mutableListOf<DayInfo>()
        val weekDayNames = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        for (i in 0 until 7) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(cal.time)

            val forecast = dailyMap[dateStr]
            val score: Int
            val weatherText: String
            val tempMax: Float
            val tempMin: Float

            if (forecast != null) {
                // 使用真实 API 数据
                val dIndex = FishingIndexCalculator.calculate(
                    pressureHpa = forecast.pressure,
                    windSpeedMs = forecast.windSpeed,
                    airTempC = (forecast.tempMax + forecast.tempMin) / 2f,
                    precipitationMm = forecast.precipitation,
                    hour = 7,
                    weatherText = forecast.weatherText,
                    targetSpecies = _state.value.selectedTargetSpecies
                )
                score = dIndex.score
                weatherText = forecast.weatherText.ifBlank { "晴" }
                tempMax = forecast.tempMax
                tempMin = forecast.tempMin
            } else {
                // 无预报数据（过去的日期）：使用算法生成
                score = generateDailyScore(y, m, d)
                weatherText = "—"
                tempMax = 0f
                tempMin = 0f
            }

            val bestWindow = when {
                score >= 70 -> "05:30-07:30"
                score >= 50 -> "17:00-19:00"
                else -> "无窗口"
            }
            days.add(
                DayInfo(
                    dayOfMonth = d,
                    date = "${m}月${d}日",
                    dayOfWeek = weekDayNames[i],
                    weatherText = weatherText,
                    tempMax = tempMax,
                    tempMin = tempMin,
                    lureScore = score,
                    bestWindow = bestWindow
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        _weekDays.value = days
    }

    private fun loadDayDetail(day: Int) {
        viewModelScope.launch {
            val year = _currentYear.value
            val month = _currentMonth.value
            val data = cachedWeatherData
            val moonPhaseValue = _state.value.moonPhase.ifBlank { null }
            val species = _state.value.selectedTargetSpecies

            // 查找选中日期的天气数据（如果是今天或未来）
            val today = Calendar.getInstance()
            val isToday = today.get(Calendar.YEAR) == year && 
                          today.get(Calendar.MONTH) + 1 == month && 
                          today.get(Calendar.DAY_OF_MONTH) == day
            
            val score: Int
            if (isToday) {
                score = _state.value.lureActivityScore
            } else {
                val dateStr = String.format("%04d-%02d-%02d", year, month, day)
                val dailyForecast = data?.daily?.find { it.date == dateStr }
                score = if (dailyForecast != null) {
                    FishingIndexCalculator.calculate(
                        pressureHpa = dailyForecast.pressure,
                        windSpeedMs = dailyForecast.windSpeed,
                        airTempC = (dailyForecast.tempMax + dailyForecast.tempMin) / 2f,
                        precipitationMm = dailyForecast.precipitation,
                        moonPhase = moonPhaseValue,
                        hour = 7,
                        weatherText = dailyForecast.weatherText,
                        targetSpecies = species
                    ).score
                } else {
                    _monthScores.value[day] ?: generateDailyScore(year, month, day)
                }
            }

            _dayDetailDate.value = "${year}年${month}月${day}日"
            _dayDetailScore.value = score
            _dayDetailScoreLabel.value = getScoreLabel(score)

            // 模拟气压数据
            _dayDetailPressureHistory.value = listOf(1012f, 1013f, 1014f, 1013f, 1012f, 1011f)
            _dayDetailPressureForecast.value = listOf(1011f, 1010f, 1011f, 1012f)

            // 模拟最佳窗口
            _dayDetailBestWindows.value = when {
                score >= 70 -> listOf(
                    FishingWindow("05:30-07:30", "清晨微风低光，适合水面系", 85),
                    FishingWindow("17:00-19:00", "黄昏气温回落，鱼类活跃", 78),
                    FishingWindow("20:00-21:30", "夜间安静，适合慢搜", 65)
                )
                score >= 50 -> listOf(
                    FishingWindow("17:00-19:00", "傍晚温差变化，鱼口尚可", 62),
                    FishingWindow("06:00-07:30", "清晨气压稳定", 58)
                )
                else -> listOf(
                    FishingWindow("17:30-18:30", "短暂窗口，建议快攻", 45)
                )
            }

            // 模拟饵料推荐
            _dayDetailLureRecommends.value = when {
                score >= 70 -> mapOf(
                    "清晨" to listOf("水面系", "波爬"),
                    "上午" to listOf("米诺", "亮片"),
                    "黄昏" to listOf("软虫", "铅头钩")
                )
                score >= 50 -> mapOf(
                    "清晨" to listOf("米诺", "摇滚"),
                    "黄昏" to listOf("软饵", "德州钓组")
                )
                else -> mapOf(
                    "黄昏" to listOf("软饵慢搜", "倒钓")
                )
            }

            // 计算该日期下不同鱼种的评分排行
            val availableSpecies = _state.value.availableSpecies
            val speciesRank = availableSpecies.map { spName ->
                val spScore = if (isToday) {
                    FishingIndexCalculator.calculate(
                        pressureHpa = data?.current?.pressure ?: 1013f,
                        windSpeedMs = data?.current?.windSpeed ?: 2.5f,
                        airTempC = data?.current?.temperature ?: 25f,
                        precipitationMm = data?.current?.precipitation ?: 0f,
                        moonPhase = moonPhaseValue,
                        hour = today.get(Calendar.HOUR_OF_DAY),
                        weatherText = data?.hourly?.firstOrNull()?.weatherText,
                        targetSpecies = spName
                    ).score
                } else {
                    val dateStr = String.format("%04d-%02d-%02d", year, month, day)
                    val dailyForecast = data?.daily?.find { it.date == dateStr }
                    if (dailyForecast != null) {
                        FishingIndexCalculator.calculate(
                            pressureHpa = dailyForecast.pressure,
                            windSpeedMs = dailyForecast.windSpeed,
                            airTempC = (dailyForecast.tempMax + dailyForecast.tempMin) / 2f,
                            precipitationMm = dailyForecast.precipitation,
                            moonPhase = moonPhaseValue,
                            hour = 7,
                            weatherText = dailyForecast.weatherText,
                            targetSpecies = spName
                        ).score
                    } else {
                        (score + (spName.hashCode() % 10 - 5)).coerceIn(20, 95)
                    }
                }
                spName to spScore
            }.sortedByDescending { it.second }

            _dayDetailTopSpecies.value = speciesRank
        }
    }

    /**
     * 根据日期生成模拟评分，后续替换为真实数据
     */
    private fun generateDailyScore(year: Int, month: Int, day: Int): Int {
        // 使用日期作为种子生成伪随机但确定性的评分
        val seed = year * 10000 + month * 100 + day
        val base = ((seed * 31 + 17) % 60) + 25 // 25-84
        // 清晨/黄昏加权
        val hourBonus = if (day % 3 == 0) 10 else 0
        return (base + hourBonus).coerceIn(20, 95)
    }
}
        val base = ((seed * 31 + 17) % 60) + 25 // 25-84
        // 清晨/黄昏加权
        val hourBonus = if (day % 3 == 0) 10 else 0
        return (base + hourBonus).coerceIn(20, 95)
    }
}

package com.lurecalendar.app.ui.screens.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurecalendar.app.ui.screens.calendar.components.DailyScoreCardRow
import com.lurecalendar.app.ui.screens.calendar.components.HourlyForecastRow
import com.lurecalendar.app.ui.screens.calendar.components.IndicatorDashboard
import com.lurecalendar.app.ui.screens.calendar.components.ScoreRingWidget
import com.lurecalendar.app.ui.screens.calendar.components.SpotSelectorSheet
import com.lurecalendar.app.ui.screens.calendar.components.getWeatherEmoji
import com.lurecalendar.app.ui.theme.WaterCyan

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToIndexExplanation: () -> Unit = {},
    onNavigateToWeatherDetail: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val monthScores by viewModel.monthScores.collectAsState()
    val weekDays by viewModel.weekDays.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    // DayDetailSheet 数据
    val dayDetailDate by viewModel.dayDetailDate.collectAsState()
    val dayDetailScore by viewModel.dayDetailScore.collectAsState()
    val dayDetailScoreLabel by viewModel.dayDetailScoreLabel.collectAsState()
    val dayDetailPressureHistory by viewModel.dayDetailPressureHistory.collectAsState()
    val dayDetailPressureForecast by viewModel.dayDetailPressureForecast.collectAsState()
    val dayDetailBestWindows by viewModel.dayDetailBestWindows.collectAsState()
    val dayDetailLureRecommends by viewModel.dayDetailLureRecommends.collectAsState()
    val dayDetailTopSpecies by viewModel.dayDetailTopSpecies.collectAsState()

    // 钓点选择弹窗状态
    var showSpotSelector by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (state.isLoading && !state.isRefreshing) {
            // 首次加载
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = WaterCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("加载中...", color = Color.White.copy(alpha = 0.7f))
                }
            }
        } else {
            AnimatedVisibility(visible = true, enter = fadeIn()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 模1: 当前钓点 + 实时天气摘要
                    item {
                        WeatherHeaderSection(
                            spotName = state.currentSpotName,
                            cityName = state.cityName,
                            temperature = state.temperature,
                            weatherText = state.weatherText,
                            lureActivityScore = state.lureActivityScore,
                            scoreLabel = state.scoreLabel,
                            selectedTargetSpecies = state.selectedTargetSpecies,
                            availableSpecies = state.availableSpecies,
                            onSpotSelectorClick = { showSpotSelector = true },
                            onScoreClick = onNavigateToIndexExplanation,
                            onWeatherClick = onNavigateToWeatherDetail,
                            onSpeciesSelected = { viewModel.selectTargetSpecies(it) }
                        )
                    }

                    // 模块2: 未来3天路亚评分卡片
                    item {
                        DailyScoreCardRow(dailyScores = state.dailyScores)
                    }

                    // 模块2.5: 月视图/周视图切换 + 日历视图
                    item {
                        CalendarViewSection(
                            viewMode = viewMode,
                            onViewModeChange = { viewModel.switchViewMode(it) },
                            year = currentYear,
                            month = currentMonth,
                            monthScores = monthScores,
                            weekDays = weekDays,
                            selectedDay = selectedDay,
                            onDayClick = { viewModel.selectDay(it) },
                            onMonthChange = { y, m -> viewModel.loadMonthScores(y, m) }
                        )
                    }

                    // 模块3: 关键指标仪表盘
                    item {
                        IndicatorDashboard(
                            pressure = state.pressure,
                            pressureTrend = state.pressureTrend,
                            windSpeed = state.windSpeed,
                            windDirection = state.windDirection,
                            moonPhase = state.moonPhase,
                            lunarDate = state.lunarDate,
                            sunrise = state.sunrise,
                            sunset = state.sunset
                        )
                    }

                    // 模块4: 逐小时预测
                    item {
                        HourlyForecastRow(forecasts = state.hourlyForecasts)
                    }

                    // 模块5: 近期待办/提醒
                    item {
                        RemindersSection(reminders = state.reminders)
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = WaterCyan
        )
    }

    // DayDetailSheet
    if (selectedDay != null) {
        DayDetailSheet(
            date = dayDetailDate,
            score = dayDetailScore,
            scoreLabel = dayDetailScoreLabel,
            pressureHistory = dayDetailPressureHistory,
            pressureForecast = dayDetailPressureForecast,
            bestWindows = dayDetailBestWindows,
            hourlyLureRecommends = dayDetailLureRecommends,
            topSpecies = dayDetailTopSpecies,
            onDismiss = { viewModel.dismissDayDetail() }
        )
    }

    // 钓点选择弹窗
    if (showSpotSelector) {
        SpotSelectorSheet(
            spots = state.spots,
            favoriteSpotIds = state.favoriteSpotIds,
            onSpotSelected = { spot ->
                viewModel.selectSpot(spot)
                showSpotSelector = false
            },
            onToggleFavorite = { spotId -> viewModel.toggleFavorite(spotId) },
            onDismiss = { showSpotSelector = false }
        )
    }
}

@Composable
private fun CalendarViewSection(
    viewMode: CalendarViewModel.CalendarViewMode,
    onViewModeChange: (CalendarViewModel.CalendarViewMode) -> Unit,
    year: Int,
    month: Int,
    monthScores: Map<Int, Int>,
    weekDays: List<DayInfo>,
    selectedDay: Int?,
    onDayClick: (Int) -> Unit,
    onMonthChange: (Int, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 视图切换 TabRow
        val tabs = listOf("月视图", "周视图")
        val selectedTabIndex = if (viewMode == CalendarViewModel.CalendarViewMode.MONTH) 0 else 1

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = WaterCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = WaterCyan
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        onViewModeChange(
                            if (index == 0) CalendarViewModel.CalendarViewMode.MONTH
                            else CalendarViewModel.CalendarViewMode.WEEK
                        )
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) WaterCyan
                                else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 视图内容
        when (viewMode) {
            CalendarViewModel.CalendarViewMode.MONTH -> {
                CalendarMonthView(
                    year = year,
                    month = month,
                    dailyScores = monthScores,
                    selectedDay = selectedDay,
                    onDayClick = onDayClick,
                    onMonthChange = onMonthChange
                )
            }
            CalendarViewModel.CalendarViewMode.WEEK -> {
                CalendarWeekView(
                    weekDays = weekDays,
                    selectedDay = selectedDay,
                    onDayClick = onDayClick
                )
            }
        }
    }
}

@Composable
private fun WeatherHeaderSection(
    spotName: String,
    cityName: String,
    temperature: Float?,
    weatherText: String,
    lureActivityScore: Int,
    scoreLabel: String,
    selectedTargetSpecies: String,
    availableSpecies: List<String>,
    onSpotSelectorClick: () -> Unit,
    onScoreClick: () -> Unit = {},
    onWeatherClick: () -> Unit = {},
    onSpeciesSelected: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // 钓点名称 + 城市 + 对象鱼选择器
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "位置",
                tint = WaterCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onSpotSelectorClick() }
            ) {
                Text(
                    text = spotName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "切换钓点",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (cityName.isNotBlank()) {
                Text(
                    text = cityName,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // 对象鱼选择器
        Spacer(modifier = Modifier.height(8.dp))
        TargetSpeciesSelector(
            selectedSpecies = selectedTargetSpecies,
            availableSpecies = availableSpecies,
            onSpeciesSelected = onSpeciesSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 温度 + 天气 + 评分圆环
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧: 温度 + 天气（可点击跳转天气详情）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onWeatherClick() }
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (temperature != null) "${temperature.toInt()}" else "--",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "°C",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getWeatherEmoji(weatherText),
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = weatherText.ifBlank { "获取中..." },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }

            // 右侧: 评分圆环
            ScoreRingWidget(
                score = lureActivityScore,
                label = scoreLabel,
                size = 110.dp,
                strokeWidth = 9.dp,
                modifier = Modifier.clickable { onScoreClick() }
            )
        }
    }
}

@Composable
private fun TargetSpeciesSelector(
    selectedSpecies: String,
    availableSpecies: List<String>,
    onSpeciesSelected: (String) -> Unit
) {
    var showSpeciesMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "目标鱼:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box {
            FilterChip(
                selected = true,
                onClick = { showSpeciesMenu = true },
                label = {
                    Text(
                        selectedSpecies,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                leadingIcon = {
                    Text("🐟", fontSize = 14.sp)
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WaterCyan.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )
            DropdownMenu(
                expanded = showSpeciesMenu,
                onDismissRequest = { showSpeciesMenu = false }
            ) {
                availableSpecies.forEach { species ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                species,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSpeciesSelected(species)
                            showSpeciesMenu = false
                        },
                        leadingIcon = {
                            if (species == selectedSpecies) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = WaterCyan
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RemindersSection(
    reminders: List<String>,
    modifier: Modifier = Modifier
) {
    if (reminders.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "提醒",
                tint = WaterCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "智能提醒",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        reminders.forEach { reminder ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = reminder,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

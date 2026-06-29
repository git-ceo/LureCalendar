package com.lurecalendar.app.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import com.lurecalendar.app.ui.screens.calendar.CalendarScreen
import com.lurecalendar.app.ui.screens.journal.JournalScreen
import com.lurecalendar.app.ui.screens.profile.ProfileScreen
import com.lurecalendar.app.ui.screens.social.SocialFeedScreen
import com.lurecalendar.app.ui.screens.spots.SpotManagerScreen
import com.lurecalendar.app.ui.screens.video.TechniqueVideoScreen
import com.lurecalendar.app.ui.screens.weather.WeatherViewModel
import com.lurecalendar.app.ui.theme.*
import com.lurecalendar.app.domain.model.FishingSpot
import com.airbnb.lottie.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToWeather: () -> Unit,
    onNavigateToWaterLevel: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToCatchRecord: () -> Unit,
    onNavigateToCatchForm: (String) -> Unit = {},
    onNavigateToReminderSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToGearStats: (String) -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToEncyclopedia: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToIndexExplanation: () -> Unit = {},
    onNavigateToWeatherDetail: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    weatherViewModel: WeatherViewModel = hiltViewModel()
) {
    val syncState by viewModel.syncState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    var navigateJob by remember { mutableStateOf<Job?>(null) }
    val navigateToPage: (Int) -> Unit = { targetPage ->
        if (pagerState.currentPage != targetPage) {
            navigateJob?.cancel()
            navigateJob = coroutineScope.launch {
                try {
                    pagerState.scrollToPage(targetPage)
                } catch (_: CancellationException) {
                    // 被新点击取消，正常忽略
                } catch (_: Exception) {
                    // 防止任何异常导致闪退
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = DarkSurface, contentColor = WaterCyan) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { navigateToPage(0) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "日历") },
                    label = { Text("日历") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WaterCyan,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = WaterCyan,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = WaterCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { navigateToPage(1) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "日志") },
                    label = { Text("日志") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WaterCyan,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = WaterCyan,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = WaterCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { navigateToPage(2) },
                    icon = { Icon(Icons.Default.Forum, contentDescription = "钓友圈") },
                    label = { Text("钓友圈") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WaterCyan,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = WaterCyan,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = WaterCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 3,
                    onClick = { navigateToPage(3) },
                    icon = { Icon(Icons.Default.Place, contentDescription = "钓点") },
                    label = { Text("钓点") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WaterCyan,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = WaterCyan,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = WaterCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 4,
                    onClick = { navigateToPage(4) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                    label = { Text("我的") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WaterCyan,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = WaterCyan,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = WaterCyan.copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(SurfaceDark)) {
            if (pagerState.currentPage == 0) {
                WaveBackground(progress = waveProgress, color = WaterCyan)
            }
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0,
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> CalendarScreen(
                        onNavigateToIndexExplanation = onNavigateToIndexExplanation,
                        onNavigateToWeatherDetail = onNavigateToWeatherDetail
                    )
                    1 -> JournalScreen(
                        onNavigateToCatchForm = { onNavigateToCatchForm("") }
                    )
                    2 -> SocialFeedScreen(
                        onNavigateToLeaderboard = onNavigateToLeaderboard
                    )
                    3 -> SpotManagerScreen(
                        onAddCatch = onNavigateToCatchForm
                    )
                    4 -> ProfileScreen(
                        onLogout = onLogout,
                        onNavigateToReminderSettings = onNavigateToReminderSettings,
                        onNavigateToAchievements = onNavigateToAchievements,
                        onNavigateToGearStats = onNavigateToGearStats,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    onNavigateToWeather: () -> Unit,
    onNavigateToWaterLevel: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToCatchRecord: () -> Unit,
    onNavigateToEncyclopedia: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel,
    weatherViewModel: WeatherViewModel,
    syncState: SyncUiState
) {
    val weatherState by weatherViewModel.state.collectAsState()
    val hotSpots by viewModel.hotSpots.collectAsState()

    Column {
        TopAppBar(
            title = { Text("Lure Calendar", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) },
            actions = {
                IconButton(onClick = { viewModel.performSync() }) {
                    Icon(
                        imageVector = if (syncState is SyncUiState.Loading) Icons.Default.Sync else Icons.Default.CloudSync,
                        contentDescription = "同步",
                        tint = if (syncState is SyncUiState.Loading) WaterCyan else Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White
            )
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FishingIndexCard(
                    score = weatherState.fishingIndex ?: 0,
                    description = weatherState.fishingDescription ?: "计算中...",
                    onClick = onNavigateToWeather
                )
            }
            
            item {
                QuickActions(
                    onNavigateToMap = onNavigateToMap,
                    onNavigateToCatchRecord = onNavigateToCatchRecord
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ActionButton(
                        label = "路亚学院",
                        icon = Icons.Default.MenuBook,
                        color = WaterCyan,
                        onClick = onNavigateToEncyclopedia,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        label = "天气钓况",
                        icon = Icons.Default.WbSunny,
                        color = DeepGreen,
                        onClick = onNavigateToWeather,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("核心数据", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("更多", color = WaterCyan, fontSize = 12.sp, modifier = Modifier.clickable { onNavigateToWaterLevel() })
                }
            }
            
            item {
                WaterLevelMiniCard(onNavigateToWaterLevel)
            }

            // 新增：路亚饵建议区域
            if (weatherState.lureSuggestions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("路亚饵建议", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        weatherState.lureSuggestions.forEach { lure ->
                            LureSuggestionItem(lure, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("热门钓场", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            
            if (hotSpots.isEmpty()) {
                items(3) {
                    SpotItem(name = "加载中...", subtitle = "正在获取附近钓场", onClick = onNavigateToMap)
                }
            } else {
                items(hotSpots.size) { index ->
                    val spot = hotSpots[index]
                    SpotItem(
                        name = spot.name,
                        subtitle = "${spot.city ?: ""} | ${spot.targetSpecies ?: "多鱼种"}",
                        onClick = onNavigateToMap
                    )
                }
            }
        }
    }
}

@Composable
fun FishingIndexCard(score: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .glassBackground(24)
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("今日钓鱼指数", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedContent(
                    targetState = score,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                    }, label = "score"
                ) { target ->
                    Text(
                        text = "$target",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = WaterCyan
                    )
                }
                Text(
                    text = description, 
                    color = WaterCyan, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            // Lottie 或图标占位
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Waves, 
                    contentDescription = null, 
                    modifier = Modifier.size(60.dp), 
                    tint = WaterCyan.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun QuickActions(onNavigateToMap: () -> Unit, onNavigateToCatchRecord: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(
            label = "钓点地图",
            icon = Icons.Default.Map,
            color = DeepGreen,
            onClick = onNavigateToMap,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            label = "新增鱼获",
            icon = Icons.Default.AddCircle,
            color = WaterCyan,
            onClick = onNavigateToCatchRecord,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WaterLevelMiniCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().glassBackground(20).clickable { onClick() }.padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(WaterCyan.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Waves, contentDescription = null, tint = WaterCyan, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("梓江(梓潼流域)", color = Color.White, fontWeight = FontWeight.Bold)
                Text("当前水位: 105.2m (平稳)", color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun SpotItem(name: String, subtitle: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().glassBackground(16).clickable { onClick() }.padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun LureSuggestionItem(lure: com.lurecalendar.app.ui.screens.weather.LureSuggestion, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .glassBackground(16)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when(lure.icon) {
                    "pencil" -> Icons.Default.Create        // 铅笔饵
                    "minnow" -> Icons.Default.Phishing      // 米诺-鱼钩
                    "vib" -> Icons.Default.Vibration        // VIB振动饵
                    "spoon" -> Icons.Default.AutoAwesome    // 亮片-闪光
                    "worm" -> Icons.Default.Water           // 软虫
                    else -> Icons.Default.Water
                },
                contentDescription = null,
                tint = WaterCyan,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = lure.name, 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = lure.desc, 
                color = Color.Gray, 
                fontSize = 10.sp, 
                lineHeight = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

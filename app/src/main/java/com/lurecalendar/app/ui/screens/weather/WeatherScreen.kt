package com.lurecalendar.app.ui.screens.weather

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurecalendar.app.common.fishing.FishInsightGenerator
import com.lurecalendar.app.domain.repository.WeatherProvider
import com.lurecalendar.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    onNavigateBack: () -> Unit,
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.state.collectAsState()

    var showRationale by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            showRationale = false
            if (granted) viewModel.useCurrentLocation()
        }
    )
    val hasLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

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
        topBar = {
            TopAppBar(
                title = { Text("天气查询", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(SurfaceDark)
        ) {
            WaveBackground(progress = waveProgress, color = WaterCyan)
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            if (com.lurecalendar.app.BuildConfig.QWEATHER_API_KEY.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().glassBackground(16),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("未配置和风天气 (QWeather) API KEY", fontWeight = FontWeight.Bold, color = WarningRed)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("请在 local.properties 中添加：QWEATHER_API_KEY=你的Key", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (state.provider == WeatherProvider.JUHE || (state.provider == WeatherProvider.AUTO && com.lurecalendar.app.BuildConfig.JUHE_WEATHER_KEY.isNotBlank())) "城市" else "城市 / 经纬度(经度,纬度)", color = Color.White.copy(alpha = 0.7f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = WaterCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = WaterCyan
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = viewModel::search,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = WaterCyan)
                ) {
                    Text("查询", color = Color.White)
                }
                OutlinedButton(
                    onClick = viewModel::refresh,
                    modifier = Modifier.weight(1f),
                    enabled = state.resolvedLocation != null,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WaterCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WaterCyan.copy(alpha = if (state.resolvedLocation != null) 1f else 0.3f))
                ) {
                    Text("刷新")
                }
            }

            OutlinedButton(
                onClick = {
                    if (hasLocationPermission) {
                        viewModel.useCurrentLocation()
                    } else if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        showRationale = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WaterCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, WaterCyan.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = WaterCyan)
                Text("使用当前位置", modifier = Modifier.padding(start = 8.dp))
            }

            state.errorMessage?.let { msg ->
                Text(text = msg, color = WarningRed)
            }

            Box(modifier = Modifier.fillMaxWidth().glassBackground(16)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("预报参数配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("数据来源", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                        var providerExpanded by remember { mutableStateOf(false) }
                        val hasJuhe = com.lurecalendar.app.BuildConfig.JUHE_WEATHER_KEY.isNotBlank()
                        val hasQWeather = com.lurecalendar.app.BuildConfig.QWEATHER_API_KEY.isNotBlank()
                        Box {
                            OutlinedButton(
                                onClick = { providerExpanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WaterCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WaterCyan.copy(alpha = 0.5f))
                            ) {
                                Text(when(state.provider) {
                                    WeatherProvider.AUTO -> "自动(优先聚合)"
                                    WeatherProvider.JUHE -> "聚合数据 (JUHE)"
                                    WeatherProvider.QWEATHER -> "和风天气 (QWeather)"
                                })
                            }
                            DropdownMenu(
                                expanded = providerExpanded,
                                onDismissRequest = { providerExpanded = false }
                            ) {
                                WeatherProvider.entries.forEach { provider ->
                                    val enabled = when (provider) {
                                        WeatherProvider.AUTO -> hasJuhe || hasQWeather
                                        WeatherProvider.JUHE -> hasJuhe
                                        WeatherProvider.QWEATHER -> hasQWeather
                                    }
                                    DropdownMenuItem(
                                        text = { 
                                            Text(when(provider) {
                                                WeatherProvider.AUTO -> "自动(优先聚合)"
                                                WeatherProvider.JUHE -> "聚合数据 (JUHE)"
                                                WeatherProvider.QWEATHER -> "和风天气 (QWeather)"
                                            })
                                        },
                                        enabled = enabled,
                                        onClick = {
                                            viewModel.updateProvider(provider)
                                            providerExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("水温策略 (数据源)", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                        var waterTypeExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { waterTypeExpanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WaterCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WaterCyan.copy(alpha = 0.5f))
                            ) {
                                Text(state.waterType)
                            }
                            DropdownMenu(
                                expanded = waterTypeExpanded,
                                onDismissRequest = { waterTypeExpanded = false }
                            ) {
                                listOf("海水", "湖泊", "河流").forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            viewModel.updateWaterType(type)
                                            waterTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("目标鱼种 (鱼情权重)", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                        var speciesExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { speciesExpanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WaterCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WaterCyan.copy(alpha = 0.5f))
                            ) {
                                Text(state.targetSpecies)
                            }
                            DropdownMenu(
                                expanded = speciesExpanded,
                                onDismissRequest = { speciesExpanded = false }
                            ) {
                                listOf("翘嘴", "鳜鱼", "黑鱼", "鲈鱼").forEach { sp ->
                                    DropdownMenuItem(
                                        text = { Text(sp) },
                                        onClick = {
                                            viewModel.updateTargetSpecies(sp)
                                            speciesExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().glassBackground(16)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("实时天气", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    val resolved = state.resolvedLocation
                    val locName = resolved?.let { 
                        it.name + (it.adm2?.let { a -> " • $a" } ?: "")
                    } ?: "未选择位置"
                    
                    Text(locName, color = WaterCyan, fontWeight = FontWeight.Bold)
                    
                    state.data?.current?.let { current ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${current.temperature}°C  湿度${current.humidity}%  气压${current.pressure}hPa", color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("风${current.windDirection} ${current.windSpeed}m/s  能见度${current.visibility}km  降水${current.precipitation}mm", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            // 天文与禁渔期卡片
            state.astronomy?.let { astro ->
                Box(modifier = Modifier.fillMaxWidth().glassBackground(16)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("日出月相", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFFB300))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("日出 ${astro.sunrise ?: "—"}    日落 ${astro.sunset ?: "—"}", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, tint = Color(0xFFB39DDB))
                            Spacer(modifier = Modifier.width(6.dp))
                            val moonText = buildString {
                                append(astro.moonPhase ?: "—")
                                astro.moonIllumination?.let { append("  亮面 ${(it * 100).toInt()}%") }
                                astro.lunarDate?.let { append("  $it") }
                            }
                            Text(moonText, color = Color.White)
                        }
                        if ((astro.isClosedSeason ?: 0) == 1 && !astro.closedSeasonNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = WarningRed)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(astro.closedSeasonNote, color = WarningRed, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 智能饵推荐
            if (state.smartLures.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().glassBackground(16)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("今日钓饵推荐 (${state.targetSpecies})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        state.smartLures.take(4).forEach { lure ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${lure.name}" + (lure.category?.let { "  · $it" } ?: ""),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val tip = listOfNotNull(
                                        lure.swimLayer?.let { "泳层:$it" },
                                        lure.suitableWaterTemp?.let { "水温:$it" },
                                        lure.weightRange?.let { "克重:$it" }
                                    ).joinToString("  ")
                                    if (tip.isNotBlank()) {
                                        Text(tip, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                lure.matchScore?.let { s ->
                                    Box(
                                        modifier = Modifier
                                            .background(WaterCyan.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("${s}分", color = WaterCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                            Divider(color = Color.White.copy(alpha = 0.08f))
                        }
                    }
                }
            }

            val hourly = state.data?.hourly.orEmpty()
            val insight = remember(hourly, state.targetSpecies) { FishInsightGenerator.generate(hourly, state.targetSpecies) }
            if (hourly.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().glassBackground(16)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("钓鱼指数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        val first = hourly.first()
                        val idx = first.fishingIndex ?: 0
                        val wt = first.waterTemperature
                        Text("当前：$idx/100  估算水温：${wt?.let { "${it}°C" } ?: "—"}", color = Color.White)
                        if (insight.bestHours.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("推荐时段：${insight.bestHours.take(4).joinToString("、")}", color = WaterCyan)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().glassBackground(16)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(insight.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(insight.suggestion, color = Color.White.copy(alpha = 0.85f))
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().glassBackground(16)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("未来水温预报(估算)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        hourly.take(6).forEach { h ->
                            val time = h.time.substringAfter('T').take(5)
                            val wtt = h.waterTemperature?.let { "${it}°C" } ?: "—"
                            val ix = h.fishingIndex?.toString() ?: "—"
                            Text("$time  水温$wtt  指数$ix", color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().glassBackground(16)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("未来15天预报", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    val daily = state.data?.daily.orEmpty()
                    if (daily.isEmpty()) {
                        Text("暂无数据", color = Color.Gray)
                    } else {
                        daily.take(7).forEach { d ->
                            key(d.date) {
                                Text("${d.date}  ${d.tempMin}~${d.tempMax}°C  湿度${d.humidity}%  气压${d.pressure}hPa", color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            if (state.isLoading) {
                Text("加载中…", color = Color.Gray)
            }
        }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("位置权限说明") },
            text = { Text("用于一键获取你当前所在位置的天气。") },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) { Text("继续") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRationale = false }) { Text("取消") }
            }
        )
    }
}

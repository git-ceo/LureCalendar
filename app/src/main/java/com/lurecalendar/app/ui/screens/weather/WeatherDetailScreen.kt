package com.lurecalendar.app.ui.screens.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurecalendar.app.domain.model.DailyWeather
import com.lurecalendar.app.domain.model.HourlyWeather
import com.lurecalendar.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    onBack: () -> Unit = {},
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("天气详情", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = SurfaceDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. 当前天气卡片
            item {
                CurrentWeatherCard(state)
            }

            // 2. 24小时温度折线图
            val hourly = state.data?.hourly.orEmpty()
            if (hourly.isNotEmpty()) {
                item {
                    HourlyTemperatureChart(hourly)
                }

                // 3. 气压趋势图
                item {
                    PressureTrendChart(hourly)
                }

                // 4. 风速趋势图
                item {
                    WindSpeedChart(hourly)
                }
            }

            // 5. 7天天气预报列表
            val daily = state.data?.daily.orEmpty()
            if (daily.isNotEmpty()) {
                item {
                    DailyForecastSection(daily)
                }
            }

            // 6. 详细气象数据表格
            item {
                DetailedMetricsSection(state)
            }
        }
    }
}

@Composable
private fun CurrentWeatherCard(state: WeatherUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 城市名
            val locName = state.resolvedLocation?.let {
                it.name + (it.adm2?.let { a -> " · $a" } ?: "")
            } ?: "未选择位置"
            Text(locName, color = WaterCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(12.dp))

            state.data?.current?.let { current ->
                // 温度大字 + 天气描述
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${current.temperature.toInt()}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "°C",
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = state.data?.hourly?.firstOrNull()?.weatherText ?: "—",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 详细数据行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem("湿度", "${current.humidity}%")
                    MetricItem("气压", "${current.pressure.toInt()}hPa")
                    MetricItem("能见度", "${current.visibility}km")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem("风速", "${current.windSpeed}m/s")
                    MetricItem("风向", current.windDirection)
                    MetricItem("降水", "${current.precipitation}mm")
                }

                // 日出日落
                state.astronomy?.let { astro ->
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("日出", astro.sunrise ?: "—")
                        MetricItem("日落", astro.sunset ?: "—")
                        MetricItem("月相", astro.moonPhase ?: "—")
                    }
                }
            } ?: run {
                Text("暂无天气数据", color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

@Composable
private fun HourlyTemperatureChart(hourly: List<HourlyWeather>) {
    val displayHours = hourly.take(24)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "24小时温度趋势",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Canvas折线图
            val temperatures = displayHours.map { it.temperature }
            val minTemp = (temperatures.minOrNull() ?: 0f) - 2f
            val maxTemp = (temperatures.maxOrNull() ?: 30f) + 2f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 20f
                val chartWidth = width - padding * 2
                val chartHeight = height - padding * 2
                val stepX = chartWidth / (temperatures.size - 1).coerceAtLeast(1)
                val range = (maxTemp - minTemp).coerceAtLeast(1f)

                // 绘制网格线
                for (i in 0..4) {
                    val y = padding + chartHeight * i / 4
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1f
                    )
                }

                // 绘制折线
                val path = Path()
                temperatures.forEachIndexed { index, temp ->
                    val x = padding + index * stepX
                    val y = padding + chartHeight * (1f - (temp - minTemp) / range)
                    if (index == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = WaterCyan,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // 绘制数据点
                temperatures.forEachIndexed { index, temp ->
                    val x = padding + index * stepX
                    val y = padding + chartHeight * (1f - (temp - minTemp) / range)
                    drawCircle(
                        color = WaterCyan,
                        radius = 4f,
                        center = Offset(x, y)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 时间标签
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayHours) { hour ->
                    val time = hour.time.substringAfter('T').take(5)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(48.dp)
                    ) {
                        Text(
                            text = "${hour.temperature.toInt()}°",
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = time,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        hour.windDirection?.let {
                            Text(
                                text = it,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PressureTrendChart(hourly: List<HourlyWeather>) {
    val displayHours = hourly.take(48)
    val pressures = displayHours.mapNotNull { it.pressure }
    if (pressures.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "气压趋势 (${pressures.size}小时)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            // 气压变化描述
            val firstPressure = pressures.first()
            val lastPressure = pressures.last()
            val diff = lastPressure - firstPressure
            val trendText = when {
                diff < -3f -> "骤降 ↓ 鱼口活跃窗口期"
                diff < -1f -> "缓降 ↘ 鱼类活性较高"
                diff > 3f -> "骤升 ↑ 鱼口可能闭合"
                diff > 1f -> "缓升 ↗ 稳定期"
                else -> "稳定 → 正常咬口"
            }
            val trendColor = when {
                diff < -3f -> NatureGreen
                diff < -1f -> WaterCyan
                diff > 3f -> WarningRed
                else -> Color.White.copy(alpha = 0.7f)
            }
            Text(trendText, color = trendColor, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(12.dp))

            val minP = pressures.min() - 2f
            val maxP = pressures.max() + 2f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 20f
                val chartWidth = width - padding * 2
                val chartHeight = height - padding * 2
                val stepX = chartWidth / (pressures.size - 1).coerceAtLeast(1)
                val range = (maxP - minP).coerceAtLeast(1f)

                // 网格
                for (i in 0..3) {
                    val y = padding + chartHeight * i / 3
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(padding, y),
                        end = Offset(width - padding, y)
                    )
                }

                // 折线
                val path = Path()
                pressures.forEachIndexed { index, p ->
                    val x = padding + index * stepX
                    val y = padding + chartHeight * (1f - (p - minP) / range)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = SandYellow,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${pressures.first().toInt()}hPa", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                Text("当前 → 未来", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                Text("${pressures.last().toInt()}hPa", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun WindSpeedChart(hourly: List<HourlyWeather>) {
    val displayHours = hourly.take(24)
    val winds = displayHours.mapNotNull { it.windSpeed }
    if (winds.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "24小时风速变化",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            // 标注微风时段
            val calmHours = displayHours.filter { (it.windSpeed ?: 99f) <= 3f }
            if (calmHours.isNotEmpty()) {
                val calmTimes = calmHours.take(4).joinToString("、") {
                    it.time.substringAfter('T').take(5)
                }
                Text(
                    "微风时段(≤3m/s): $calmTimes",
                    color = NatureGreen,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val minW = 0f
            val maxW = (winds.max() + 2f).coerceAtLeast(8f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 20f
                val chartWidth = width - padding * 2
                val chartHeight = height - padding * 2
                val stepX = chartWidth / (winds.size - 1).coerceAtLeast(1)
                val range = (maxW - minW).coerceAtLeast(1f)

                // 适合出钓的区域 (0-3 m/s)
                val calmY = padding + chartHeight * (1f - 3f / range)
                drawRect(
                    color = NatureGreen.copy(alpha = 0.08f),
                    topLeft = Offset(padding, calmY),
                    size = androidx.compose.ui.geometry.Size(chartWidth, height - padding - calmY)
                )

                // 折线
                val path = Path()
                winds.forEachIndexed { index, w ->
                    val x = padding + index * stepX
                    val y = padding + chartHeight * (1f - (w - minW) / range)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF64B5F6),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("最小 ${winds.min().toInt()}m/s", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                Text("最大 ${winds.max().toInt()}m/s", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DailyForecastSection(daily: List<DailyWeather>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "7天天气预报",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            daily.take(7).forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 日期
                    Text(
                        text = day.date.takeLast(5),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        modifier = Modifier.width(56.dp)
                    )

                    // 温度范围条
                    val tempRange = day.tempMax - day.tempMin
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${day.tempMin.toInt()}°",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .padding(horizontal = 8.dp)
                                    .background(
                                        color = WaterCyan.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Text(
                                "${day.tempMax.toInt()}°",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // 风速
                    Text(
                        text = "${day.windSpeed.toInt()}m/s",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.width(42.dp),
                        textAlign = TextAlign.End
                    )
                }
                if (day != daily.take(7).last()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
private fun DetailedMetricsSection(state: WeatherUiState) {
    val current = state.data?.current ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "详细气象数据",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 第一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailMetricCard("气压", "${current.pressure.toInt()}", "hPa")
                DetailMetricCard("湿度", "${current.humidity}", "%")
                DetailMetricCard("能见度", "${current.visibility}", "km")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 第二行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailMetricCard("风速", "${current.windSpeed}", "m/s")
                DetailMetricCard("风向", current.windDirection, "")
                DetailMetricCard("降水", "${current.precipitation}", "mm")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 第三行 - 额外数据
            val firstHourly = state.data?.hourly?.firstOrNull()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailMetricCard(
                    "估算水温",
                    firstHourly?.waterTemperature?.let { "${it.toInt()}" } ?: "—",
                    "°C"
                )
                DetailMetricCard(
                    "钓鱼指数",
                    firstHourly?.fishingIndex?.toString() ?: "—",
                    "/100"
                )
                DetailMetricCard(
                    "降水概率",
                    firstHourly?.precipitationProbability?.toString() ?: "—",
                    "%"
                )
            }

            // UV指数（来自daily）
            state.data?.daily?.firstOrNull()?.let { today ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailMetricCard("紫外线", "${today.uvIndex}", "级")
                    DetailMetricCard("日湿度", "${today.humidity}", "%")
                    DetailMetricCard("日气压", "${today.pressure.toInt()}", "hPa")
                }
            }
        }
    }
}

@Composable
private fun DetailMetricCard(label: String, value: String, unit: String) {
    Card(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = WaterCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                    )
                }
            }
        }
    }
}

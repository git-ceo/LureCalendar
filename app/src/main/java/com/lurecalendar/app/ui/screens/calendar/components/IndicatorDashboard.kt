package com.lurecalendar.app.ui.screens.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurecalendar.app.ui.theme.WaterCyan
import com.lurecalendar.app.ui.theme.SandYellow
import kotlin.math.roundToInt

@Composable
fun IndicatorDashboard(
    pressure: Float?,
    pressureTrend: String,
    windSpeed: Float?,
    windDirection: String,
    moonPhase: String,
    lunarDate: String,
    sunrise: String,
    sunset: String,
    prevWindSpeed: Float? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "关键指标",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // 第一行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 气压卡片
            IndicatorCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Compress,
                iconColor = WaterCyan,
                title = "气压",
                value = if (pressure != null) "${pressure.roundToInt()} hPa" else "-- hPa",
                subtitle = "${getTrendArrow(pressureTrend)} $pressureTrend",
                trendIcon = when (pressureTrend) {
                    "上升" -> Icons.Default.ArrowUpward
                    "下降" -> Icons.Default.ArrowDownward
                    else -> Icons.Default.Remove
                },
                trendColor = when (pressureTrend) {
                    "上升" -> Color(0xFF4CAF50)
                    "下降" -> Color(0xFFF44336)
                    else -> Color.White.copy(alpha = 0.5f)
                }
            )
            // 风速风向卡片
            IndicatorCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Air,
                iconColor = Color(0xFF64B5F6),
                title = "风速风向",
                value = if (windSpeed != null) "${String.format("%.1f", windSpeed)} m/s" else "-- m/s",
                subtitle = "$windDirection ${getWindLevel(windSpeed)}",
                trendIcon = if (prevWindSpeed != null && windSpeed != null) {
                    val diff = windSpeed - prevWindSpeed
                    when {
                        diff >= 1f -> Icons.Default.ArrowUpward
                        diff <= -1f -> Icons.Default.ArrowDownward
                        else -> Icons.Default.Remove
                    }
                } else null,
                trendColor = if (prevWindSpeed != null && windSpeed != null) {
                    val diff = windSpeed - prevWindSpeed
                    when {
                        diff >= 1f -> Color(0xFFF44336)
                        diff <= -1f -> Color(0xFF4CAF50)
                        else -> Color.White.copy(alpha = 0.5f)
                    }
                } else Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 第二行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 月相卡片
            IndicatorCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.NightsStay,
                iconColor = SandYellow,
                title = "月相",
                value = "${getMoonEmoji(moonPhase)} $moonPhase",
                subtitle = lunarDate
            )
            // 日出日落卡片
            IndicatorCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LightMode,
                iconColor = Color(0xFFFFB74D),
                title = "日出日落",
                value = "↑$sunrise  ↓$sunset",
                subtitle = calcDaylight(sunrise, sunset)
            )
        }
    }
}

@Composable
fun IndicatorCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    subtitle: String,
    trendIcon: ImageVector? = null,
    trendColor: Color = Color.White.copy(alpha = 0.5f)
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1
                )
                if (trendIcon != null) {
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = "趋势",
                        tint = trendColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

private fun getTrendArrow(trend: String): String = when (trend) {
    "上升" -> "↑"
    "下降" -> "↓"
    else -> "→"
}

private fun getWindLevel(windSpeed: Float?): String {
    val ws = windSpeed ?: return ""
    return when {
        ws < 0.3f -> "0级"
        ws < 1.6f -> "1级"
        ws < 3.4f -> "2级"
        ws < 5.5f -> "3级"
        ws < 8.0f -> "4级"
        ws < 10.8f -> "5级"
        else -> "6级+"
    }
}

private fun getMoonEmoji(moonPhase: String): String = when {
    moonPhase.contains("新月") || moonPhase.contains("朔") -> "🌑"
    moonPhase.contains("蛾眉") || moonPhase.contains("娥眉") -> "🌒"
    moonPhase.contains("上弦") -> "🌓"
    moonPhase.contains("盈凸") -> "🌔"
    moonPhase.contains("满月") || moonPhase.contains("望") -> "🌕"
    moonPhase.contains("亏凸") -> "🌖"
    moonPhase.contains("下弦") -> "🌗"
    moonPhase.contains("残月") -> "🌘"
    else -> "🌙"
}

private fun calcDaylight(sunrise: String, sunset: String): String {
    return try {
        val sunriseMinutes = sunrise.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val sunsetMinutes = sunset.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val diff = sunsetMinutes - sunriseMinutes
        if (diff > 0) "日照 ${diff / 60}h${diff % 60}min" else "日照时长计算中"
    } catch (_: Exception) {
        "日照时长计算中"
    }
}

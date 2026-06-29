package com.lurecalendar.app.ui.screens.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurecalendar.app.ui.screens.calendar.CalendarViewModel.HourlyForecast
import com.lurecalendar.app.ui.theme.IndexHigh
import com.lurecalendar.app.ui.theme.IndexLow
import com.lurecalendar.app.ui.theme.IndexMid

@Composable
fun HourlyForecastRow(
    forecasts: List<HourlyForecast>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "逐小时预测",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            itemsIndexed(forecasts) { index, forecast ->
                val prevTemp = if (index > 0) forecasts[index - 1].temperature else null
                HourlyForecastItem(forecast = forecast, prevTemperature = prevTemp)
            }
        }
    }
}

@Composable
fun HourlyForecastItem(
    forecast: HourlyForecast,
    modifier: Modifier = Modifier,
    prevTemperature: Float? = null
) {
    val indexColor = when {
        forecast.fishingIndex >= 70 -> IndexHigh
        forecast.fishingIndex >= 50 -> IndexMid
        else -> IndexLow
    }

    Column(
        modifier = modifier
            .width(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 时间
        Text(
            text = forecast.time,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 温度 + 趋势箭头
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${forecast.temperature.toInt()}°",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            if (prevTemperature != null) {
                val tempDiff = forecast.temperature - prevTemperature
                if (tempDiff >= 1f) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "升温",
                        tint = Color(0xFFFF7043),
                        modifier = Modifier.size(12.dp)
                    )
                } else if (tempDiff <= -1f) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = "降温",
                        tint = Color(0xFF42A5F5),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 天气小图标
        Text(
            text = getWeatherEmoji(forecast.weatherText),
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 钓鱼指数色块
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(indexColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${forecast.fishingIndex}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = indexColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 推荐饵料
        Text(
            text = forecast.lureRecommend,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

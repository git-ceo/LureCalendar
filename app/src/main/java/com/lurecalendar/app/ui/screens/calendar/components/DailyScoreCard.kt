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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurecalendar.app.ui.screens.calendar.CalendarViewModel.DailyScore
import com.lurecalendar.app.ui.theme.IndexHigh
import com.lurecalendar.app.ui.theme.IndexLow
import com.lurecalendar.app.ui.theme.IndexMid
import com.lurecalendar.app.ui.theme.IndexPerfect
import com.lurecalendar.app.ui.theme.WaterCyan

@Composable
fun DailyScoreCardRow(
    dailyScores: List<DailyScore>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "未来3天路亚评分",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            itemsIndexed(dailyScores) { index, day ->
                val prevDay = if (index > 0) dailyScores[index - 1] else null
                DailyScoreCard(
                    dailyScore = day,
                    prevTempMax = prevDay?.tempMax,
                    prevScore = prevDay?.lureScore
                )
            }
        }
    }
}

@Composable
fun DailyScoreCard(
    dailyScore: DailyScore,
    modifier: Modifier = Modifier,
    prevTempMax: Float? = null,
    prevScore: Int? = null
) {
    val scoreColor = when {
        dailyScore.lureScore >= 85 -> IndexPerfect
        dailyScore.lureScore >= 70 -> IndexHigh
        dailyScore.lureScore >= 55 -> IndexMid
        else -> IndexLow
    }

    Card(
        modifier = modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 日期 + 星期
            Text(
                text = dailyScore.date,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = dailyScore.dayOfWeek,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 天气图标 + 温度 + 趋势箭头
            Text(
                text = getWeatherEmoji(dailyScore.weatherText),
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${dailyScore.tempMax.toInt()}° / ${dailyScore.tempMin.toInt()}°",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                // 温度趋势图标
                if (prevTempMax != null) {
                    val tempDiff = dailyScore.tempMax - prevTempMax
                    val (trendIcon, trendColor) = when {
                        tempDiff >= 2f -> Icons.Default.TrendingUp to Color(0xFFFF7043)
                        tempDiff <= -2f -> Icons.Default.TrendingDown to Color(0xFF42A5F5)
                        else -> Icons.Default.TrendingFlat to Color.White.copy(alpha = 0.5f)
                    }
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = "温度趋势",
                        tint = trendColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 路亚评分 + 趋势箭头
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${dailyScore.lureScore}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                // 评分趋势图标
                if (prevScore != null) {
                    val scoreDiff = dailyScore.lureScore - prevScore
                    val (scoreIcon, scoreIconColor) = when {
                        scoreDiff >= 5 -> Icons.Default.TrendingUp to Color(0xFF4CAF50)
                        scoreDiff <= -5 -> Icons.Default.TrendingDown to Color(0xFFF44336)
                        else -> Icons.Default.TrendingFlat to Color.White.copy(alpha = 0.5f)
                    }
                    Icon(
                        imageVector = scoreIcon,
                        contentDescription = "评分趋势",
                        tint = scoreIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "分",
                fontSize = 11.sp,
                color = scoreColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 最佳窗口时段
            Text(
                text = dailyScore.bestWindow,
                fontSize = 10.sp,
                color = WaterCyan,
                maxLines = 1
            )
        }
    }
}

fun getWeatherEmoji(weatherText: String): String = when {
    weatherText.contains("晴") -> "☀️"
    weatherText.contains("多云") -> "⛅"
    weatherText.contains("阴") -> "☁️"
    weatherText.contains("雨") -> "🌧️"
    weatherText.contains("雪") -> "❄️"
    weatherText.contains("雾") -> "🌫️"
    weatherText.contains("风") -> "💨"
    else -> "🌤️"
}

package com.lurecalendar.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurecalendar.app.ui.screens.calendar.components.getWeatherEmoji
import com.lurecalendar.app.ui.theme.NatureGreen
import com.lurecalendar.app.ui.theme.SandYellow
import com.lurecalendar.app.ui.theme.WaterCyan
import com.lurecalendar.app.ui.theme.WarningRed

/**
 * 周视图中每天的信息
 */
data class DayInfo(
    val dayOfMonth: Int,        // 日期数字，用于选中标识
    val date: String,           // "5月16日"
    val dayOfWeek: String,      // "周五"
    val weatherText: String,
    val tempMax: Float,
    val tempMin: Float,
    val lureScore: Int,
    val bestWindow: String      // "05:30-07:30"
)

@Composable
fun CalendarWeekView(
    weekDays: List<DayInfo>,
    selectedDay: Int?,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "本周概览",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        weekDays.forEach { dayInfo ->
            WeekDayRow(
                dayInfo = dayInfo,
                isSelected = dayInfo.dayOfMonth == selectedDay,
                onClick = { onDayClick(dayInfo.dayOfMonth) }
            )
        }
    }
}

@Composable
private fun WeekDayRow(
    dayInfo: DayInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        dayInfo.lureScore >= 70 -> NatureGreen
        dayInfo.lureScore >= 50 -> SandYellow
        else -> WarningRed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                WaterCyan.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 3.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 日期 + 星期
            Column(
                modifier = Modifier.width(64.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = dayInfo.date,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = dayInfo.dayOfWeek,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 天气图标 + 温度
            Row(
                modifier = Modifier.width(80.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getWeatherEmoji(dayInfo.weatherText),
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${dayInfo.tempMax.toInt()}°/${dayInfo.tempMin.toInt()}°",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 评分圆点 + 数字
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(52.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(scoreColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${dayInfo.lureScore}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 最佳窗口时段
            Text(
                text = dayInfo.bestWindow,
                fontSize = 11.sp,
                color = WaterCyan,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

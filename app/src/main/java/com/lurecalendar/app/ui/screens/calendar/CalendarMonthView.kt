package com.lurecalendar.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.lurecalendar.app.ui.theme.NatureGreen
import com.lurecalendar.app.ui.theme.SandYellow
import com.lurecalendar.app.ui.theme.WaterCyan
import com.lurecalendar.app.ui.theme.WarningRed
import java.util.Calendar

@Composable
fun CalendarMonthView(
    year: Int,
    month: Int,
    dailyScores: Map<Int, Int>,  // day -> score
    selectedDay: Int?,
    onDayClick: (Int) -> Unit,
    onMonthChange: (Int, Int) -> Unit,  // year, month
    modifier: Modifier = Modifier
) {
    val today = Calendar.getInstance()
    val isCurrentMonth = today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) + 1 == month
    val todayDate = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else -1

    // 计算当月天数和起始星期
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday

    val weekDayHeaders = listOf("日", "一", "二", "三", "四", "五", "六")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 月份切换头部
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newMonth = if (month == 1) 12 else month - 1
                val newYear = if (month == 1) year - 1 else year
                onMonthChange(newYear, newMonth)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上一月",
                    tint = Color.White
                )
            }

            Text(
                text = "${year}年${month}月",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(onClick = {
                val newMonth = if (month == 12) 1 else month + 1
                val newYear = if (month == 12) year + 1 else year
                onMonthChange(newYear, newMonth)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下一月",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 星期标题行
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDayHeaders.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 日期网格 (6行 x 7列)
        val totalCells = 42 // 6 weeks * 7 days
        val startOffset = firstDayOfWeek - 1 // Sunday = 0 offset

        for (row in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - startOffset + 1

                    if (day in 1..daysInMonth) {
                        DayCell(
                            day = day,
                            score = dailyScores[day],
                            isToday = day == todayDate,
                            isSelected = day == selectedDay,
                            onClick = { onDayClick(day) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // 空格占位
                        Box(modifier = Modifier.weight(1f).height(52.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    score: Int?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor = when {
        score == null -> Color.Gray.copy(alpha = 0.4f)
        score >= 70 -> NatureGreen
        score >= 50 -> SandYellow
        else -> WarningRed
    }

    val borderModifier = when {
        isSelected -> Modifier.border(2.dp, WaterCyan, RoundedCornerShape(10.dp))
        isToday -> Modifier.border(1.5.dp, WaterCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
        else -> Modifier
    }

    val bgModifier = if (isSelected) {
        Modifier.background(WaterCyan.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(bgModifier)
            .then(borderModifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$day",
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) WaterCyan else Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            // 评分色块圆点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

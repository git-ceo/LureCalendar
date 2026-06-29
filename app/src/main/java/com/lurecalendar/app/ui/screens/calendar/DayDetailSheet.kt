package com.lurecalendar.app.ui.screens.calendar

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurecalendar.app.ui.theme.IndexHigh
import com.lurecalendar.app.ui.theme.IndexLow
import com.lurecalendar.app.ui.theme.IndexMid
import com.lurecalendar.app.ui.theme.IndexPerfect
import com.lurecalendar.app.ui.theme.NatureGreen
import com.lurecalendar.app.ui.theme.SandYellow
import com.lurecalendar.app.ui.theme.WaterCyan
import com.lurecalendar.app.ui.theme.WarningRed

/**
 * 最佳出钓窗口数据
 */
data class FishingWindow(
    val timeRange: String,      // "05:30-07:30"
    val reason: String,         // "微风低光，适合水面系"
    val score: Int              // 该窗口评分
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
    date: String,
    score: Int,
    scoreLabel: String,
    pressureHistory: List<Float>,
    pressureForecast: List<Float>,
    bestWindows: List<FishingWindow>,
    hourlyLureRecommends: Map<String, List<String>>,  // "清晨" -> ["水面系", "米诺"]
    topSpecies: List<Pair<String, Int>>,  // [("翘嘴", 85), ("鲈鱼", 72)]
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scoreColor = when {
        score >= 85 -> IndexPerfect
        score >= 70 -> IndexHigh
        score >= 55 -> IndexMid
        else -> IndexLow
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // 1. 日期标题 + 评分大字
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = date,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = scoreLabel,
                        fontSize = 14.sp,
                        color = scoreColor
                    )
                }

                // 评分大字
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "综合评分",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // 2. 气压变化曲线 - PressureChart 尚未创建，先用占位 Box
            // TODO: 后续集成 PressureChart 组件 (Task 8)
            SectionTitle(icon = Icons.Default.WaterDrop, title = "气压变化趋势")
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (pressureHistory.isNotEmpty() || pressureForecast.isNotEmpty()) {
                    // 简易气压数值展示
                    val allPressures = pressureHistory + pressureForecast
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "气压范围: ${allPressures.minOrNull()?.toInt() ?: "--"} ~ ${allPressures.maxOrNull()?.toInt() ?: "--"} hPa",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "气压图表后续集成",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    Text(
                        text = "暂无气压数据",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. 最佳出钓窗口
            SectionTitle(icon = Icons.Default.AccessTime, title = "最佳出钓窗口")
            Spacer(modifier = Modifier.height(8.dp))

            if (bestWindows.isEmpty()) {
                Text(
                    text = "今日无明显适钓窗口",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                bestWindows.forEach { window ->
                    FishingWindowCard(window = window)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. 各时段饵料推荐
            SectionTitle(icon = Icons.Default.Star, title = "各时段饵料推荐")
            Spacer(modifier = Modifier.height(8.dp))

            val timeSlotOrder = listOf("清晨", "上午", "中午", "下午", "黄昏", "夜间")
            val sortedRecommends = hourlyLureRecommends.entries
                .sortedBy { timeSlotOrder.indexOf(it.key).let { idx -> if (idx == -1) 99 else idx } }

            if (sortedRecommends.isEmpty()) {
                Text(
                    text = "暂无饵料推荐数据",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                sortedRecommends.forEach { (timeSlot, lures) ->
                    LureRecommendRow(timeSlot = timeSlot, lures = lures)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. 鱼种活跃排行
            if (topSpecies.isNotEmpty()) {
                SectionTitle(icon = Icons.Default.Star, title = "鱼种活跃排行")
                Spacer(modifier = Modifier.height(8.dp))

                topSpecies.forEachIndexed { index, (species, speciesScore) ->
                    SpeciesRankRow(
                        rank = index + 1,
                        species = species,
                        score = speciesScore
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WaterCyan,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FishingWindowCard(
    window: FishingWindow
) {
    val windowScoreColor = when {
        window.score >= 70 -> NatureGreen
        window.score >= 50 -> SandYellow
        else -> WarningRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间范围
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = window.timeRange,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WaterCyan
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = window.reason,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 窗口评分
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${window.score}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = windowScoreColor
                )
                Text(
                    text = "分",
                    fontSize = 10.sp,
                    color = windowScoreColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun LureRecommendRow(
    timeSlot: String,
    lures: List<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timeSlot,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = WaterCyan,
            modifier = Modifier.width(48.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lures.forEach { lure ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(WaterCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = lure,
                        fontSize = 12.sp,
                        color = WaterCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeciesRankRow(
    rank: Int,
    species: String,
    score: Int
) {
    val rankColor = when (rank) {
        1 -> IndexPerfect
        2 -> IndexHigh
        3 -> IndexMid
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名圆点
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(rankColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = species,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${score}分",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = rankColor
        )
    }
}

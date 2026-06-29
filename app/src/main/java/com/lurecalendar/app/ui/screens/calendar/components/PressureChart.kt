package com.lurecalendar.app.ui.screens.calendar.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurecalendar.app.ui.theme.ChartCurrentMark
import com.lurecalendar.app.ui.theme.ChartForecastBg
import com.lurecalendar.app.ui.theme.ChartForecastLine
import com.lurecalendar.app.ui.theme.ChartHistoryBg
import com.lurecalendar.app.ui.theme.ChartHistoryLine
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry

/**
 * 气压趋势折线图
 * X轴: 过去24h + 未来24h = 48个数据点
 * Y轴: 气压值 hPa
 * 使用 Vico 1.14.0 渲染折线 + Canvas 叠加区域标记
 */
@Composable
fun PressureChart(
    historyPressures: List<Float>,     // 过去24小时气压
    forecastPressures: List<Float>,    // 未来24小时预测
    modifier: Modifier = Modifier
) {
    val totalPoints = historyPressures.size + forecastPressures.size
    val historyCount = historyPressures.size

    // Vico ModelProducer - 双系列：历史 + 预测
    val modelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(historyPressures, forecastPressures) {
        // 系列1: 历史数据 (x: 0..historyCount-1)
        val historyEntries = historyPressures.mapIndexed { i, v ->
            FloatEntry(x = i.toFloat(), y = v)
        }
        // 系列2: 预测数据 (x: historyCount-1..totalPoints-1)
        // 从历史末尾开始连接，确保曲线连续
        val forecastEntries = mutableListOf<FloatEntry>()
        if (historyPressures.isNotEmpty()) {
            forecastEntries.add(FloatEntry(x = (historyCount - 1).toFloat(), y = historyPressures.last()))
        }
        forecastPressures.forEachIndexed { i, v ->
            forecastEntries.add(FloatEntry(x = (historyCount + i).toFloat(), y = v))
        }

        modelProducer.setEntries(listOf(historyEntries, forecastEntries))
    }

    Column(modifier = modifier) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "气压趋势",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "hPa",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // 图表主体
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            // Vico 折线图
            Chart(
                chart = lineChart(
                    lines = listOf(
                        // 系列1: 历史 - 实线，较粗
                        LineChart.LineSpec(
                            lineColor = ChartHistoryLine.toArgb(),
                            lineThicknessDp = 2.5f
                        ),
                        // 系列2: 预测 - 较浅颜色
                        LineChart.LineSpec(
                            lineColor = ChartForecastLine.toArgb(),
                            lineThicknessDp = 2f
                        )
                    )
                ),
                chartModelProducer = modelProducer,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier.fillMaxSize()
            )

            // Canvas 叠加层：当前时间标记 + 区域区分
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 48.dp, bottom = 24.dp, end = 8.dp, top = 8.dp)
            ) {
                if (totalPoints == 0) return@Canvas
                val midRatio = historyCount.toFloat() / totalPoints.toFloat()
                val midX = size.width * midRatio

                // 历史区域背景
                drawRect(
                    color = ChartHistoryBg,
                    topLeft = Offset.Zero,
                    size = Size(midX, size.height)
                )

                // 预测区域背景
                drawRect(
                    color = ChartForecastBg,
                    topLeft = Offset(midX, 0f),
                    size = Size(size.width - midX, size.height)
                )

                // 当前时间竖线（虚线）
                drawLine(
                    color = ChartCurrentMark,
                    start = Offset(midX, 0f),
                    end = Offset(midX, size.height),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8.dp.toPx(), 4.dp.toPx())
                    )
                )

                // "当前" 标记小圆点
                drawCircle(
                    color = ChartCurrentMark,
                    radius = 4.dp.toPx(),
                    center = Offset(midX, size.height * 0.5f)
                )
            }
        }

        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 历史图例
            Box(
                modifier = Modifier
                    .size(12.dp, 3.dp)
                    .background(ChartHistoryLine, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(4.dp))
            Text("过去24h", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

            Spacer(Modifier.width(16.dp))

            // 当前标记图例
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(ChartCurrentMark, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(4.dp))
            Text("当前", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

            Spacer(Modifier.width(16.dp))

            // 预测图例
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(12.dp, 3.dp)) {
                    drawLine(
                        color = ChartForecastLine,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Text("未来24h", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

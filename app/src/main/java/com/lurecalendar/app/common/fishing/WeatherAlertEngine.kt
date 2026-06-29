package com.lurecalendar.app.common.fishing

import kotlin.math.abs

/**
 * 异常天气预警引擎
 * 分析气象条件变化趋势，给出出钓建议预警
 */
class WeatherAlertEngine {

    enum class AlertLevel {
        POSITIVE,   // 正面提示（窗口期）
        NEUTRAL,    // 中性信息
        WARNING,    // 警告（不利条件）
        DANGER      // 严重警告（不建议出钓）
    }

    data class WeatherAlert(
        val level: AlertLevel,
        val title: String,          // 简短标题
        val message: String,        // 详细说明
        val suggestion: String      // 行动建议
    )

    /**
     * 分析气压序列，生成预警
     * @param pressureHistory 过去24小时气压列表（每小时一个值，从旧到新）
     * @param pressureForecast 未来24小时气压预测
     * @param currentWindSpeed 当前风速 m/s
     * @param precipitation 当前/预计降水量 mm
     * @param hour 当前小时 0-23
     */
    fun analyze(
        pressureHistory: List<Float>,
        pressureForecast: List<Float> = emptyList(),
        currentWindSpeed: Float = 0f,
        precipitation: Float = 0f,
        hour: Int = 12
    ): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()

        // === 气压类预警 ===
        analyzePressure(pressureHistory, alerts, hour)

        // === 风速预警 ===
        analyzeWind(currentWindSpeed, alerts)

        // === 降水预警 ===
        analyzePrecipitation(precipitation, alerts)

        // === 未来气压趋势预警 ===
        analyzeForecast(pressureForecast, pressureHistory, alerts)

        // 按严重程度排序：DANGER > WARNING > NEUTRAL > POSITIVE
        return alerts.sortedByDescending { it.level.ordinal }
    }

    /**
     * 气压变化分析
     */
    private fun analyzePressure(
        pressureHistory: List<Float>,
        alerts: MutableList<WeatherAlert>,
        hour: Int
    ) {
        if (pressureHistory.size < 3) return

        // 计算最近3小时气压变化
        val recentSize = minOf(3, pressureHistory.size)
        val recentPressures = pressureHistory.takeLast(recentSize)
        val delta3h = recentPressures.last() - recentPressures.first()

        // 3h气压降>5hPa → DANGER
        if (delta3h < -5f) {
            alerts.add(
                WeatherAlert(
                    level = AlertLevel.DANGER,
                    title = "气压骤降",
                    message = "气压骤降${String.format("%.1f", abs(delta3h))}hPa，鱼口闭锁，不建议出钓",
                    suggestion = "建议取消出钓计划，等待气压稳定后再考虑"
                )
            )
            return // 骤降时不再给其他气压建议
        }

        // 3h气压降3-5hPa → WARNING
        if (delta3h in -5f..-3f) {
            alerts.add(
                WeatherAlert(
                    level = AlertLevel.WARNING,
                    title = "气压明显下降",
                    message = "气压下降中（3h降${String.format("%.1f", abs(delta3h))}hPa），鱼活性降低，口会变轻",
                    suggestion = "放慢节奏，换小号饵，注意轻口信号"
                )
            )
        }

        // 持续高压(>1020)后缓降(3h降1-3hPa) → POSITIVE
        if (pressureHistory.size >= 6) {
            val avgPressure = pressureHistory.dropLast(recentSize).average().toFloat()
            if (avgPressure > 1020f && delta3h in -3f..-1f) {
                alerts.add(
                    WeatherAlert(
                        level = AlertLevel.POSITIVE,
                        title = "窗口期临近",
                        message = "稳定高压后缓降，窗口期临近，准备出钓",
                        suggestion = "抓紧时间出发！鱼口即将大开，优先用反应饵快速搜索"
                    )
                )
            }
        }

        // 气压连续6h稳定 + 清晨时段 → POSITIVE
        if (pressureHistory.size >= 6 && hour in 4..7) {
            val last6 = pressureHistory.takeLast(6)
            val maxP = last6.max()
            val minP = last6.min()
            if (maxP - minP < 2f) {
                alerts.add(
                    WeatherAlert(
                        level = AlertLevel.POSITIVE,
                        title = "清晨窗口",
                        message = "气压稳定，清晨微光期是最佳出钓时段",
                        suggestion = "立刻出发！优先选择水面系和米诺，搜索浅滩和岸边结构"
                    )
                )
            }
        }
    }

    /**
     * 风速分析
     */
    private fun analyzeWind(windSpeed: Float, alerts: MutableList<WeatherAlert>) {
        when {
            // 风速>8m/s → DANGER
            windSpeed > 8f -> alerts.add(
                WeatherAlert(
                    level = AlertLevel.DANGER,
                    title = "大风预警",
                    message = "风力5级以上（${String.format("%.1f", windSpeed)}m/s），不建议出钓，注意安全",
                    suggestion = "取消出钓，大风天气岸边作钓有落水风险，船钓更加危险"
                )
            )
            // 风速5-8m/s → WARNING
            windSpeed in 5f..8f -> alerts.add(
                WeatherAlert(
                    level = AlertLevel.WARNING,
                    title = "风力较大",
                    message = "风力4级（${String.format("%.1f", windSpeed)}m/s），可选择背风岸作钓，用重饵",
                    suggestion = "选择背风岸避风作钓，换用15g以上重饵保证抛投精准度"
                )
            )
        }
    }

    /**
     * 降水分析
     */
    private fun analyzePrecipitation(precipitation: Float, alerts: MutableList<WeatherAlert>) {
        when {
            // 降水>10mm → DANGER
            precipitation > 10f -> alerts.add(
                WeatherAlert(
                    level = AlertLevel.DANGER,
                    title = "暴雨预警",
                    message = "强降水天气（${String.format("%.1f", precipitation)}mm），不建议出钓",
                    suggestion = "暴雨天气水位暴涨、能见度极低，存在安全隐患，请取消出钓"
                )
            )
            // 降水5-10mm → WARNING
            precipitation in 5f..10f -> alerts.add(
                WeatherAlert(
                    level = AlertLevel.WARNING,
                    title = "中雨天气",
                    message = "中等降水（${String.format("%.1f", precipitation)}mm），水体浑浊度上升",
                    suggestion = "可以作钓但效果打折，建议使用强振动饵（VIB、复合亮片）"
                )
            )
            // 降水1-5mm → NEUTRAL
            precipitation in 1f..5f -> alerts.add(
                WeatherAlert(
                    level = AlertLevel.NEUTRAL,
                    title = "小雨天气",
                    message = "毛毛雨有利于路亚，鱼的警觉性降低",
                    suggestion = "小雨天气是路亚好时机，水面被雨点打破可掩护钓线，大胆作钓"
                )
            )
        }
    }

    /**
     * 未来气压趋势分析
     */
    private fun analyzeForecast(
        forecast: List<Float>,
        history: List<Float>,
        alerts: MutableList<WeatherAlert>
    ) {
        if (forecast.size < 3) return

        val forecast3h = forecast.take(3)
        val forecastDelta = forecast3h.last() - forecast3h.first()

        // 未来3小时气压将骤降
        if (forecastDelta < -5f && history.isNotEmpty()) {
            // 当前气压正常但即将骤降
            val currentP = history.last()
            if (currentP > 1005f) {
                alerts.add(
                    WeatherAlert(
                        level = AlertLevel.WARNING,
                        title = "气压即将骤降",
                        message = "未来3小时气压预计下降${String.format("%.1f", abs(forecastDelta))}hPa，窗口即将关闭",
                        suggestion = "抓紧当前时间作钓，气压骤降前鱼口可能短暂活跃，之后将闭口"
                    )
                )
            }
        }

        // 未来气压将回升（利好）
        if (forecastDelta > 3f && history.isNotEmpty()) {
            val recentDelta = if (history.size >= 3) history.last() - history[history.size - 3] else 0f
            if (recentDelta < -2f) {
                alerts.add(
                    WeatherAlert(
                        level = AlertLevel.POSITIVE,
                        title = "气压即将回升",
                        message = "经历下降后气压将回升，鱼活性有望恢复",
                        suggestion = "可以准备出钓装备，气压回升初期鱼口会逐渐变好"
                    )
                )
            }
        }
    }
}

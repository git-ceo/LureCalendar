package com.lurecalendar.app.common.fishing

import com.lurecalendar.app.domain.model.HourlyWeather

data class FishInsight(
    val title: String,
    val suggestion: String,
    val bestHours: List<String>
)

object FishInsightGenerator {

    private val lureEngine = LureRecommendEngine()
    private val alertEngine = WeatherAlertEngine()

    fun generate(hourly: List<HourlyWeather>, targetSpecies: String = "翘嘴"): FishInsight {
        if (hourly.isEmpty()) {
            return FishInsight(
                title = "鱼情解析 ($targetSpecies)",
                suggestion = "暂无逐小时数据",
                bestHours = emptyList()
            )
        }
        val withIndex = hourly.mapNotNull { h -> h.fishingIndex?.let { it to h } }
        if (withIndex.isEmpty()) {
            return FishInsight(
                title = "鱼情解析 ($targetSpecies)",
                suggestion = "暂无钓鱼指数数据",
                bestHours = emptyList()
            )
        }
        val best = withIndex.sortedByDescending { it.first }.take(6)
        val bestHours = best.map { it.second.time.substringAfter('T').take(5) }
        val peak = best.first().first
        val avg = (withIndex.sumOf { it.first } / withIndex.size)
        val trend = FishingIndexCalculator.compareTrend(withIndex.firstOrNull()?.first, withIndex.getOrNull(3)?.first)
        
        val speciesAdvice = when (targetSpecies) {
            "翘嘴" -> "翘嘴喜迎风，优先选择迎风口、明暗交界处，使用米诺/亮片主攻中上层。"
            "鳜鱼" -> "鳜鱼喜底避光，优先选择桥墩、乱石、水下结构，使用德州/铅头钩/VIB贴底慢跳。"
            "黑鱼" -> "黑鱼喜温喜草，优先选择重草区、浅滩水暖处，使用雷蛙/软虫打水面。"
            "鲈鱼" -> "鲈鱼捕食凶猛，优先选择水流交汇口、障碍物边缘，清晨傍晚为最佳窗口期。"
            else -> "优先选择清晨/傍晚与风口、结构边，风大用大克重/贴底，气压偏低放慢节奏。"
        }

        val suggestion = buildString {
            append("峰值$peak/100，均值$avg/100")
            if (trend != null) append("，未来数小时$trend")
            append("。\n策略：$speciesAdvice")
        }
        return FishInsight(
            title = "鱼情解析 ($targetSpecies)",
            suggestion = suggestion,
            bestHours = bestHours
        )
    }

    /**
     * 获取路亚饵料推荐列表
     * @param condition 当前天气条件
     * @param species 目标鱼种（可选）
     * @return 按置信度降序排列的饵料推荐列表（最多5个）
     */
    fun getLureRecommendations(
        condition: LureRecommendEngine.WeatherCondition,
        species: String? = null
    ): List<LureRecommendEngine.LureRecommendation> {
        return lureEngine.recommend(condition, species)
    }

    /**
     * 获取天气预警列表
     * @param pressureHistory 过去24小时气压列表（每小时一个值）
     * @param pressureForecast 未来24小时气压预测
     * @param windSpeed 当前风速 m/s
     * @param precipitation 当前/预计降水量 mm
     * @param hour 当前小时 0-23
     * @return 按严重程度降序排列的预警列表
     */
    fun getWeatherAlerts(
        pressureHistory: List<Float>,
        pressureForecast: List<Float> = emptyList(),
        windSpeed: Float = 0f,
        precipitation: Float = 0f,
        hour: Int = 12
    ): List<WeatherAlertEngine.WeatherAlert> {
        return alertEngine.analyze(
            pressureHistory = pressureHistory,
            pressureForecast = pressureForecast,
            currentWindSpeed = windSpeed,
            precipitation = precipitation,
            hour = hour
        )
    }
}


package com.lurecalendar.app.common.fishing

import com.lurecalendar.app.common.fish.FishSpecies

/**
 * 路亚目标鱼种活跃时段预测器
 * 根据鱼种习性 × 当前天气条件，预测每种鱼的活跃时段
 */
class SpeciesActivityPredictor {

    data class ActivityWindow(
        val startHour: Int,         // 开始小时 (0-23)
        val endHour: Int,           // 结束小时 (0-23)
        val activityLevel: String,  // "高活跃" / "中等" / "低活跃"
        val score: Int              // 0-100 活跃度评分
    )

    data class SpeciesPrediction(
        val species: FishSpecies,
        val overallScore: Int,                      // 今日综合活跃度 0-100
        val activityWindows: List<ActivityWindow>,  // 活跃时段列表
        val recommendedLures: List<String>,         // 推荐饵料
        val recommendedTechnique: String,           // 推荐操控手法
        val insight: String                         // 一句话建议
    )

    /**
     * 预测指定鱼种今日的活跃情况
     */
    fun predict(
        species: FishSpecies,
        temperature: Float,         // 气温℃
        waterTemperature: Float?,   // 水温℃
        pressure: Float,            // 气压hPa
        pressureTrend: Float,       // 气压趋势（3h变化量）
        windSpeed: Float,           // 风速m/s
        weatherText: String,        // 天气描述
        moonPhase: String? = null,  // 月相
        hour: Int = 12              // 当前小时（用于判断是否在活跃窗口内）
    ): SpeciesPrediction {
        val baseScore = calcBaseScore(species, hour)
        val waterTempScore = calcWaterTempScore(species, waterTemperature, temperature)
        val pressureScore = calcPressureScore(pressure, pressureTrend)
        val windScore = calcWindScore(windSpeed, species)
        val weatherScore = calcWeatherScore(weatherText, hour, species)
        val moonScore = calcMoonScore(moonPhase)

        val rawScore = baseScore + waterTempScore + pressureScore + windScore + weatherScore + moonScore
        val overallScore = rawScore.coerceIn(0, 100)

        val activityWindows = generateActivityWindows(species, overallScore, waterTemperature, temperature, pressure, pressureTrend, windSpeed, weatherText)
        val recommendedLures = selectRecommendedLures(species, overallScore)
        val technique = selectTechnique(species, overallScore)
        val insight = generateInsight(species, overallScore, activityWindows, weatherText, windSpeed)

        return SpeciesPrediction(
            species = species,
            overallScore = overallScore,
            activityWindows = activityWindows,
            recommendedLures = recommendedLures,
            recommendedTechnique = technique,
            insight = insight
        )
    }

    /**
     * 批量预测所有鱼种
     */
    fun predictAll(
        temperature: Float,
        waterTemperature: Float?,
        pressure: Float,
        pressureTrend: Float,
        windSpeed: Float,
        weatherText: String,
        moonPhase: String? = null,
        hour: Int = 12
    ): List<SpeciesPrediction> {
        return FishSpecies.values().map { species ->
            predict(species, temperature, waterTemperature, pressure, pressureTrend, windSpeed, weatherText, moonPhase, hour)
        }.sortedByDescending { it.overallScore }
    }

    // ========== 评分因子计算 ==========

    /**
     * 基础分：来自鱼种偏好时段匹配
     * - 当前时间在 peakHours 内：60分
     * - 当前时间在 peakHours ±1小时：45分
     * - 其他时段：30分
     */
    private fun calcBaseScore(species: FishSpecies, hour: Int): Int {
        val inPeak = species.peakHours.any { range -> hour in range }
        if (inPeak) return 60

        val nearPeak = species.peakHours.any { range ->
            hour == range.first - 1 || hour == range.last + 1
        }
        if (nearPeak) return 45

        return 30
    }

    /**
     * 水温因子（±20分）
     * - 水温在鱼种 preferredWaterTemp 范围内：+20分
     * - 偏离范围 1-3℃：+10分
     * - 偏离 3-6℃：+0分
     * - 偏离 >6℃：-10分
     */
    private fun calcWaterTempScore(species: FishSpecies, waterTemp: Float?, airTemp: Float): Int {
        val effectiveTemp = waterTemp ?: estimateWaterTemp(airTemp)
        val range = species.preferredWaterTemp
        if (effectiveTemp in range) return 20

        val deviation = if (effectiveTemp < range.start) {
            range.start - effectiveTemp
        } else {
            effectiveTemp - range.endInclusive
        }

        return when {
            deviation <= 3f -> 10
            deviation <= 6f -> 0
            else -> -10
        }
    }

    /**
     * 气压因子（±15分）
     * - 气压1010-1025 且趋势稳定/缓升：+15分
     * - 气压正常但趋势不明：+8分
     * - 气压骤降(>5hPa/3h)：-10分
     */
    private fun calcPressureScore(pressure: Float, pressureTrend: Float): Int {
        if (pressureTrend < -5f) return -10

        val normalRange = pressure in 1010f..1025f
        val stableOrRising = pressureTrend >= -1f

        return when {
            normalRange && stableOrRising -> 15
            normalRange -> 8
            stableOrRising -> 5
            else -> 0
        }
    }

    /**
     * 风速因子（±10分）
     * - 1-4m/s 微风：+10分
     * - 4-7m/s 中风：+5分（翘嘴额外+5）
     * - >7m/s 大风：-5分
     */
    private fun calcWindScore(windSpeed: Float, species: FishSpecies): Int {
        return when {
            windSpeed in 1f..4f -> 10
            windSpeed > 4f && windSpeed <= 7f -> {
                val base = 5
                if (species == FishSpecies.TOPMOUTH_CULTER) base + 5 else base
            }
            windSpeed > 7f -> -5
            else -> 5 // 无风或极微风
        }
    }

    /**
     * 天气/光照因子（±5分）
     * - 阴天/多云：+5分（减少警觉）
     * - 晴天清晨/黄昏：+5分
     * - 正午烈日：-5分（黑鱼例外，+3）
     */
    private fun calcWeatherScore(weatherText: String, hour: Int, species: FishSpecies): Int {
        val isOvercast = "阴" in weatherText || "多云" in weatherText
        val isClearDawn = ("晴" in weatherText) && (hour in 4..7 || hour in 17..20)
        val isNoonSun = ("晴" in weatherText) && (hour in 10..14)

        return when {
            isOvercast -> 5
            isClearDawn -> 5
            isNoonSun -> if (species == FishSpecies.SNAKEHEAD) 3 else -5
            else -> 0
        }
    }

    /**
     * 月相加成（0-5分）
     * - 新月/满月：+5分
     * - 上弦/下弦：+3分
     */
    private fun calcMoonScore(moonPhase: String?): Int {
        if (moonPhase == null) return 0
        return when {
            "新月" in moonPhase || "满月" in moonPhase || "朔" in moonPhase || "望" in moonPhase -> 5
            "上弦" in moonPhase || "下弦" in moonPhase -> 3
            else -> 0
        }
    }

    // ========== 活跃时段窗口生成 ==========

    private fun generateActivityWindows(
        species: FishSpecies,
        overallScore: Int,
        waterTemp: Float?,
        airTemp: Float,
        pressure: Float,
        pressureTrend: Float,
        windSpeed: Float,
        weatherText: String
    ): List<ActivityWindow> {
        val windows = mutableListOf<ActivityWindow>()

        for (peakRange in species.peakHours) {
            val windowScore = calcWindowScore(overallScore, peakRange, waterTemp, airTemp, species, pressure, pressureTrend, windSpeed, weatherText)
            val level = when {
                windowScore >= 70 -> "高活跃"
                windowScore >= 50 -> "中等"
                else -> "低活跃"
            }
            windows.add(
                ActivityWindow(
                    startHour = peakRange.first,
                    endHour = peakRange.last,
                    activityLevel = level,
                    score = windowScore.coerceIn(0, 100)
                )
            )
        }

        // 如果天气条件特别好，增加额外窗口（午间阴天）
        val isOvercast = "阴" in weatherText || "多云" in weatherText
        if (isOvercast && overallScore >= 60) {
            val midScore = (overallScore * 0.7f).toInt().coerceIn(0, 100)
            val level = when {
                midScore >= 70 -> "高活跃"
                midScore >= 50 -> "中等"
                else -> "低活跃"
            }
            windows.add(
                ActivityWindow(
                    startHour = 10,
                    endHour = 14,
                    activityLevel = level,
                    score = midScore
                )
            )
        }

        return windows.sortedBy { it.startHour }
    }

    private fun calcWindowScore(
        overallScore: Int,
        peakRange: IntRange,
        waterTemp: Float?,
        airTemp: Float,
        species: FishSpecies,
        pressure: Float,
        pressureTrend: Float,
        windSpeed: Float,
        weatherText: String
    ): Int {
        // 窗口分基于整体评分，对应时段给予额外加成
        val isMorning = peakRange.first < 10
        val isEvening = peakRange.first >= 16

        var windowScore = overallScore

        // 清晨窗口气压通常更稳定，微加分
        if (isMorning && pressureTrend >= 0) windowScore += 5
        // 黄昏窗口如果有微风，利于觅食
        if (isEvening && windSpeed in 1f..4f) windowScore += 3

        return windowScore.coerceIn(0, 100)
    }

    // ========== 推荐饵料 ==========

    private fun selectRecommendedLures(species: FishSpecies, overallScore: Int): List<String> {
        return if (overallScore >= 70) {
            // 高活跃：推荐反应饵、搜索型饵
            when (species) {
                FishSpecies.BASS -> listOf("米诺", "VIB", "复合亮片")
                FishSpecies.TOPMOUTH_CULTER -> listOf("亮片", "米诺", "铅笔")
                FishSpecies.MANDARIN_FISH -> listOf("铅头钩+卷尾蛆", "德州")
                FishSpecies.SNAKEHEAD -> listOf("雷蛙", "胡须佬")
                FishSpecies.MINNOW_FISH -> listOf("微物亮片", "小米诺")
                FishSpecies.YELLOWCHEEK -> listOf("大米诺", "铁板", "VIB")
                FishSpecies.SQUALIOBARBUS -> listOf("亮片", "米诺")
            }
        } else if (overallScore >= 50) {
            // 中等活跃：搜索+慢诱结合
            when (species) {
                FishSpecies.BASS -> listOf("德州", "倒吊", "米诺")
                FishSpecies.TOPMOUTH_CULTER -> listOf("VIB", "米诺慢抽停")
                FishSpecies.MANDARIN_FISH -> listOf("德州", "铅头钩+针尾虫")
                FishSpecies.SNAKEHEAD -> listOf("雷蛙慢收", "德州重钓组")
                FishSpecies.MINNOW_FISH -> listOf("小米诺", "小飞蝇")
                FishSpecies.YELLOWCHEEK -> listOf("米诺变速", "VIB")
                FishSpecies.SQUALIOBARBUS -> listOf("米诺抽停", "飞蝇")
            }
        } else {
            // 低活跃：finesse / 慢诱
            when (species) {
                FishSpecies.BASS -> listOf("倒吊", "Neko Rig", "小软虫")
                FishSpecies.TOPMOUTH_CULTER -> listOf("小亮片慢收", "沉水米诺")
                FishSpecies.MANDARIN_FISH -> listOf("铅头钩贴底慢拖", "软虫")
                FishSpecies.SNAKEHEAD -> listOf("德州重钓组慢拖", "软蛙")
                FishSpecies.MINNOW_FISH -> listOf("微物亮片慢收", "小飞蝇")
                FishSpecies.YELLOWCHEEK -> listOf("深潜米诺", "铁板慢摇")
                FishSpecies.SQUALIOBARBUS -> listOf("飞蝇", "小亮片")
            }
        }
    }

    // ========== 操控手法建议 ==========

    private fun selectTechnique(species: FishSpecies, overallScore: Int): String {
        val isShallowSpecies = species in listOf(
            FishSpecies.SNAKEHEAD, FishSpecies.MINNOW_FISH
        )
        val isDeepSpecies = species in listOf(
            FishSpecies.MANDARIN_FISH, FishSpecies.YELLOWCHEEK
        )

        return when {
            overallScore > 70 && isShallowSpecies -> "快抽、反应饵快速搜索"
            overallScore > 70 && isDeepSpecies -> "中速搜索、匀速收线"
            overallScore > 70 -> "快速搜索、连续抽竿"
            overallScore in 50..70 -> "抽停结合、变速收线"
            else -> "慢收、跳底、长停顿、finesse"
        }
    }

    // ========== 一句话建议 ==========

    private fun generateInsight(
        species: FishSpecies,
        overallScore: Int,
        windows: List<ActivityWindow>,
        weatherText: String,
        windSpeed: Float
    ): String {
        val bestWindow = windows.maxByOrNull { it.score }
        val timeDesc = bestWindow?.let {
            if (it.startHour < 10) "清晨" else "傍晚"
        } ?: "今日"

        val isOvercast = "阴" in weatherText || "多云" in weatherText
        val isWindy = windSpeed > 4f

        return when {
            overallScore >= 70 -> {
                when (species) {
                    FishSpecies.TOPMOUTH_CULTER -> {
                        if (isWindy) "${species.displayName}今日活跃度高，${timeDesc}迎风口用亮片快搜效果最佳"
                        else "${species.displayName}今日活跃度高，${timeDesc}明水用米诺快搜效果佳"
                    }
                    FishSpecies.SNAKEHEAD -> "${species.displayName}高温活跃，重草区雷蛙打标点"
                    FishSpecies.MINNOW_FISH -> "${species.displayName}活跃度高，溪流浅滩微物亮片连竿机会大"
                    else -> "${species.displayName}今日活跃度高，${timeDesc}积极搜索，推荐${species.preferredLures.first()}"
                }
            }
            overallScore >= 50 -> {
                when (species) {
                    FishSpecies.MANDARIN_FISH -> "${species.displayName}中等活性，建议铅头钩贴底慢搜，石堆区多做停顿"
                    FishSpecies.BASS -> "${species.displayName}中等活性，${timeDesc}深浅交界处抽停结合"
                    else -> "${species.displayName}中等活性，${timeDesc}耐心搜索，抽停结合有机会"
                }
            }
            else -> {
                when (species) {
                    FishSpecies.MANDARIN_FISH -> "${species.displayName}低活性期，建议铅头钩贴底慢拖，多做停顿"
                    FishSpecies.TOPMOUTH_CULTER -> "${species.displayName}活性低，沉水米诺慢抽停守深浅交界"
                    else -> "${species.displayName}低活性期，建议放慢节奏，精细作钓"
                }
            }
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 根据气温估算水温（简易公式）
     * 水温通常比气温低2-4℃，且变化更缓
     */
    private fun estimateWaterTemp(airTemp: Float): Float {
        return airTemp - 3f
    }
}

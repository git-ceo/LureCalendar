package com.lurecalendar.app.common.fishing

import kotlin.math.abs
import kotlin.math.roundToInt

data class FishingIndexResult(
    val score: Int,
    val summary: String,
    val factors: Map<String, Int> = emptyMap(),
    val seasonalMultiplier: Double = 1.0,
    val targetSpecies: String = "通用"
)

/**
 * 鱼种配置数据类
 */
data class SpeciesConfig(
    val name: String,
    val optimalTempRange: ClosedFloatingPointRange<Float>,
    val peakHours: List<Int>,
    val weights: Map<String, Int>,  // pressure, wind, light, moon, rain, temperature
    val pressureSensitivity: Float,
    val seasonalBoost: Map<Int, Float>  // month -> multiplier
)

val SPECIES_CONFIGS = mapOf(
    "翘嘴" to SpeciesConfig(
        name = "翘嘴",
        optimalTempRange = 16f..28f,
        peakHours = listOf(4, 5, 6, 7, 17, 18, 19, 20),
        weights = mapOf("pressure" to 25, "wind" to 30, "light" to 20, "moon" to 15, "rain" to 10, "temperature" to 10),
        pressureSensitivity = 1.2f,
        seasonalBoost = mapOf(3 to 0.9f, 4 to 1.1f, 5 to 1.15f, 6 to 1.0f, 9 to 1.2f, 10 to 1.15f, 11 to 0.9f)
    ),
    "鲈鱼" to SpeciesConfig(
        name = "鲈鱼",
        optimalTempRange = 18f..26f,
        peakHours = listOf(5, 6, 7, 8, 17, 18, 19, 20),
        weights = mapOf("pressure" to 30, "wind" to 20, "light" to 20, "moon" to 15, "rain" to 10, "temperature" to 15),
        pressureSensitivity = 1.3f,
        seasonalBoost = mapOf(4 to 1.1f, 5 to 1.2f, 6 to 1.0f, 9 to 1.15f, 10 to 1.1f)
    ),
    "鳜鱼" to SpeciesConfig(
        name = "鳜鱼",
        optimalTempRange = 18f..25f,
        peakHours = listOf(5, 6, 7, 8, 18, 19, 20, 21),
        weights = mapOf("pressure" to 35, "wind" to 15, "light" to 20, "moon" to 15, "rain" to 5, "temperature" to 15),
        pressureSensitivity = 1.5f,
        seasonalBoost = mapOf(4 to 1.0f, 5 to 1.15f, 6 to 1.1f, 9 to 1.2f, 10 to 1.15f)
    ),
    "黑鱼" to SpeciesConfig(
        name = "黑鱼",
        optimalTempRange = 20f..30f,
        peakHours = listOf(6, 7, 8, 9, 16, 17, 18, 19),
        weights = mapOf("pressure" to 20, "wind" to 20, "light" to 25, "moon" to 10, "rain" to 10, "temperature" to 20),
        pressureSensitivity = 0.8f,
        seasonalBoost = mapOf(5 to 1.1f, 6 to 1.2f, 7 to 1.15f, 8 to 1.1f, 9 to 1.0f)
    ),
    "马口" to SpeciesConfig(
        name = "马口",
        optimalTempRange = 14f..22f,
        peakHours = listOf(6, 7, 8, 9, 16, 17, 18),
        weights = mapOf("pressure" to 25, "wind" to 25, "light" to 20, "moon" to 10, "rain" to 15, "temperature" to 15),
        pressureSensitivity = 1.0f,
        seasonalBoost = mapOf(3 to 1.0f, 4 to 1.15f, 5 to 1.2f, 9 to 1.1f, 10 to 1.05f)
    ),
    "鳡鱼" to SpeciesConfig(
        name = "鳡鱼",
        optimalTempRange = 18f..28f,
        peakHours = listOf(5, 6, 7, 8, 16, 17, 18, 19),
        weights = mapOf("pressure" to 25, "wind" to 30, "light" to 20, "moon" to 15, "rain" to 10, "temperature" to 10),
        pressureSensitivity = 1.1f,
        seasonalBoost = mapOf(4 to 1.0f, 5 to 1.15f, 6 to 1.1f, 9 to 1.2f, 10 to 1.1f)
    ),
    "军鱼" to SpeciesConfig(
        name = "军鱼",
        optimalTempRange = 16f..24f,
        peakHours = listOf(5, 6, 7, 8, 17, 18, 19, 20),
        weights = mapOf("pressure" to 30, "wind" to 25, "light" to 20, "moon" to 10, "rain" to 10, "temperature" to 15),
        pressureSensitivity = 1.2f,
        seasonalBoost = mapOf(4 to 1.1f, 5 to 1.2f, 9 to 1.15f, 10 to 1.1f)
    )
)

val DEFAULT_CONFIG = SpeciesConfig(
    name = "通用",
    optimalTempRange = 15f..28f,
    peakHours = listOf(5, 6, 7, 17, 18, 19),
    weights = mapOf("pressure" to 30, "wind" to 25, "light" to 20, "moon" to 15, "rain" to 10, "temperature" to 10),
    pressureSensitivity = 1.0f,
    seasonalBoost = emptyMap()
)

/**
 * 路亚活跃度评分计算器
 *
 * 6因子评分体系（基础满分由鱼种配置权重决定 + 趋势±10，归一化到0-100后乘以季节系数）：
 * - 气压: 按鱼种权重
 * - 风速: 按鱼种权重
 * - 光照(鱼种活跃时段): 按鱼种权重
 * - 月相: 按鱼种权重
 * - 降水: 按鱼种权重
 * - 温度: 按鱼种权重（使用鱼种最适温区）
 * - 气压趋势: ±10分（乘以鱼种气压敏感度）
 * - 季节乘数: 鱼种季节加成优先，否则使用默认
 */
object FishingIndexCalculator {

    /**
     * 计算路亚活跃度评分
     *
     * @param pressureHpa 当前气压(hPa)
     * @param windSpeedMs 风速(m/s)
     * @param airTempC 气温(℃)，用于温度评分
     * @param waterTempC 水温(保留兼容)
     * @param precipitationMm 降水量(mm)
     * @param precipitationProbability 降水概率(保留兼容)
     * @param moonPhase 月相名称(如 "新月"、"满月"、"上弦月" 等)
     * @param hour 当前小时(0-23)，用于光照时段判断
     * @param weatherText 天气描述文本(如 "阴"、"多云" 等)
     * @param pressureHistory 过去几小时气压列表(从旧到新)，用于趋势判断
     * @param month 月份(1-12)，用于季节乘数和动态日出日落计算
     * @param targetSpecies 目标鱼种名称，影响各因子权重和评分标准
     */
    fun calculate(
        pressureHpa: Float?,
        windSpeedMs: Float?,
        airTempC: Float? = null,
        waterTempC: Float? = null,
        precipitationMm: Float?,
        precipitationProbability: Int? = null,
        moonPhase: String? = null,
        hour: Int? = null,
        weatherText: String? = null,
        pressureHistory: List<Float>? = null,
        month: Int? = null,
        targetSpecies: String = "default"
    ): FishingIndexResult {
        val notes = ArrayList<String>(4)
        val currentMonth = month ?: java.time.LocalDate.now().monthValue

        // 获取鱼种配置
        val config = SPECIES_CONFIGS[targetSpecies] ?: DEFAULT_CONFIG

        // === 1. 气压评分（原始满分30） ===
        val pressureRaw = calcPressureScore(pressureHpa, notes)
        val pressureScaled = pressureRaw * config.weights["pressure"]!! / 30f

        // === 2. 风速评分（原始满分25） ===
        val windRaw = calcWindScore(windSpeedMs, notes)
        val windScaled = windRaw * config.weights["wind"]!! / 25f

        // === 3. 光照/时间段评分（原始满分20，使用鱼种活跃时段） ===
        val lightRaw = if (config == DEFAULT_CONFIG) {
            calcDynamicLightScore(hour, currentMonth, weatherText, notes)
        } else {
            calcLightScoreForSpecies(hour, config, weatherText, notes)
        }
        val lightScaled = lightRaw * config.weights["light"]!! / 20f

        // === 4. 月相评分（原始满分15） ===
        val moonRaw = calcMoonScore(moonPhase, notes)
        val moonScaled = moonRaw * config.weights["moon"]!! / 15f

        // === 5. 降水评分（原始满分10） ===
        val rainRaw = calcRainScore(precipitationMm, notes)
        val rainScaled = rainRaw * config.weights["rain"]!! / 10f

        // === 6. 温度评分（原始满分10，使用鱼种最适温区） ===
        val tempRaw = calcTemperatureScoreForSpecies(airTempC, config, notes)
        val tempScaled = tempRaw * config.weights["temperature"]!! / 10f

        // === 气压趋势加成（乘以鱼种气压敏感度） ===
        val trendRaw = calcPressureTrendBonus(pressureHistory, pressureHpa, notes)
        val trendScore = trendRaw * config.pressureSensitivity

        // 总权重 = 各因子权重之和
        val totalWeight = config.weights.values.sum()  // 应该=110
        val rawScore = pressureScaled + windScaled + lightScaled + moonScaled + rainScaled + tempScaled + trendScore

        // 归一化到0-100
        val normalizedScore = (rawScore * 100.0 / (totalWeight + 10f)).coerceIn(0.0, 100.0)

        // 应用季节乘数（鱼种季节加成优先）
        val seasonalMultiplier = if (config.seasonalBoost.containsKey(currentMonth)) {
            config.seasonalBoost[currentMonth]!!.toDouble()
        } else {
            getSeasonalMultiplier(currentMonth)
        }
        val finalScore = (normalizedScore * seasonalMultiplier).coerceIn(0.0, 100.0).roundToInt()

        // 标签映射
        val label = when {
            finalScore >= 85 -> "爆护期"
            finalScore >= 70 -> "活跃"
            finalScore >= 55 -> "一般"
            finalScore >= 40 -> "低迷"
            else -> "不建议出钓"
        }

        return FishingIndexResult(
            score = finalScore,
            summary = buildString {
                append("$label（$finalScore/100）")
                if (notes.isNotEmpty()) {
                    append(" · ")
                    append(notes.distinct().take(3).joinToString("、"))
                }
            },
            factors = mapOf(
                "气压" to pressureScaled.roundToInt(),
                "风速" to windScaled.roundToInt(),
                "光照" to lightScaled.roundToInt(),
                "月相" to moonScaled.roundToInt(),
                "降水" to rainScaled.roundToInt(),
                "温度" to tempScaled.roundToInt(),
                "趋势" to trendScore.roundToInt()
            ),
            seasonalMultiplier = seasonalMultiplier,
            targetSpecies = config.name
        )
    }

    // ============================================================
    // 各因子评分方法
    // ============================================================

    /**
     * 气压评分（满分30分）
     * 最优：稳定在1010-1025hPa
     */
    private fun calcPressureScore(pressureHpa: Float?, notes: MutableList<String>): Float {
        val p = pressureHpa ?: return 15f // 无数据给中间分
        return when {
            p in 1010f..1025f -> {
                notes.add("气压适中")
                30f
            }
            p in 1000f..1010f || p in 1025f..1030f -> {
                notes.add("气压尚可")
                22f
            }
            p in 995f..1000f || p in 1030f..1035f -> {
                notes.add("气压偏离")
                14f
            }
            else -> {
                notes.add("气压异常")
                6f
            }
        }
    }

    /**
     * 风速评分（满分25分）
     * 1-3级(0.3-5.4m/s)满分；4级(5.4-7.9m/s)减半；5级以上0分
     */
    private fun calcWindScore(windSpeedMs: Float?, notes: MutableList<String>): Float {
        val w = windSpeedMs ?: return 12f // 无数据给中间分
        return when {
            w in 0.3f..5.4f -> {
                notes.add("微风好口")
                25f
            }
            w in 5.4f..7.9f -> {
                notes.add("中风减半")
                12.5f
            }
            w < 0.3f -> {
                // 几乎无风，给部分分
                notes.add("无风偏闷")
                15f
            }
            else -> {
                // 5级以上(>7.9m/s)
                notes.add("大风不宜")
                0f
            }
        }
    }

    /**
     * 鱼种光照评分（满分20分）
     * 根据鱼种活跃时段评分，越接近活跃时段得分越高
     */
    private fun calcLightScoreForSpecies(
        hour: Int?,
        config: SpeciesConfig,
        weatherText: String?,
        notes: MutableList<String>
    ): Float {
        val isOvercast = weatherText?.contains("阴") == true || weatherText?.contains("多云") == true

        if (hour == null) {
            return if (isOvercast) 16f else 10f
        }

        if (hour in config.peakHours) {
            notes.add("活跃时段")
            val base = 20f
            return base
        }

        val minDistance = config.peakHours.minOf { peakH ->
            minOf(
                abs(hour - peakH),
                abs(hour + 24 - peakH),
                abs(hour - 24 - peakH)
            )
        }

        val baseScore = when {
            minDistance == 1 -> 15f
            minDistance == 2 -> 10f
            minDistance <= 4 -> 6f
            else -> 3f
        }

        // 阴天修正：非活跃时段如果阴天，提升得分
        if (minDistance > 2 && isOvercast) {
            notes.add("阴天遮阳")
            return maxOf(baseScore, 14f)
        }

        if (baseScore <= 6f && !isOvercast) {
            notes.add("非活跃时段")
        }

        return baseScore
    }

    /**
     * 动态光照/时间段评分（满分20分）- 通用默认配置使用
     * 使用简化的日出日落估算（基于月份）
     * 黄金时段 = 日出前30分~日出后90分 + 日落前90分~日落后30分
     */
    private fun calcDynamicLightScore(hour: Int?, month: Int, weatherText: String?, notes: MutableList<String>): Float {
        val isOvercast = weatherText?.contains("阴") == true || weatherText?.contains("多云") == true
        val isCloudy = weatherText?.contains("云") == true

        if (hour == null) {
            // 无时间数据，按天气文本给分
            return when {
                isOvercast -> 16f
                isCloudy -> 14f
                else -> 10f
            }
        }

        // 简化的日出日落估算（中国中纬度地区，约31°N 绵阳）
        val (sunriseHour, sunsetHour) = estimateSunriseSunset(month)

        // 黄金清晨窗口：日出前0.5h ~ 日出后1.5h
        val morningStart = sunriseHour - 0.5
        val morningEnd = sunriseHour + 1.5

        // 黄金黄昏窗口：日落前1.5h ~ 日落后0.5h
        val eveningStart = sunsetHour - 1.5
        val eveningEnd = sunsetHour + 0.5

        // 次优时段：清晨后延2h + 黄昏前延2h
        val subMorningEnd = sunriseHour + 3.0
        val subEveningStart = sunsetHour - 3.0

        val currentHour = hour.toDouble()

        val baseScore = when {
            currentHour in morningStart..morningEnd -> {
                notes.add("黄金清晨")
                20f
            }
            currentHour in eveningStart..eveningEnd -> {
                notes.add("黄金黄昏")
                20f
            }
            currentHour in morningEnd..subMorningEnd -> 15f   // 次优清晨
            currentHour in subEveningStart..eveningStart -> 15f // 次优黄昏
            currentHour in 20.0..21.0 || currentHour in 4.0..morningStart -> 12f // 夜钓
            currentHour in 22.0..24.0 || currentHour in 0.0..3.0 -> 8f // 深夜
            else -> {
                // 正午时段
                notes.add("正午烈日")
                6f
            }
        }

        // 阴天修正：正午时段如果阴天/多云，提升到16分
        val isMidday = currentHour > subMorningEnd && currentHour < subEveningStart
        if (isMidday && (isOvercast || isCloudy)) {
            notes.remove("正午烈日")
            notes.add("阴天遮阳")
            return maxOf(baseScore, 16f)
        }

        return baseScore
    }

    /**
     * 估算日出日落时间（简化版，基于月份）
     * 适用于中国中纬度地区(30-32°N，如绵阳31.5°N)
     */
    private fun estimateSunriseSunset(month: Int): Pair<Double, Double> {
        return when (month) {
            1 -> 7.5 to 18.0    // 冬至后，日出晚日落早
            2 -> 7.2 to 18.3
            3 -> 6.8 to 18.7    // 春分前后
            4 -> 6.3 to 19.0
            5 -> 6.0 to 19.4
            6 -> 5.8 to 19.7    // 夏至前后，日出最早日落最晚
            7 -> 6.0 to 19.6
            8 -> 6.2 to 19.3
            9 -> 6.5 to 18.8    // 秋分前后
            10 -> 6.7 to 18.3
            11 -> 7.0 to 17.9
            12 -> 7.4 to 17.8   // 冬至
            else -> 6.5 to 18.5
        }
    }

    /**
     * 月相评分（满分15分）
     * 新月/满月前后满分(+15)；上弦月/下弦月+7；其余+3
     */
    private fun calcMoonScore(moonPhase: String?, notes: MutableList<String>): Float {
        if (moonPhase == null) return 7f // 无数据给中间分
        return when {
            moonPhase.contains("新月") || moonPhase.contains("满月") ||
            moonPhase.contains("朔") || moonPhase.contains("望") -> {
                notes.add("月相极佳")
                15f
            }
            moonPhase.contains("上弦") || moonPhase.contains("下弦") ||
            moonPhase.contains("半月") -> 7f
            else -> 3f
        }
    }

    /**
     * 降水评分（满分10分）
     * 无雨或毛毛雨(<1mm)满分；小雨7分；中雨3分；大雨以上0分
     */
    private fun calcRainScore(precipitationMm: Float?, notes: MutableList<String>): Float {
        val pr = precipitationMm ?: return 10f // 无降水数据视为无雨
        return when {
            pr < 1f -> 10f           // 无雨或毛毛雨
            pr < 5f -> {
                notes.add("小雨影响")
                7f                   // 小雨
            }
            pr < 15f -> {
                notes.add("中雨影响")
                3f                   // 中雨
            }
            else -> {
                notes.add("大雨不宜")
                0f                   // 大雨以上
            }
        }
    }

    /**
     * 鱼种温度评分（满分10分）
     * 根据鱼种最适温区评分
     */
    private fun calcTemperatureScoreForSpecies(airTempC: Float?, config: SpeciesConfig, notes: MutableList<String>): Float {
        val t = airTempC ?: return 5f
        val range = config.optimalTempRange
        return when {
            t in range -> {
                notes.add("水温适宜")
                10f
            }
            t in (range.start - 3f)..range.start || t in range.endInclusive..(range.endInclusive + 3f) -> 7f
            t in (range.start - 6f)..(range.start - 3f) || t in (range.endInclusive + 3f)..(range.endInclusive + 6f) -> {
                notes.add("温度偏离")
                4f
            }
            else -> {
                notes.add("温度极端")
                1f
            }
        }
    }

    /**
     * 季节系数（作用于总分的乘数）- 默认通用
     * 基于月份判断鱼类整体活跃度
     */
    private fun getSeasonalMultiplier(month: Int): Double {
        return when (month) {
            1 -> 0.5
            2 -> 0.6
            3 -> 0.8
            4 -> 1.0
            5 -> 1.05
            6 -> 0.95
            7 -> 0.85
            8 -> 0.9
            9 -> 1.1
            10 -> 1.15
            11 -> 0.9
            12 -> 0.6
            else -> 1.0
        }
    }

    /**
     * 气压趋势加成
     */
    private fun calcPressureTrendBonus(
        pressureHistory: List<Float>?,
        currentPressure: Float?,
        notes: MutableList<String>
    ): Float {
        if (pressureHistory == null || pressureHistory.size < 2 || currentPressure == null) return 0f

        val oldest = pressureHistory.first()
        val newest = pressureHistory.last()
        val delta = newest - oldest

        return when {
            delta < -5f -> {
                notes.add("气压骤降")
                -10f
            }
            delta in -5f..-2f && pressureHistory.average() > 1020.0 -> {
                notes.add("窗口期")
                5f
            }
            delta in 2f..5f -> {
                notes.add("气压缓升")
                3f
            }
            abs(delta) < 2f -> 0f
            else -> 0f
        }
    }

    /**
     * 估算水温（保留兼容）
     */
    fun estimateWaterTempC(airTempC: Float?, waterType: String = "河流"): Float? {
        val t = airTempC ?: return null
        val estimated = when (waterType) {
            "海水" -> t * 0.8f + 4.0f
            "湖泊" -> t - 1.0f
            else -> t - 2.0f
        }
        return estimated.coerceIn(0f, 35f)
    }

    /**
     * 趋势对比（保留兼容）
     */
    fun compareTrend(a: Int?, b: Int?): String? {
        if (a == null || b == null) return null
        val delta = b - a
        return when {
            abs(delta) <= 3 -> "趋稳"
            delta > 0 -> "走强"
            else -> "走弱"
        }
    }
}

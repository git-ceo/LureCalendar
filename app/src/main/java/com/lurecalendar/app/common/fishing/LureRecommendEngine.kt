package com.lurecalendar.app.common.fishing

/**
 * 路亚饵料智能推荐引擎
 * 基于当前及未来3小时天气变化，推荐拟饵类型和操控手法
 */
class LureRecommendEngine {

    data class LureRecommendation(
        val lureType: String,           // 饵料类型名称
        val description: String,        // 简短描述
        val technique: String,          // 推荐操控手法
        val techniqueDetail: String,    // 手法详细说明
        val confidence: Int             // 推荐置信度 0-100
    )

    data class WeatherCondition(
        val pressure: Float,            // 当前气压 hPa
        val pressureTrend: Float,       // 3小时气压变化量（正=上升，负=下降）
        val windSpeed: Float,           // 风速 m/s
        val temperature: Float,         // 气温 ℃
        val waterTemperature: Float?,   // 水温 ℃（可选）
        val hour: Int,                  // 当前小时 0-23
        val weatherText: String,        // 天气描述（晴/多云/阴/雨等）
        val precipitation: Float        // 降水量 mm
    )

    /**
     * 获取推荐的饵料列表（按推荐度排序，最多5个）
     */
    fun recommend(condition: WeatherCondition, targetSpecies: String? = null): List<LureRecommendation> {
        val recommendations = mutableListOf<LureRecommendation>()

        // === 鱼种专属推荐（优先级最高） ===
        when (targetSpecies) {
            "鳜鱼" -> recommendations.add(
                LureRecommendation(
                    lureType = "德州钓组/铅头钩+软虫",
                    description = "鳜鱼贴底伏击，软虫模拟底层猎物",
                    technique = "跳底、贴底慢拖",
                    techniqueDetail = "抛投到标点后沉底，竿尖轻挑让软虫跳离底部10-20cm后自由落下，模拟虾蟹逃窜动作",
                    confidence = 90
                )
            )
            "翘嘴" -> recommendations.add(
                LureRecommendation(
                    lureType = "米诺/亮片/VIB",
                    description = "翘嘴追食中上层，反应饵高效搜索",
                    technique = "快抽、匀收、抽停",
                    techniqueDetail = "快速收线配合竿尖抽动，制造逃窜小鱼的泳姿，抽2-3下后短暂停顿诱发攻击",
                    confidence = 90
                )
            )
            "黑鱼" -> recommendations.add(
                LureRecommendation(
                    lureType = "雷蛙/德州重钓组",
                    description = "黑鱼喜重草区水面捕食",
                    technique = "打草洞、慢拖出草",
                    techniqueDetail = "精准抛入草洞，落水后静止3-5秒，轻抽让雷蛙在草洞中跳动，诱黑鱼炸水攻击",
                    confidence = 90
                )
            )
            "马口" -> recommendations.add(
                LureRecommendation(
                    lureType = "微物亮片/小型米诺",
                    description = "马口体型小，需用微物装备精细作钓",
                    technique = "轻抽、匀速慢收",
                    techniqueDetail = "使用1-3g微物亮片顺流抛投，匀速慢收配合偶尔轻抽，模拟小型水生昆虫",
                    confidence = 90
                )
            )
        }

        // === 天气条件推荐 ===

        val isMorning = condition.hour in 5..7
        val isEvening = condition.hour in 17..19
        val isNoon = condition.hour in 11..14
        val isLightWind = condition.windSpeed < 3f
        val isStrongWind = condition.windSpeed > 4f
        val isPressureStable = kotlin.math.abs(condition.pressureTrend) < 2f
        val isPressureDrop = condition.pressureTrend < -3f
        val isPressureDropStabilized = condition.pressureTrend in -5f..-3f
        val isOvercast = condition.weatherText.contains("阴") || condition.weatherText.contains("多云")
        val isSunny = condition.weatherText.contains("晴")
        val isRainAfter = condition.weatherText.contains("雨") || condition.precipitation in 1f..5f
        val isColdWater = (condition.waterTemperature ?: condition.temperature) < 15f

        // 清晨 + 微风 + 气压稳定 → 水面系
        if (isMorning && isLightWind && isPressureStable) {
            recommendations.add(
                LureRecommendation(
                    lureType = "水面系（波爬、铅笔）",
                    description = "清晨低光微风，鱼群上浮觅食的黄金时段",
                    technique = "快抽停顿、狗走",
                    techniqueDetail = "波爬快速抽动制造水花和声响吸引鱼注意，铅笔左右狗走模拟受伤小鱼在水面挣扎",
                    confidence = 88
                )
            )
        }

        // 清晨 + 阴天/低光 → 潜水米诺
        if (isMorning && isOvercast) {
            recommendations.add(
                LureRecommendation(
                    lureType = "潜水米诺",
                    description = "阴天清晨光线暗，米诺的反光和泳姿更具吸引力",
                    technique = "快抽、抽停",
                    techniqueDetail = "匀速收线让米诺达到目标水层，快抽2-3下后停顿1-2秒，模拟小鱼逃窜后喘息",
                    confidence = 85
                )
            )
        }

        // 黄昏 + 气压稳/缓降 → 水面系、VIB
        if (isEvening && (isPressureStable || condition.pressureTrend in -3f..0f)) {
            recommendations.add(
                LureRecommendation(
                    lureType = "水面系/VIB",
                    description = "黄昏鱼群活跃上浮，水面系和VIB均可高效搜索",
                    technique = "快抽、匀速收线",
                    techniqueDetail = "水面系制造炸水声诱鱼攻击，VIB匀速收线搜索中下层，覆盖不同水层",
                    confidence = 85
                )
            )
        }

        // 中午 + 晴天高温 → 德州钓组
        if (isNoon && isSunny && condition.temperature > 28f) {
            recommendations.add(
                LureRecommendation(
                    lureType = "德州钓组（软虫）",
                    description = "正午高温鱼躲深水阴凉处，需贴底作钓",
                    technique = "跳底、慢拖",
                    techniqueDetail = "让钓组完全沉底后，竿尖轻挑使软虫跳起再自然下落，每次移动20-30cm慢慢搜索",
                    confidence = 82
                )
            )
        }

        // 气压骤降后稳定 → VIB、复合亮片
        if (isPressureDropStabilized) {
            recommendations.add(
                LureRecommendation(
                    lureType = "VIB/复合亮片",
                    description = "气压骤降后鱼群烦躁，反应饵快速搜索可激发攻击本能",
                    technique = "快速搜索、反应饵",
                    techniqueDetail = "使用中大克重VIB或复合亮片，快速匀收搜索大面积水域，利用强烈振动刺激鱼的侧线",
                    confidence = 80
                )
            )
        }

        // 低温 + 深水 → 铅头钩+卷尾蛆
        if (isColdWater) {
            recommendations.add(
                LureRecommendation(
                    lureType = "铅头钩+卷尾蛆",
                    description = "低温期鱼活性低，需慢速贴底精细作钓",
                    technique = "慢收、跳底、停顿",
                    techniqueDetail = "使用较重铅头钩确保贴底，极慢速收线配合长时间停顿(3-5秒)，给鱼充足攻击时间",
                    confidence = 78
                )
            )
        }

        // 风速>4m/s → 亮片、VIB
        if (isStrongWind) {
            recommendations.add(
                LureRecommendation(
                    lureType = "亮片/VIB",
                    description = "大风天需要重克数饵保证抛投距离和稳定性",
                    technique = "匀速收线、逆风抛投",
                    techniqueDetail = "选择15g以上亮片或VIB，逆风低抛角抛投，匀速收线利用风浪掩护接近鱼群",
                    confidence = 76
                )
            )
        }

        // 雨后/微浊水 → 复合亮片、摇摆胖子
        if (isRainAfter) {
            recommendations.add(
                LureRecommendation(
                    lureType = "复合亮片/摇摆胖子",
                    description = "雨后水质微浊，需要强振动和闪光吸引鱼注意",
                    technique = "慢滚、震动搜索",
                    techniqueDetail = "复合亮片的柳叶片旋转产生强烈闪光和振动，摇摆胖子宽幅摆动推水，浊水中效果显著",
                    confidence = 75
                )
            )
        }

        // 如果没有任何推荐命中，添加通用推荐
        if (recommendations.isEmpty()) {
            recommendations.add(
                LureRecommendation(
                    lureType = "米诺",
                    description = "万能搜索饵，适用于大多数水域和鱼情",
                    technique = "匀收配合抽停",
                    techniqueDetail = "匀速收线让米诺保持稳定泳层，每收3-4圈后抽停一次模拟小鱼变向逃窜",
                    confidence = 60
                )
            )
        }

        // 按置信度降序排列，最多返回5个
        return recommendations
            .distinctBy { it.lureType }
            .sortedByDescending { it.confidence }
            .take(5)
    }
}

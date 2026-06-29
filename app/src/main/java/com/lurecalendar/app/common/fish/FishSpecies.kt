package com.lurecalendar.app.common.fish

val BuiltInFishSpecies: List<String> = listOf(
    "翘嘴", "鳜鱼", "黑鱼", "鲶鱼", "鲈鱼", "白条", "鲫鱼", "鲤鱼", "草鱼", "青鱼",
    "鳊鱼", "鳙鱼", "鲢鱼", "黄颡鱼", "罗非鱼", "泥鳅", "鳗鱼", "马口", "鳟鱼", "鳕鱼",
    "黄鳝", "鲦鱼", "鲮鱼", "鳊鲷", "石斑鱼", "鲷鱼", "金枪鱼", "竹荚鱼", "带鱼", "鲱鱼",
    "海鲈", "红鲷", "真鲷", "鲹鱼", "沙丁鱼", "鳀鱼", "鳗鲡", "鳓鱼", "鲳鱼", "鲻鱼"
)

/**
 * 路亚目标鱼种枚举
 * 包含7种常见路亚对象鱼的习性参数
 */
enum class FishSpecies(
    val displayName: String,
    val englishName: String,
    val preferredWaterTemp: ClosedFloatingPointRange<Float>,  // 适宜水温范围℃
    val peakHours: List<IntRange>,  // 活跃高峰时段
    val preferredStructure: List<String>,  // 偏好结构区
    val preferredLures: List<String>  // 常用饵料
) {
    BASS("鲈鱼", "bass", 18f..26f, listOf(5..8, 17..20), listOf("岩壁", "深浅交界", "桥墩"), listOf("米诺", "德州", "VIB")),
    TOPMOUTH_CULTER("翘嘴", "topmouth_culter", 16f..28f, listOf(4..7, 17..20), listOf("明水", "入水口", "深浅交界"), listOf("米诺", "亮片", "VIB", "铅笔")),
    MANDARIN_FISH("鳜鱼", "mandarin_fish", 18f..25f, listOf(5..8, 18..21), listOf("岩壁", "石堆", "深水"), listOf("德州", "铅头钩", "软虫")),
    SNAKEHEAD("黑鱼", "snakehead", 20f..30f, listOf(6..9, 16..19), listOf("草区", "浅水", "倒树"), listOf("雷蛙", "德州重钓组", "胡须佬")),
    MINNOW_FISH("马口", "minnow_fish", 14f..22f, listOf(6..9, 16..18), listOf("溪流浅滩", "入水口", "急流缓"), listOf("微物亮片", "小米诺", "小飞蝇")),
    YELLOWCHEEK("鳡鱼", "yellowcheek", 18f..28f, listOf(5..8, 16..19), listOf("明水", "深水急流", "入水口"), listOf("大米诺", "铁板", "VIB")),
    SQUALIOBARBUS("军鱼", "squaliobarbus", 16f..24f, listOf(5..8, 17..20), listOf("急流缓", "深潭", "岩壁"), listOf("亮片", "米诺", "飞蝇"))
}

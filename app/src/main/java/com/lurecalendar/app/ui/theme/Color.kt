package com.lurecalendar.app.ui.theme

import androidx.compose.ui.graphics.Color

val DeepSeaBlue = Color(0xFF1E3A8A)
val NatureGreen = Color(0xFF10B981)
val WarningRed = Color(0xFFEF4444)
val SurfaceLight = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF0F172A)
val TextPrimaryLight = Color(0xFF1E293B)
val TextSecondaryLight = Color(0xFF64748B)
val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)

// --- 视觉升级新增 ---
val DeepGreen = Color(0xFF1B4332)
val WaterCyan = Color(0xFF2A9D8F)
val SandYellow = Color(0xFFE9C46A)
val DarkSurface = Color(0xFF1E1E2E)
val GlassWhite = Color.White.copy(alpha = 0.12f)
val GlassWhiteDeep = Color.White.copy(alpha = 0.20f)

// --- 钓鱼指数动态颜色 ---
val IndexPerfect = Color(0xFF22C55E)  // 绿色 (>=85)
val IndexHigh = Color(0xFF86EFAC)     // 浅绿 (70-84)
val IndexMid = Color(0xFFEAB308)      // 黄色 (55-69)
val IndexLow = Color(0xFFF97316)      // 橙色 (40-54)
val IndexDanger = Color(0xFFEF4444)   // 红色 (<40)
val IndexRadar = Color(0xFFFFFFFF).copy(alpha = 0.4f)

// --- 深色模式层级色 ---
val SurfaceDarkElevated = Color(0xFF162032)   // surface: 比background稍亮
val SurfaceVariantDark = Color(0xFF1E293B)    // surfaceVariant: 卡片背景
val OutlineVariantDark = Color(0xFF334155)    // outlineVariant: 分割线
val OutlineDark = Color(0xFF475569)           // outline: 边框
val PrimaryContainerDark = Color(0xFF1A3A4A)  // primaryContainer: 选中态背景

// --- 毛玻璃/半透明背景 ---
val GlassBackgroundLight = Color(0xFFFFFFFF).copy(alpha = 0.72f)
val GlassBackgroundDark = Color(0xFF1E293B).copy(alpha = 0.72f)

// --- 图表颜色 ---
val ChartHistoryLine = Color(0xFF2A9D8F)   // 历史数据实线
val ChartForecastLine = Color(0xFF2A9D8F).copy(alpha = 0.45f) // 预测虚线
val ChartCurrentMark = Color(0xFFEAB308)   // 当前时刻标记
val ChartHistoryBg = Color(0xFF2A9D8F).copy(alpha = 0.08f)
val ChartForecastBg = Color(0xFF94A3B8).copy(alpha = 0.05f)

// --- 月相颜色 ---
val MoonLight = Color(0xFFFFF9C4)   // 月球亮面
val MoonDark = Color(0xFF1A1A2E)    // 月球暗面
val MoonGlow = Color(0xFFFFF176)    // 月球光晕

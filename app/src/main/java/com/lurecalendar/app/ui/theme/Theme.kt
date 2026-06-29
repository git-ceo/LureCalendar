package com.lurecalendar.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Path
import kotlin.math.sin

// CompositionLocal：在任何 Composable 中获取当前是否深色模式
val LocalIsDarkTheme = compositionLocalOf { false }

// 优化后的毛玻璃效果：移除模糊（模糊会导致文字看不清），改用高质感透明层+边框
fun Modifier.glassBackground(cornerRadius: Int = 16): Modifier = this
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(Color.White.copy(alpha = 0.08f))
    .border(
        width = 0.5.dp,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

// 动态波浪背景
@Composable
fun WaveBackground(progress: Float, color: Color = WaterCyan) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val waveLength = 2 * Math.PI.toFloat() / size.width
        val amplitude = 30f
        val path = Path().apply {
            moveTo(0f, size.height * 0.85f) // 调低波浪位置
            for (x in 0..size.width.toInt() step 10) {
                val y = size.height * 0.85f + amplitude * sin(x * waveLength + progress * 2 * Math.PI.toFloat())
                lineTo(x.toFloat(), y.toFloat())
            }
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color = color.copy(alpha = 0.15f)) // 调淡波浪颜色
    }
}

// --- 主题感知色彩工具 ---
object LureColors {
    val glassBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (LocalIsDarkTheme.current) GlassBackgroundDark else GlassBackgroundLight

    val indexPerfect: Color get() = IndexPerfect
    val indexHigh: Color get() = IndexHigh
    val indexMid: Color get() = IndexMid
    val indexLow: Color get() = IndexLow
    val indexDanger: Color get() = IndexDanger

    fun scoreColor(score: Int): Color = when {
        score >= 85 -> IndexPerfect
        score >= 70 -> IndexHigh
        score >= 55 -> IndexMid
        score >= 40 -> IndexLow
        else -> IndexDanger
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = WaterCyan,
    secondary = NatureGreen,
    tertiary = WaterCyan,
    background = SurfaceDark,
    surface = SurfaceDarkElevated,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = WaterCyan,
    error = WarningRed
)

private val LightColorScheme = lightColorScheme(
    primary = DeepSeaBlue,
    secondary = NatureGreen,
    tertiary = DeepSeaBlue,
    background = SurfaceLight,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = WarningRed
)

@Composable
fun LureCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

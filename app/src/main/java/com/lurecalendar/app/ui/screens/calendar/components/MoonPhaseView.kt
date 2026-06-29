package com.lurecalendar.app.ui.screens.calendar.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurecalendar.app.ui.theme.MoonDark
import com.lurecalendar.app.ui.theme.MoonGlow
import com.lurecalendar.app.ui.theme.MoonLight

/**
 * 月相3D轻拟物渲染组件
 * 使用 Canvas 自定义绘制月相，带径向渐变模拟球体光影
 */
@Composable
fun MoonPhaseView(
    moonPhase: String,      // "新月"/"满月"/"上弦月" 等
    lunarDate: String,      // "四月初八" 等
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 月球 Canvas 绘制
        Canvas(
            modifier = Modifier.size(64.dp)
        ) {
            drawMoonPhase(moonPhase)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 农历日期
        Text(
            text = lunarDate,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // 月相名称
        Text(
            text = moonPhase,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 根据月相名称绘制月球
 */
private fun DrawScope.drawMoonPhase(moonPhase: String) {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    // 1. 绘制月球底色（深色背景，代表暗面）
    drawCircle(
        color = MoonDark,
        radius = radius,
        center = center
    )

    // 2. 计算亮面比例和方向
    val illumination = getIllumination(moonPhase)
    val isWaxing = isWaxingPhase(moonPhase)

    // 3. 绘制亮面（带3D球体径向渐变）
    if (illumination > 0f) {
        val brightPath = createMoonBrightPath(
            center = center,
            radius = radius,
            illumination = illumination,
            isWaxing = isWaxing
        )

        clipPath(brightPath) {
            // 3D球体效果: 径向渐变模拟光照
            val lightOffset = if (isWaxing) -radius * 0.2f else radius * 0.2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MoonGlow,
                        MoonLight,
                        MoonLight.copy(alpha = 0.8f)
                    ),
                    center = Offset(center.x + lightOffset, center.y - radius * 0.15f),
                    radius = radius * 1.2f
                ),
                radius = radius,
                center = center
            )

            // 表面纹理: 环形山
            drawMoonCraters(center, radius)
        }
    }

    // 4. 球体边缘高光（顶部轻微反光）
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = Offset(center.x, center.y - radius * 0.4f),
            radius = radius * 0.6f
        ),
        radius = radius,
        center = center
    )

    // 5. 球体外发光光晕（亮度较高时显示）
    if (illumination > 0.3f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    MoonGlow.copy(alpha = 0.04f * illumination),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.3f
            ),
            radius = radius * 1.3f,
            center = center
        )
    }
}

/**
 * 创建月球亮面裁剪路径
 * 使用 arcTo 实现不同月相的亮面形状
 */
private fun createMoonBrightPath(
    center: Offset,
    radius: Float,
    illumination: Float,
    isWaxing: Boolean
): Path {
    val path = Path()
    val oval = Rect(center = center, radius = radius)

    when {
        illumination >= 1f -> {
            // 满月: 整圆
            path.addOval(oval)
        }
        illumination >= 0.5f -> {
            // 上弦月/盈凸月 或 下弦月/亏凸月
            // 用半圆 + 椭圆弧组合
            val ovalFraction = (illumination - 0.5f) * 2f // 0..1 range for the extra portion
            val innerOvalHalfWidth = radius * ovalFraction

            if (isWaxing) {
                // 右侧亮: 右半圆 + 左侧椭圆扩展
                // 右半圆弧 (从顶部到底部，经过右侧)
                path.arcTo(oval, -90f, 180f, false)
                // 左侧回路用椭圆弧
                val innerOval = Rect(
                    left = center.x - innerOvalHalfWidth,
                    top = center.y - radius,
                    right = center.x + innerOvalHalfWidth,
                    bottom = center.y + radius
                )
                path.arcTo(innerOval, 90f, 180f, false)
            } else {
                // 左侧亮: 左半圆 + 右侧椭圆扩展
                path.arcTo(oval, 90f, 180f, false)
                val innerOval = Rect(
                    left = center.x - innerOvalHalfWidth,
                    top = center.y - radius,
                    right = center.x + innerOvalHalfWidth,
                    bottom = center.y + radius
                )
                path.arcTo(innerOval, -90f, 180f, false)
            }
            path.close()
        }
        else -> {
            // 蛾眉月/残月: 窄月牙
            // illumination < 0.5: 用两段弧组合月牙形状
            val crescentFraction = illumination * 2f // 0..1
            val innerOvalHalfWidth = radius * (1f - crescentFraction)

            if (isWaxing) {
                // 蛾眉月: 右侧窄月牙
                // 外弧: 右半的大圆弧
                path.arcTo(oval, -90f, 180f, false)
                // 内弧: 右偏椭圆弧（向右凹进去）
                val innerOval = Rect(
                    left = center.x - innerOvalHalfWidth,
                    top = center.y - radius,
                    right = center.x + innerOvalHalfWidth,
                    bottom = center.y + radius
                )
                path.arcTo(innerOval, 90f, -180f, false)
            } else {
                // 残月: 左侧窄月牙
                path.arcTo(oval, 90f, 180f, false)
                val innerOval = Rect(
                    left = center.x - innerOvalHalfWidth,
                    top = center.y - radius,
                    right = center.x + innerOvalHalfWidth,
                    bottom = center.y + radius
                )
                path.arcTo(innerOval, -90f, -180f, false)
            }
            path.close()
        }
    }

    return path
}

/** 绘制月球表面环形山纹理 */
private fun DrawScope.drawMoonCraters(center: Offset, radius: Float) {
    val craterColor = Color(0x18000000)
    drawCircle(craterColor, radius * 0.08f, Offset(center.x - radius * 0.2f, center.y - radius * 0.3f))
    drawCircle(craterColor, radius * 0.12f, Offset(center.x + radius * 0.15f, center.y + radius * 0.2f))
    drawCircle(craterColor, radius * 0.06f, Offset(center.x + radius * 0.35f, center.y - radius * 0.1f))
    drawCircle(craterColor, radius * 0.09f, Offset(center.x - radius * 0.3f, center.y + radius * 0.35f))
    drawCircle(craterColor, radius * 0.05f, Offset(center.x + radius * 0.1f, center.y - radius * 0.45f))
}

/** 根据月相名称获取亮面比例 (0=全暗, 1=全亮) */
private fun getIllumination(moonPhase: String): Float = when {
    moonPhase.contains("新月") -> 0f
    moonPhase.contains("蛾眉") -> 0.2f
    moonPhase.contains("上弦") -> 0.5f
    moonPhase.contains("盈凸") -> 0.8f
    moonPhase.contains("满月") -> 1f
    moonPhase.contains("亏凸") -> 0.8f
    moonPhase.contains("下弦") -> 0.5f
    moonPhase.contains("残月") -> 0.2f
    else -> 0.5f
}

/** 判断是否为盈相（月相增大方向，右侧亮） */
private fun isWaxingPhase(moonPhase: String): Boolean = when {
    moonPhase.contains("蛾眉") -> true
    moonPhase.contains("上弦") -> true
    moonPhase.contains("盈凸") -> true
    moonPhase.contains("满月") -> true
    else -> false // 亏凸、下弦、残月、新月
}

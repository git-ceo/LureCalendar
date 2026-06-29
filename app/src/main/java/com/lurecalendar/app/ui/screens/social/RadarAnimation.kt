package com.lurecalendar.app.ui.screens.social

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.lurecalendar.app.ui.theme.WaterCyan

@Composable
fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    
    val radius1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing)),
        label = "r1"
    )
    
    val radius2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(3000, delayMillis = 1000, easing = LinearEasing)),
        label = "r2"
    )

    val radius3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(3000, delayMillis = 2000, easing = LinearEasing)),
        label = "r3"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = center
        val maxRadius = size.minDimension / 1.5f
        
        listOf(radius1, radius2, radius3).forEach { r ->
            drawCircle(
                color = WaterCyan.copy(alpha = 1f - r),
                radius = maxRadius * r,
                center = center,
                style = Stroke(width = 2f)
            )
        }
    }
}

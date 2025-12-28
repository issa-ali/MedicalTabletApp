package com.example.androidapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.*
import com.example.androidapp.ui.theme.NeonCyan
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

@Composable
fun ScanningOverlay(active: Boolean, content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanPos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_pos"
    )

    Box {
        content()
        if (active) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val y = size.height * scanPos
                
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.48f to NeonCyan.copy(alpha = 0.8f),
                        0.5f to Color.White,
                        0.52f to NeonCyan.copy(alpha = 0.8f),
                        1f to Color.Transparent,
                        startY = y - 30f,
                        endY = y + 30f
                    ),
                    topLeft = Offset(0f, y - 30f),
                    size = size.copy(height = 60f)
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to NeonCyan.copy(alpha = 0.15f),
                        1f to Color.Transparent,
                        startY = y - 150f,
                        endY = y + 150f
                    ),
                    topLeft = Offset(0f, y - 150f),
                    size = size.copy(height = 300f)
                )
            }
        }
    }
}

fun Modifier.glassmorphic(
    hazeState: HazeState? = null,
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    backgroundColor: Color = Color.White.copy(alpha = 0.05f),
    cornerRadius: Dp = 24.dp
) = this
    .then(
        if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = RoundedCornerShape(cornerRadius))
        else Modifier
    )
    .background(
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(borderColor, Color.Transparent)
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

fun Modifier.neonGlow(
    color: Color,
    radius: Dp = 8.dp
) = this.drawBehind {
    drawCircle(
        color = color.copy(alpha = 0.15f),
        radius = radius.toPx(),
        center = Offset(size.width / 2f, size.height / 2f),
        blendMode = BlendMode.Screen
    )
}

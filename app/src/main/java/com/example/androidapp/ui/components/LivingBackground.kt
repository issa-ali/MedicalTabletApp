package com.example.androidapp.ui.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.DeepSpaceBlack
import com.example.androidapp.ui.theme.NeonCyan
import com.example.androidapp.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LivingBackground(rotation: Pair<Float, Float> = Pair(0f, 0f)) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_flow")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlack)) {
        NeuralParticles(rotation)
        
        // AAA Titan Logic: Single Canvas for all light layers to minimize GPU context switching
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Cinematic Backdrop (Draw directly in canvas to save layout layers)
            // Note: We can't easily draw painterResource in generic Canvas without a Painter, 
            // but we can keep the Image layer above for simplicity, OR draw it here.
            // Keeping Image layer for now as it's hardware accelerated.

            // 2. Simulated Shader Flux: Moving Gradient Orbs
            repeat(3) { i ->
                val offsetX = cos(time + (i * 2f)) * (width * 0.15f)
                val offsetY = sin(time * 0.7f + i) * (height * 0.15f)
                
                val centerX = width * (0.3f + i * 0.2f) + offsetX
                val centerY = height * (0.5f) + offsetY
                val radius = 500f + (i * 80f)
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (i % 2 == 0) listOf(NeonPurple.copy(alpha = 0.1f * flicker), Color.Transparent) 
                                 else listOf(NeonCyan.copy(alpha = 0.06f * flicker), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Plus
                )
            }

            // 3. Interactive Starfield (Optimized placement)
            val random = java.util.Random(1337)
            repeat(40) {
                val x = random.nextFloat() * width
                val y = random.nextFloat() * height
                drawCircle(Color.White, 1.5f, Offset(x, y), alpha = 0.3f * flicker)
            }
        }
    }
}

private fun DrawScope.drawOrb(centerX: Float, centerY: Float, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = Offset(centerX, centerY),
            radius = radius
        ),
        radius = radius,
        center = Offset(centerX, centerY),
        blendMode = androidx.compose.ui.graphics.BlendMode.Plus
    )
}

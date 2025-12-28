package com.example.androidapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AnimatedBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")

    // Animate a slow rotation or movement
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Deep space gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E293B), // Lighter center
                            Color(0xFF0F172A), // Darker edges
                            Color.Black
                        ),
                        center = Offset.Unspecified,
                        radius = 1500f
                    )
                )
        )

        // Starfield / Particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Draw some "stars" or particles that drift
            // Since we can't easily store state in Canvas draw scope without remembering it outside, 
            // we'll generate deterministic positions based on index but animate them with 'time'
            
            val particleCount = 100
            for (i in 0 until particleCount) {
                // Deterministic random to keep stars same place across frames unless animated
                val random = Random(i)
                val initialX = random.nextFloat() * width
                val initialY = random.nextFloat() * height
                val speed = 10f + random.nextFloat() * 50f
                val radius = 1f + random.nextFloat() * 2f
                val alpha = 0.3f + random.nextFloat() * 0.7f

                // Simple flow animation: moving downwards or sideways
                // Let's make them flow slowly to the right and slightly down
                val offsetX = (initialX + time * speed) % width
                val offsetY = (initialY + time * speed * 0.2f) % height

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(offsetX, offsetY)
                )
            }
            
            // Draw a subtle grid if desired for "Sci-Fi" look
            // ... (keeping it simple for now to avoid clutter)
        }
    }
}

@Preview
@Composable
fun AnimatedBackgroundPreview() {
    AnimatedBackground()
}

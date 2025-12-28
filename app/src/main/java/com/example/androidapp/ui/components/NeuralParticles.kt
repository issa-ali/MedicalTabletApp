package com.example.androidapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.androidapp.ui.theme.NeonCyan
import com.example.androidapp.ui.theme.NeonPurple
import java.util.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float
)

@Composable
fun NeuralParticles(rotation: Pair<Float, Float> = Pair(0f, 0f)) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    // Titan Optimization: Pre-allocate particles to avoid GC pressure on 2GB RAM
    val particles = remember {
        val random = Random(42)
        List(100) {
            Particle(
                x = random.nextFloat(),
                y = random.nextFloat(),
                vx = (random.nextFloat() - 0.5f) * 0.002f,
                vy = (random.nextFloat() - 0.5f) * 0.002f,
                color = if (random.nextBoolean()) NeonCyan.copy(alpha = 0.4f) else NeonPurple.copy(alpha = 0.3f),
                size = 2f + random.nextFloat() * 4f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Titan Physics: Link particles to device rotation (The 'Float' effect)
        val tiltX = rotation.second * 20f // Roll affects X
        val tiltY = rotation.first * 20f  // Pitch affects Y

        particles.forEach { p ->
            // Update positions with momentum + tilt
            p.x = (p.x + p.vx + (tiltX / width)) % 1f
            p.y = (p.y + p.vy + (tiltY / height)) % 1f
            
            if (p.x < 0) p.x += 1f
            if (p.y < 0) p.y += 1f

            drawCircle(
                color = p.color,
                radius = p.size,
                center = Offset(p.x * width, p.y * height),
                blendMode = androidx.compose.ui.graphics.BlendMode.Plus
            )
        }
    }
}

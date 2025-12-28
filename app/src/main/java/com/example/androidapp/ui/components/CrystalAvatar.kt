package com.example.androidapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.NeonTeal
import com.airbnb.lottie.compose.*

@Composable
fun CrystalAvatar(
    isSpeaking: Boolean,
    amplitude: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_fx")
    
    // 3D Spatial Tilting (Always on, subtle)
    val rotateX by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt_x"
    )
    val rotateY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt_y"
    )

    // Pulse animation (When speaking) - Enhanced with Titan Amplitude
    val speakScale by animateFloatAsState(
        targetValue = if (isSpeaking) 1.0f + (amplitude * 0.2f) else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "speak_pulse"
    )

    // Glow intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val titanGlow = if (isSpeaking) (glowAlpha + amplitude).coerceAtMost(1f) else glowAlpha

    Box(
        modifier = modifier.size(450.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. NEON VOID GLOW (Professional Layer)
        Box(
            modifier = Modifier
                .size(300.dp)
                .graphicsLayer { alpha = titanGlow }
                .background(
                    Brush.radialGradient(listOf(NeonTeal.copy(0.5f), Color.Transparent)),
                    CircleShape
                )
                .blur(50.dp)
        )

        // 2. THE CINEMATIC ROBOT (3D Layer)
        Image(
            painter = painterResource(id = com.example.androidapp.R.drawable.avatar_cinematic),
            contentDescription = "AI Medical Assistant",
            modifier = Modifier
                .size(400.dp)
                .graphicsLayer {
                    this.rotationX = rotateX
                    this.rotationY = rotateY
                    this.scaleX = speakScale
                    this.scaleY = speakScale
                    this.cameraDistance = 12f * density
                },
            contentScale = ContentScale.Fit
        )

        // 3. BRAIN HOLOGRAM (Lottie Layer)
        val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://lottie.host/7900b147-3aed-494b-9705-1a86847c20c4/5L6d5L6d.json"))
        val progress by animateLottieCompositionAsState(
            composition,
            iterations = LottieConstants.IterateForever,
            speed = if (isSpeaking) 1.5f else 0.5f
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(120.dp)
                .offset(y = (-110).dp) // Positioning over the robot's head
                .graphicsLayer {
                    alpha = if (isSpeaking) 0.9f else 0.4f
                    scaleX = if (isSpeaking) 1.2f else 1.0f
                    scaleY = if (isSpeaking) 1.2f else 1.0f
                }
        )
    }
}

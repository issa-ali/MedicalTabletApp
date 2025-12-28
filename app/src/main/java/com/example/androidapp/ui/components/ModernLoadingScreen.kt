package com.example.androidapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.ui.theme.NeonCyan
import com.example.androidapp.ui.theme.NeonPurple

import androidx.compose.ui.res.stringResource
import com.example.androidapp.R

@Composable
fun ModernLoadingScreen(
    loadingText: String = stringResource(R.string.initializing_core)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E12)), // Deep background
        contentAlignment = Alignment.Center
    ) {
        // animated background mesh/grid could go here, but keeping it clean for now
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Main Spinner
            NeonSpinner()
            
            // Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = loadingText,
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.secure_connection),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun NeonSpinner() {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            
            // Outer Ring (Purple)
            withTransform({ rotate(rotation) }) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, NeonPurple, Color.Transparent)
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
            }
            
            // Inner Ring (Cyan) - Counter Rotation
            withTransform({ rotate(-rotation * 1.5f) }) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, NeonCyan, Color.Transparent)
                    ),
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(20f, 20f),
                    size = Size(size.width - 40f, size.height - 40f)
                )
            }
            
            // Center Dot
            drawCircle(
                color = NeonCyan,
                radius = 8.dp.toPx() * pulse,
                center = center
            )
        }
    }
}

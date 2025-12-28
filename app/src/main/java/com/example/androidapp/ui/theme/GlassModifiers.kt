package com.example.androidapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a "Future Glass" effect:
 * - Semi-transparent dark background
 * - Subtle gradient border
 * - Rounded corners
 */
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color(0xFF1E293B).copy(alpha = 0.6f),
    borderColor: Color = GlassWhite10
): Modifier = composed {
    this
        .background(
            color = backgroundColor,
            shape = shape
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor,
                    Color.Transparent
                )
            ),
            shape = shape
        )
}

/**
 * A more intense glass effect for active elements
 */
fun Modifier.neonGlassEffect(
    shape: Shape = RoundedCornerShape(24.dp),
    neonColor: Color = NeonCyan
): Modifier = composed {
    this
        .background(
            color = neonColor.copy(alpha = 0.05f),
            shape = shape
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    neonColor.copy(alpha = 0.5f),
                    Color.Transparent
                )
            ),
            shape = shape
        )
}

package com.example.androidapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Base Neutrals & Backgrounds (Deep Space)
val DeepSpaceBlack = Color(0xFF050B14)
val DeepSpaceBlue = Color(0xFF0F172A)
val VoidBlack = Color(0xFF000000)

// Neon Accents (Medical Future - Vibrant Start-up Shift)
val NeonCyan = Color(0xFF00FBFF) // Electric Blue (Vibrant)
val NeonTeal = Color(0xFF00FFD5) // Mint/Success
val NeonPurple = Color(0xFFE200FF) // Laser Magenta (Vibrant)
val NeonRed = Color(0xFFFF1744) // Alert / High HR
val NeonAmber = Color(0xFFFFC400) // Warning

// Glass System
val GlassWhite80 = Color(0xCCFFFFFF)
val GlassWhite50 = Color(0x80FFFFFF)
val GlassWhite40 = Color(0x66FFFFFF) // Added
val GlassWhite30 = Color(0x4DFFFFFF)
val GlassWhite20 = Color(0x33FFFFFF) // Added
val GlassWhite10 = Color(0x1AFFFFFF)
val GlassWhite05 = Color(0x0DFFFFFF)

// Gradients
val GradientCrystal = Brush.linearGradient(
    colors = listOf(NeonCyan, NeonPurple)
)

val GradientBackground = Brush.radialGradient(
    colors = listOf(Color(0xFF1A2332), DeepSpaceBlack),
    radius = 1500f
)

// Material Theme Mappings
val Primary = NeonCyan
val OnPrimary = Color.Black
val Secondary = NeonPurple
val Background = DeepSpaceBlack
val Surface = Color(0xFF111827)
val Error = NeonRed

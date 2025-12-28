package com.example.androidapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.LivingBackground
import com.example.androidapp.ui.components.glassmorphic
import com.example.androidapp.ui.components.neonGlow
import com.example.androidapp.ui.theme.*

@Composable
fun WelcomeScreen(
    userName: String = "Patient",
    onStartConsultation: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LivingBackground()
        
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .glassmorphic()
                    .padding(64.dp)
            ) {
                Text(
                    text = "Welcome, $userName",
                    style = MaterialTheme.typography.displayMedium,
                    color = NeonCyan
                )
                
                Text(
                    text = "AI Medical Assistant Ready",
                    style = MaterialTheme.typography.headlineSmall,
                    color = GlassWhite50
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Start Button
            Box(
                modifier = Modifier
                    .size(240.dp, 64.dp)
                    .glassmorphic(borderColor = NeonCyan)
                    .neonGlow(NeonCyan)
                    .clickable { onStartConsultation() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "START SYSTEM",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Footer
        Text(
            text = "System Online • Version 2.0 (Demo)",
            color = GlassWhite50,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

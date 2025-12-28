package com.example.androidapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.LivingBackground
import com.example.androidapp.ui.components.glassmorphic
import com.example.androidapp.ui.theme.NeonCyan

@Composable
fun DashboardScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        LivingBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "MEDICAL SESSION DASHBOARD",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                DashboardCard("Active Shifts", "Dr. AI (Neurology)", Modifier.weight(1f))
                DashboardCard("System Load", "3.2% Sensor Traffic", Modifier.weight(1f))
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .glassmorphic()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "WAITING FOR PATIENT SCAN...",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DashboardCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .glassmorphic()
            .padding(24.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.5f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
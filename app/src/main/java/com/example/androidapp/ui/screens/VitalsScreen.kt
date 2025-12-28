package com.example.androidapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.ui.components.LivingBackground
import com.example.androidapp.ui.components.glassmorphic
import com.example.androidapp.ui.components.neonGlow
import com.example.androidapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.sin
import androidx.activity.compose.rememberLauncherForActivityResult

@Composable
fun VitalsScreen(
    viewModel: VitalsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val vitals by viewModel.vitals.collectAsState()
    
    // Permission & Auto-connect
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
             val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                 context, 
                 android.Manifest.permission.BLUETOOTH_CONNECT
             ) == android.content.pm.PackageManager.PERMISSION_GRANTED
             
             if (!hasPerm) {
                 launcher.launch(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN))
             } else {
                 viewModel.connect()
             }
        } else {
             viewModel.connect()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LivingBackground()
        
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: ESP32 Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(borderColor = NeonTeal.copy(alpha = 0.3f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ESP32 Sensor Array: ", color = GlassWhite80)
                    Text(
                        if (isConnected) "CONNECTED" else "SEARCHING...",
                        color = if (isConnected) NeonTeal else NeonAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Signal Bars Sim
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height((10 + i * 6).dp)
                                .background(
                                    if (isConnected) NeonTeal else GlassWhite10,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
            
            // Main Grid
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT COL: Heart Rate & ECG
                Column(
                    modifier = Modifier.weight(1.5f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HR & SpO2
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.startHeartRate() }
                    ) {
                        Column {
                            Text("Heart Rate & SpO2", color = GlassWhite80, fontWeight = FontWeight.SemiBold)
                            Text("Click to Stream PPG", style = MaterialTheme.typography.labelSmall, color = GlassWhite50)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (isConnected) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text("${vitals.heartRate}", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                    Text(" BPM", fontSize = 24.sp, color = NeonCyan, modifier = Modifier.padding(bottom = 8.dp))
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("${vitals.spo2}%", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = NeonTeal)
                                    Text(" SpO2", fontSize = 24.sp, color = NeonTeal, modifier = Modifier.padding(bottom = 8.dp))
                                }
                                SimulatedGraph(
                                    color = NeonCyan, 
                                    speed = 300,
                                    isActive = vitals.heartRate > 0
                                )
                            } else {
                                Text("Waiting for sensor...", color = GlassWhite30)
                            }
                        }
                    }
                    
                    // ECG
                    GlassCard(
                        modifier = Modifier
                            .weight(0.8f)
                            .clickable { viewModel.startECG() }
                    ) {
                        Column {
                            Text("ECG / EKG", color = GlassWhite80, fontWeight = FontWeight.SemiBold)
                            Text("Click to Stream ECG", style = MaterialTheme.typography.labelSmall, color = GlassWhite50)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isConnected) {
                                SimulatedGraph(
                                    color = NeonTeal, 
                                    speed = 100, 
                                    isSpiky = true,
                                    isActive = vitals.ecg > 100 // Threshold for valid ECG signal
                                )
                            }
                        }
                    }
                }
                
                // RIGHT COL: Temp & BP
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Temp
                    GlassCard(
                        modifier = Modifier
                            .weight(0.6f)
                            .clickable { viewModel.startTemp() }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Body Temperature", color = GlassWhite80)
                            Text("Click to Read Object Temp", style = MaterialTheme.typography.labelSmall, color = GlassWhite50)
                            Spacer(modifier = Modifier.height(24.dp))
                            if (isConnected) {
                                Text("${vitals.temperature}°C", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                Text("Normal", color = NeonTeal)
                            } else {
                                Text("--.-°C", fontSize = 56.sp, color = GlassWhite10)
                            }
                        }
                    }
                    
                    // BP Manual Entry
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Blood Pressure", color = GlassWhite80)
                            Spacer(modifier = Modifier.height(16.dp))
                            // Placeholder for buttons
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .glassmorphic(borderColor = NeonCyan)
                                    .clickable { viewModel.startBP() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("START MEASUREMENT", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic()
            .padding(24.dp),
        content = content
    )
}

@Composable
fun SimulatedGraph(
    color: Color, 
    speed: Int, 
    isSpiky: Boolean = false,
    isActive: Boolean = false // New param: only animate if real data is flowing
) {
    val infiniteTransition = rememberInfiniteTransition(label = "graph")
    
    // Only animate phase if isActive is true
    val phase by if (isActive) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(speed, easing = LinearEasing)
            ),
            label = "phase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        val path = Path()
        path.moveTo(0f, centerY)
        
        val points = 50
        for (i in 0..points) {
            val x = i * (width / points)
            
            // If not active, draw flat line
            val yOffset = if (isActive) {
                if (isSpiky) {
                     // ECG-ish spike
                     if (i % 10 == 0) -50f else 0f 
                } else {
                     // Smooth breathing/HR wave
                     sin((i + phase * 10).toDouble()).toFloat() * 30f 
                }
            } else {
                0f
            }
            
            path.lineTo(x, centerY + yOffset)
        }
        
        drawPath(
            path = path,
            color = if (isActive) color else color.copy(alpha = 0.3f),
            style = Stroke(width = 3f)
        )
    }
}

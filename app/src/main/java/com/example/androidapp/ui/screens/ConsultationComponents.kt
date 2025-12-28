package com.example.androidapp.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.example.androidapp.R
import com.airbnb.lottie.compose.*
import dev.chrisbanes.haze.HazeState
import com.example.androidapp.ui.theme.*
import com.example.androidapp.ui.components.*
import com.example.androidapp.data.models.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun TimerHeader(
    seconds: Long, 
    onEndSession: () -> Unit,
    onDebugNavigate: (ConsultationPhase) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xFF2A3441).copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            val m = seconds / 60
            val s = seconds % 60
            Text(stringResource(R.string.session_timer, String.format("%02d:%02d", m, s)), color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .neonGlow(NeonPurple)
                .background(Color(0xFF2A3441).copy(alpha = 0.9f), CircleShape)
                .clickable { showMenu = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, "Settings", tint = Color.White.copy(0.7f), modifier = Modifier.size(22.dp))
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF2D3748))
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.end_session), color = NeonRed) },
                    onClick = { showMenu = false; onEndSession() },
                    leadingIcon = { Icon(Icons.Default.ExitToApp, null, tint = NeonRed) }
                )
                HorizontalDivider(color = Color.White.copy(0.1f))
                DropdownMenuItem(
                    text = { Text("Skip to Vitals (Debug)", color = Color.White) },
                    onClick = { showMenu = false; onDebugNavigate(ConsultationPhase.VITALS) }
                )
                DropdownMenuItem(
                    text = { Text("Skip to Report (Debug)", color = Color.White) },
                    onClick = { showMenu = false; onDebugNavigate(ConsultationPhase.REPORT) }
                )
            }
        }
    }
}

@Composable
fun WelcomeWidget(userName: String, hazeState: HazeState? = null, onStart: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Column(
        modifier = Modifier
            .widthIn(max = 550.dp)
            .glassmorphic(hazeState = hazeState, borderColor = NeonPurple.copy(alpha = 0.5f))
            .padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Patient Consultation: $userName", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        TypingText("I am your AI Medical Assistant. I will guide you through a comprehensive health assessment. Please state your symptoms below. 👇", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.75f))
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neonGlow(NeonTeal)
                .background(Brush.horizontalGradient(listOf(Color(0xFF2DD4BF), Color(0xFF3ABAB4))), RoundedCornerShape(12.dp))
                .clickable { 
                    com.example.androidapp.data.audio.SoundManager.playSuccess()
                    onStart() 
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Start Consultation", color = Color(0xFF1A202C), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ChatWidget(
    chatMessages: List<ChatMessage>,
    listState: LazyListState,
    isListening: Boolean,
    isThinking: Boolean,
    onMicClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatMessages, key = { it.id }) { message ->
                ChatBubble(message)
            }
            if (isThinking) {
                item(key = "thinking") { ThinkingIndicator() }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FloatingActionButton(
                onClick = {
                    com.example.androidapp.data.audio.SoundManager.playTechScan()
                    onMicClick()
                },
                containerColor = if (isListening) NeonRed else NeonTeal,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(72.dp).neonGlow(if (isListening) NeonRed else NeonTeal)
            ) {
                Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, null, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isAi = message.role == "ai"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .glassmorphic(
                    borderColor = if (isAi) NeonCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                    backgroundColor = if (isAi) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.05f)
                )
                .padding(16.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isAi) Color.White else Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun ThinkingIndicator() {
    val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://lottie.host/7900b147-3aed-494b-9705-1a86847c20c4/5L6d5L6d.json"))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("Thinking...", color = NeonCyan.copy(0.6f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun LiveECGWaveform(ecgValue: Int, modifier: Modifier = Modifier) {
    val points = remember { mutableStateListOf<Float>() }
    val maxPoints = 50
    
    LaunchedEffect(ecgValue) {
        points.add(ecgValue.toFloat())
        if (points.size > maxPoints) points.removeAt(0)
    }

    Canvas(modifier = modifier.height(120.dp).fillMaxWidth().graphicsLayer(alpha = 0.99f)) {
        if (points.size < 2) return@Canvas
        
        val width = size.width
        val height = size.height
        val xStep = width / (maxPoints - 1).toFloat()
        
        // AAA Titan Logic: Draw single path to save GPU draw calls
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = index * xStep
            val y = height - (value / 4095f * height)
            if (index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = NeonTeal,
            style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        
        drawPath(
            path = path,
            color = NeonTeal.copy(alpha = 0.2f),
            style = Stroke(width = 8f)
        )
    }
}

@Composable
fun VitalsWidget(
    vitals: List<VitalSign>,
    selectedVital: VitalSign?,
    bluetoothData: com.example.androidapp.data.bluetooth.VitalsData,
    isBluetoothConnected: Boolean,
    onSelectVital: (VitalSign) -> Unit,
    onMeasureComplete: (String, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    ScanningOverlay(active = isBluetoothConnected) {
        if (selectedVital != null) {
            Column(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            Text("Measuring ${selectedVital.name}", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
            Icon(selectedVital.icon, null, tint = NeonPurple, modifier = Modifier.size(64.dp))
            Text(selectedVital.instruction, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            
            if (isBluetoothConnected) {
                Spacer(Modifier.height(16.dp))
                when (selectedVital.id) {
                    "hr" -> {
                        Text("HR: ${bluetoothData.heartRate} BPM", color = NeonCyan, style = MaterialTheme.typography.titleLarge)
                        Text("SpO2: ${bluetoothData.spo2}%", color = NeonTeal, style = MaterialTheme.typography.titleMedium)
                    }
                    "temp" -> {
                        Text("${bluetoothData.temperature}°C", color = NeonCyan, style = MaterialTheme.typography.titleLarge)
                    }
                    "bp" -> {
                        Text("SYS: ${bluetoothData.systolic}", color = NeonCyan, style = MaterialTheme.typography.titleLarge)
                        Text("DIA: ${bluetoothData.diastolic}", color = NeonTeal, style = MaterialTheme.typography.titleMedium)
                    }
                    "ecg" -> {
                        Text("Stable ECG Signal", color = NeonTeal, style = MaterialTheme.typography.labelMedium)
                        LiveECGWaveform(bluetoothData.ecg, modifier = Modifier.padding(vertical = 16.dp))
                    }
                }
                
                Button(
                    onClick = { 
                        val result = when (selectedVital.id) {
                            "hr" -> "${bluetoothData.heartRate} bpm / ${bluetoothData.spo2}%"
                            "temp" -> "${bluetoothData.temperature} °C"
                            "bp" -> "${bluetoothData.systolic}/${bluetoothData.diastolic} mmHg"
                            "ecg" -> "Recorded"
                            else -> "Done"
                        }
                        onMeasureComplete(selectedVital.id, result)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                ) {
                    Text("Done / Accept Value", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://lottie.host/8051a6d7-cd6b-4f90-8edb-7c70281b674b/6T6T6T6T.json"))
                val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(150.dp).neonGlow(NeonCyan)
                )
                TypingText("Connecting to Secure Sensors...", style = MaterialTheme.typography.bodyMedium)
            }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VitalsList(vitals = vitals, selectedVital = selectedVital, onSelect = onSelectVital, modifier = Modifier.weight(1f))
                
                // Show "Generate Report" button only when all vitals are measured
                if (vitals.all { it.value != null }) {
                    Button(
                        onClick = { onMeasureComplete("trigger_report", "confirmed") }, // We use this callback to advance
                        modifier = Modifier.fillMaxWidth().height(60.dp).neonGlow(NeonTeal),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                    ) {
                        Icon(Icons.Default.Analytics, null, tint = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        Text("ANALYZE & GENERATE REPORT", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VitalsList(
    vitals: List<VitalSign>,
    selectedVital: VitalSign?,
    onSelect: (VitalSign) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
    ) {
        items(vitals, key = { it.id }) { vital ->
            val isSelected = selectedVital?.id == vital.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(
                        borderColor = if (isSelected) NeonCyan else Color.White.copy(0.1f),
                        backgroundColor = if (isSelected) NeonCyan.copy(0.1f) else Color.White.copy(0.05f)
                    )
                    .clickable { onSelect(vital) }
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(vital.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(vital.id.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                }
                
                if (vital.value != null) {
                    Text("${vital.value} ${vital.unit}", style = MaterialTheme.typography.headlineSmall, color = NeonTeal, fontWeight = FontWeight.Bold)
                } else if (isSelected) {
                    androidx.compose.material3.CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun ReportWidget(
    userName: String,
    detected: String,
    recommendation: String,
    medicine: String,
    isAnalyzing: Boolean,
    onComplete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .glassmorphic(borderColor = NeonTeal.copy(alpha = 0.5f))
            .padding(48.dp)
    ) {
        if (isAnalyzing) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = NeonCyan)
                Spacer(Modifier.height(16.dp))
                TypingText("Preparing your health summary...", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    "📋 CLINICAL SUMMARY: $userName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonTeal,
                    fontWeight = FontWeight.Bold
                )
                
                ReportDetail("🔍 Detected Condition", detected, NeonCyan)
                ReportDetail("🤖 AI Recommendation", recommendation, Color.White)
                ReportDetail("💊 Suggested Treatment", medicine, NeonPurple)

                Spacer(Modifier.weight(1f))
                
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                ) {
                    Text("PROCEED TO PHARMACY", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReportDetail(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.5f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PharmacyWidget(
    userId: String,
    medicine: Medicine?,
    userBalance: Float,
    isBuyingInProgress: Boolean, // Renamed to avoid confusion
    onPurchaseSuccess: (Float) -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPurchased by remember { mutableStateOf(false) }
    var isBuying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .glassmorphic(borderColor = NeonPurple.copy(alpha = 0.5f))
            .padding(48.dp)
    ) {
        if (!isPurchased) {
            if (medicine == null) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NO PRESCRIPTIONS FOUND AT THIS TIME", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.5f))
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = onComplete) { Text("CONTINUE") }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("YOUR PRESCRIBED TREATMENT", style = MaterialTheme.typography.headlineSmall, color = NeonPurple, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(16.dp))
                        /* Removed student discount badge */
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(backgroundColor = Color.White.copy(0.08f))
                            .padding(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(medicine.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text(medicine.description, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))
                        }
                        Text(medicine.price, style = MaterialTheme.typography.headlineSmall, color = NeonCyan, fontWeight = FontWeight.ExtraBold)
                    }

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = NeonRed, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ACCOUNT BALANCE: ${"%.2f".format(userBalance)} EGP", color = Color.White.copy(0.7f), fontWeight = FontWeight.SemiBold)
                        
                        val priceNum = medicine.price.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 0.0f
                        val canAfford = userBalance >= priceNum

                        Button(
                            onClick = { 
                                if (canAfford && medicine.id != null) {
                                    scope.launch {
                                        isBuying = true
                                        errorMessage = null
                                        try {
                                            val response = com.example.androidapp.data.api.NetworkModule.api.purchase(
                                                com.example.androidapp.data.api.PurchaseRequest(
                                                    userId = userId,
                                                    productId = medicine.id!!,
                                                    quantity = 1
                                                )
                                            )
                                            if (response.success) {
                                                isPurchased = true
                                                onPurchaseSuccess(response.newBalance ?: (userBalance - priceNum))
                                            } else {
                                                errorMessage = response.error ?: "Transaction failed"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Secure connection timeout. Please try again."
                                        } finally {
                                            isBuying = false
                                        }
                                    }
                                }
                            },
                            enabled = canAfford && !isBuying,
                            modifier = Modifier.width(300.dp).height(64.dp).neonGlow(if (canAfford) NeonPurple else Color.Transparent),
                            colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) NeonPurple else Color.Gray)
                        ) {
                            if (isBuying) {
                                androidx.compose.material3.CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(if (canAfford) "CONFIRM PURCHASE" else "INSUFFICIENT FUNDS", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = NeonTeal, modifier = Modifier.size(120.dp).neonGlow(NeonTeal, 32.dp))
                Text("Prescription Dispensed", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Your medication is ready for pickup.", color = Color.White.copy(0.7f))
                Spacer(Modifier.height(32.dp))
                Button(onClick = onComplete, modifier = Modifier.width(200.dp).height(50.dp)) {
                    Text("FINISH SESSION")
                }
            }
        }
    }
}

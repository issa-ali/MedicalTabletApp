package com.example.androidapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.ui.components.*
import com.example.androidapp.ui.theme.*
import com.example.androidapp.data.models.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

@Composable
fun ConsultationScreen(
    userName: String,
    userId: String,
    initialBalance: Float,
    onConsultationComplete: () -> Unit,
    viewModel: ConsultationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val currentPhase by viewModel.currentPhase.collectAsState(ConsultationPhase.WELCOME)
    val isAppLoading by viewModel.isAppLoading.collectAsState(true)
    val isAiSpeaking by viewModel.isAiSpeaking.collectAsState(false)
    val sessionDuration by viewModel.sessionDurationSeconds.collectAsState(0L)
    val voiceAmplitude by viewModel.voiceAmplitude.collectAsState(0f)
    val selectedVital by viewModel.selectedVital.collectAsState(null)
    
    val bluetoothVitals by viewModel.bluetoothVitals.collectAsState(com.example.androidapp.data.bluetooth.VitalsData())
    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsState(false)
    
    val chatMessages = viewModel.chatMessages
    val listState = rememberLazyListState()
    val isThinking by viewModel.isThinking.collectAsState(false)
    
    val aiRecommendation by viewModel.aiRecommendation.collectAsState("")
    val detectedType by viewModel.detectedType.collectAsState("")
    val recommendedMedicine by viewModel.recommendedMedicine.collectAsState(null)
    val isAnalyzing by viewModel.isAnalyzing.collectAsState(false)
    val userBalance by viewModel.userBalance.collectAsState(0f)

    val deviceRotation by viewModel.rotation.collectAsState(Pair(0f, 0f))
    val hazeState = remember { HazeState() }

    LaunchedEffect(Unit) {
        viewModel.startConsultation(userName, initialBalance)
        viewModel.connectBluetooth()
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.voiceManager.startListening()
            }
        }
    )

    DisposableEffect(Unit) {
        viewModel.voiceManager.onSpeechResult = { text ->
            viewModel.handleSpeechResult(text)
        }
        onDispose { /* ViewModel handles cleanup */ }
    }

    // AAA LAYER: Root Cinematic Container (Compatible with 8.1)
    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. SPATIAL REALITY LAYER (Tilt works on API 27)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = deviceRotation.first
                    rotationY = deviceRotation.second
                    cameraDistance = 15f * density
                }
        ) {
            if (isAppLoading) {
                ModernLoadingScreen()
            } else {
                // LAYER: Background with Haze (Haze has Legacy Fallback)
                Box(modifier = Modifier.fillMaxSize().haze(state = hazeState)) {
                    LivingBackground(rotation = deviceRotation)
                }
                
                TimerHeader(
                    seconds = sessionDuration,
                    onEndSession = onConsultationComplete,
                    onDebugNavigate = { viewModel.setPhase(it) }
                )

                Row(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .neonGlow(NeonPurple, 100.dp), 
                        contentAlignment = Alignment.Center
                    ) {
                        CrystalAvatar(isSpeaking = isAiSpeaking, amplitude = voiceAmplitude)
                    }
                    
                    Box(modifier = Modifier.weight(0.55f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = currentPhase,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(800)) + scaleIn(initialScale = 0.95f)) togetherWith
                                (fadeOut(animationSpec = tween(800)) + scaleOut(targetScale = 1.05f))
                            },
                            label = "ProfessionalPhaseSwitch"
                        ) { phase ->
                            when (phase) {
                                ConsultationPhase.WELCOME -> WelcomeWidget(userName = userName, hazeState = hazeState) { viewModel.setPhase(ConsultationPhase.SYMPTOMS) }
                                ConsultationPhase.SYMPTOMS -> ChatWidget(chatMessages, listState, false, isThinking) { 
                                     if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        scope.launch { 
                                            com.example.androidapp.data.audio.SoundManager.playTechScan()
                                            delay(200)
                                            viewModel.voiceManager.startListening() 
                                        }
                                    } else {
                                        permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                                ConsultationPhase.VITALS -> VitalsWidget(
                                    vitals = viewModel.vitals,
                                    selectedVital = selectedVital,
                                    bluetoothData = bluetoothVitals,
                                    isBluetoothConnected = isBluetoothConnected,
                                    onSelectVital = { viewModel.selectVital(it) },
                                    onMeasureComplete = { id, result -> viewModel.completeMeasurement(id, result) }
                                )
                                ConsultationPhase.REPORT -> ReportWidget(userName, detectedType, aiRecommendation, recommendedMedicine?.name ?: "", isAnalyzing) { viewModel.setPhase(ConsultationPhase.PHARMACY) }
                                ConsultationPhase.PHARMACY -> PharmacyWidget(
                                    userId = userId,
                                    medicine = recommendedMedicine,
                                    userBalance = userBalance,
                                    isBuyingInProgress = false,
                                    onPurchaseSuccess = { newBalance -> /* ViewModel should handle this, but for now: */ },
                                    onComplete = onConsultationComplete
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. UNIVERSAL GOD-TIER POST-PROCESS (Legacy Compatible Vignette)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.6f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.5f),
                        center = androidx.compose.ui.geometry.Offset.Unspecified
                    )
                )
        )
    }
}

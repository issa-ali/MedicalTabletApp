package com.example.androidapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.androidapp.ui.dialogs.ServerConfigurationDialog
import com.example.androidapp.ui.theme.DeepSpaceBlack
import com.example.androidapp.ui.theme.NeonCyan
import com.example.androidapp.ui.components.glassmorphic
import com.example.androidapp.ui.components.neonGlow

import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScanPatientScreen(
    onScanSuccess: (userId: String) -> Unit = {},
    onManualLoginClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var isScanning by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    // Sound Helper
    fun playSound(resIdName: String) {
        try {
            val resId = context.resources.getIdentifier(resIdName, "raw", context.packageName)
            if (resId != 0) {
                MediaPlayer.create(context, resId)?.apply {
                    setOnCompletionListener { release() }
                    start()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Play Hint on Entry
    LaunchedEffect(Unit) {
        playSound("scan_hint")
    }

    if (showSettingsDialog) {
        ServerConfigurationDialog(
            onDismiss = { showSettingsDialog = false },
            onSave = { 
                showSettingsDialog = false
                Toast.makeText(context, "Network Configured", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBlack)
    ) {
        // 1. Clean gradient background instead of galaxy video
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0F1419),
                            Color(0xFF0A0E12)
                        )
                    )
                )
        )
        
        // 2. Camera Preview (Overlay when scanning)
        if (isScanning && hasCameraPermission) {
             AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        
                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            val mediaImage = imageProxy.image
                            
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                                val scanner = BarcodeScanning.getClient()
                                
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty()) {
                                            val qrContent = barcodes.first().rawValue ?: ""
                                            // Ensure UI operations run on Main Thread
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                onScanSuccess(qrContent)
                                            }
                                            isScanning = false
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA, // Selfie Camera per requirement
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // 3. Center Scanner Button (Visible when NOT scanning)
        if (!isScanning) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                 ScannerButton(
                    onClick = {
                        playSound("scan_click")
                        if (hasCameraPermission) {
                            isScanning = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                 )
            }
            
            // Manual Login Option (Now here)
            Text(
                text = "Or login manually with email",
                color = NeonCyan.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .clickable { onManualLoginClick() }
                    .padding(8.dp)
            )
        } else {
             // Overlay for Scanning Mode - with improved contrast
             // Dark overlay outside the scan circle
             Box(
                 modifier = Modifier
                     .fillMaxSize()
                     .background(Color.Black.copy(alpha = 0.6f))
             )
             
             // Scan circle cutout area (Scaled for 10-inch Tablet)
             Box(
                 modifier = Modifier
                    .size(400.dp)
                    .background(Color.Transparent)
                    .border(4.dp, NeonCyan, CircleShape)
                    .neonGlow(NeonCyan, 40.dp)
                    .align(Alignment.Center)
             )
             
             // Clear, high-contrast instruction text
             Column(
                 modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .glassmorphic(backgroundColor = Color(0xFF0F172A).copy(alpha = 0.9f))
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                 horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
             ) {
                 Text(
                     text = "Welcome. Please scan your QR code",
                     color = Color.White,
                     style = MaterialTheme.typography.headlineSmall,
                     fontWeight = FontWeight.Bold
                 )
                 Spacer(modifier = Modifier.height(4.dp))
                 Text(
                     text = "Position the QR code from the patient's phone inside the circle",
                     color = Color.White.copy(alpha = 0.7f),
                     fontSize = 14.sp,
                     textAlign = TextAlign.Center
                 )
             }
        }
        
        // 4. Settings Icon
        IconButton(
            onClick = { 
                playSound("scan_click")
                showSettingsDialog = true 
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp)
                .size(56.dp)
                .glassmorphic(cornerRadius = 28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = NeonCyan.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }
        

    }
}

@Composable
fun ScannerButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_anim")
    
    // Scale Animation for pulsing
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Glow pulsing
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(300.dp)
            .clickable { onClick() }
    ) {
        // Outer Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = glowAlpha * 0.5f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        
        // Glassy Circle Container
        Box(
            modifier = Modifier
                .size(260.dp)
                .glassmorphic(borderColor = NeonCyan.copy(alpha = 0.5f))
                .neonGlow(NeonCyan, 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR",
                    tint = NeonCyan,
                    modifier = Modifier.size(80.dp)
                )
                
                Text(
                    text = "SCAN PATIENT QR",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Rotating Scanning Ring
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing)
            ),
            label = "ring_rotation"
        )
        
        androidx.compose.foundation.Canvas(modifier = Modifier.size(320.dp)) {
             withTransform({ rotate(rotation) }) {
                  drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, NeonCyan, Color.Transparent)
                    ),
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                  )
             }
        }
    }
}

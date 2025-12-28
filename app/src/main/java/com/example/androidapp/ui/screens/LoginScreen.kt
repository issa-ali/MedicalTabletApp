package com.example.androidapp.ui.screens

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.androidapp.ui.components.glassmorphic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.R
import com.example.androidapp.data.api.LoginRequest
import com.example.androidapp.data.api.NetworkModule
import com.example.androidapp.data.prefs.PreferencesManager
import com.example.androidapp.ui.components.LivingBackground
import com.example.androidapp.ui.dialogs.ServerConfigurationDialog
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.theme.NeonCyan
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.example.androidapp.ui.components.neonGlow
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: (com.example.androidapp.data.api.UserDto) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    
    // Initialize network with saved prefs on startup
    LaunchedEffect(Unit) {
        NetworkModule.initialize(prefs.getBaseUrl())
    }
    
    // State
    var showSettingsDialog by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Sound Effect (Best Effort)
    DisposableEffect(Unit) {
        var mediaPlayer: MediaPlayer? = null
        try {
             val resId = context.resources.getIdentifier("login_sfx", "raw", context.packageName)
             if (resId != 0) {
                 mediaPlayer = MediaPlayer.create(context, resId)
                 mediaPlayer?.start()
             }
        } catch (e: Exception) { e.printStackTrace() }
        onDispose { mediaPlayer?.release() }
    }

    // Login Function
    fun performLogin() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Please enter username and password"
            return
        }
        
        isLoading = true
        errorMessage = null
        
        scope.launch {
            try {
                val response = NetworkModule.api.login(
                    LoginRequest(email = username, password = password)
                )
                
                isLoading = false
                Toast.makeText(context, "Welcome ${response.user.fullName}", Toast.LENGTH_LONG).show()
                onLoginSuccess(response.user)
                
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
                errorMessage = e.localizedMessage ?: "Login failed"
            }
        }
    }
    
    // Settings Dialog
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
        modifier = modifier
            .fillMaxSize()
            .haze(state = hazeState),
        contentAlignment = Alignment.Center
    ) {
        // 1. Animated Background
        LivingBackground()
        
        // 2. Navigation & Settings
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // Settings Icon (Top Right)
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(56.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = NeonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: Cinematic Branding
            Column(
                modifier = Modifier.weight(0.45f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.login_welcome),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Light
                    )
                )
                Spacer(modifier = Modifier.height(48.dp))
                
                // System Status Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(NeonCyan, CircleShape)
                            .neonGlow(NeonCyan, 8.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "CAMPUS NETWORK LINKED ⚡",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(0.6f),
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(64.dp))

                // Uni Branding Footer
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "GRADUATION PROJECT 2025",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.4f),
                        letterSpacing = 3.sp
                    )
                    Text(
                        "UNIVERSITY MEDICAL CENTER",
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonCyan.copy(0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // RIGHT SIDE: Auth Form
            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .padding(start = 64.dp)
                    .glassmorphic(borderColor = NeonCyan.copy(alpha = 0.4f))
                    .padding(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Error Message
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Inputs
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Email", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = NeonCyan,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = Color.White)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = NeonCyan,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Login Button (Modernized)
                    Button(
                        onClick = { performLogin() },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .neonGlow(NeonCyan)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "SIGN IN TO CLINIC",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                            )
                        }
                    }

                    // STUDENT ADDITION: NFC MOCKUP
                    Text("— OR —", color = Color.White.copy(0.4f), style = MaterialTheme.typography.labelSmall)

                    OutlinedButton(
                        onClick = { /* Mock NFC Action */ },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        border = BorderStroke(1.dp, NeonCyan.copy(0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.CreditCard, null, tint = NeonCyan)
                        Spacer(Modifier.width(12.dp))
                        Text("TAP STUDENT ID (NFC)", color = NeonCyan)
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 1200,
    heightDp = 800
)
@Composable
fun LoginScreenPreview() {
    AndroidAppTheme {
        LoginScreen()
    }
}

package com.example.androidapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.androidapp.data.api.NetworkModule
import com.example.androidapp.data.api.VerifyPatientRequest
import com.example.androidapp.ui.screens.ConsultationScreen
import com.example.androidapp.ui.screens.LoginScreen
import com.example.androidapp.ui.screens.ScanPatientScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    // Loading state for verification
    var isVerifying by remember { mutableStateOf(false) }

    NavHost(
        navController = navController, 
        startDestination = "scan_patient", // QR Scan is now the entry point (Patient Friendly)
        enterTransition = { fadeIn(animationSpec = tween(500)) },
        exitTransition = { fadeOut(animationSpec = tween(500)) }
    ) {
        // 1. Scan Patient Screen (Primary Entry)
        composable("scan_patient") {
            ScanPatientScreen(
                onScanSuccess = { userId ->
                    // Verify patient logic
                    scope.launch {
                        isVerifying = true
                        try {
                            // Verify with backend
                            val response = NetworkModule.api.verifyPatient(com.example.androidapp.data.api.VerifyPatientRequest(userId))
                            if (response.fullName != null) {
                                val safeName = response.fullName.replace(" ", "_")
                                val balance = response.balance ?: 0.0f
                                navController.navigate("welcome/$safeName/${response.userId}/$balance") {
                                    popUpTo("scan_patient") { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Fallback for demo
                            navController.navigate("welcome/Guest_Patient/guest/5000.0")
                        } finally {
                            isVerifying = false
                        }
                    }
                },
                onManualLoginClick = {
                    navController.navigate("login")
                }
            )
        }

        // 2. Login Screen (Secondary Option)
        composable("login") {
            LoginScreen(
                onLoginSuccess = { user ->
                    val safeName = (user.fullName ?: "Patient").replace(" ", "_")
                    val balance = user.balance ?: 0.0f
                    navController.navigate("welcome/$safeName/${user.id}/$balance") {
                         popUpTo("scan_patient") { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 3. AI Consultation & Welcome (Consolidated)
        composable(
            route = "welcome/{userName}/{userId}/{balance}"
        ) { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName")?.replace("_", " ") ?: "Patient"
            val userId = backStackEntry.arguments?.getString("userId") ?: "guest"
            val balance = backStackEntry.arguments?.getString("balance")?.toFloatOrNull() ?: 0.0f
            
             ConsultationScreen(
                 userName = userName,
                 userId = userId,
                 initialBalance = balance,
                 onConsultationComplete = {
                    navController.navigate("scan_patient") {
                        popUpTo("scan_patient") { inclusive = true }
                    }
                 }
             )
        }

        // 4. Consultation (Direct Route - Fallback)
        composable(
            route = "consultation"
        ) {
             ConsultationScreen(
                 userName = "Guest",
                 userId = "guest",
                 initialBalance = 5000.0f,
                 onConsultationComplete = {
                    navController.navigate("scan_patient") {
                        popUpTo("scan_patient") { inclusive = true }
                    }
                 }
             )
        }
    }
}

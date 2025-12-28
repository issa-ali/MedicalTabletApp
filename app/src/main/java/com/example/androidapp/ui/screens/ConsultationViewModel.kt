package com.example.androidapp.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.ai.OllamaRepository
import com.example.androidapp.data.api.NetworkModule
import com.example.androidapp.data.audio.VoiceManager
import com.example.androidapp.data.motion.MotionManager
import com.example.androidapp.data.bluetooth.VitalsData
import com.example.androidapp.data.models.ChatMessage
import com.example.androidapp.data.models.Medicine
import com.example.androidapp.data.models.VitalSign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Compress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConsultationViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repositories & Managers
    private val ollamaRepository = OllamaRepository()
    val voiceManager = VoiceManager(application)
    val motionManager = MotionManager(application)
    private val vitalsViewModel = VitalsViewModel(application) // Reusing existing VM logic for Bluetooth
    
    // UI State
    val rotation = motionManager.rotation
    val voiceAmplitude = voiceManager.voiceAmplitude
    private val _currentPhase = MutableStateFlow(ConsultationPhase.WELCOME)
    val currentPhase: StateFlow<ConsultationPhase> = _currentPhase.asStateFlow()
    
    private val _isAppLoading = MutableStateFlow(true)
    val isAppLoading: StateFlow<Boolean> = _isAppLoading.asStateFlow()
    
    private val _sessionDurationSeconds = MutableStateFlow(0L)
    val sessionDurationSeconds: StateFlow<Long> = _sessionDurationSeconds.asStateFlow()

    // AI & Consultation Data
    val chatMessages = mutableStateListOf<ChatMessage>()
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()
    
    private val _isAiSpeaking = MutableStateFlow(false)
    val isAiSpeaking: StateFlow<Boolean> = _isAiSpeaking.asStateFlow()

    // Vitals Data
    val vitals = mutableStateListOf<VitalSign>()
    val bluetoothVitals: StateFlow<VitalsData> = vitalsViewModel.vitals
    val isBluetoothConnected: StateFlow<Boolean> = vitalsViewModel.isConnected
    val lastBluetoothMessage: StateFlow<String> = vitalsViewModel.lastMessage

    private val _selectedVital = MutableStateFlow<VitalSign?>(null)
    val selectedVital: StateFlow<VitalSign?> = _selectedVital.asStateFlow()

    // Report & Pharmacy
    private val _aiRecommendation = MutableStateFlow("Generating recommendation...")
    val aiRecommendation: StateFlow<String> = _aiRecommendation.asStateFlow()
    
    private val _detectedType = MutableStateFlow("Analyzing...")
    val detectedType: StateFlow<String> = _detectedType.asStateFlow()
    
    private val _recommendedMedicine = MutableStateFlow<Medicine?>(null)
    val recommendedMedicine: StateFlow<Medicine?> = _recommendedMedicine.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private val _userBalance = MutableStateFlow(0f)
    val userBalance: StateFlow<Float> = _userBalance.asStateFlow()

    init {
        setupVitals()
        voiceManager.initialize()
    }

    private fun setupVitals() {
        vitals.addAll(listOf(
            VitalSign("hr", "Heart Rate & SpO2", Icons.Filled.Favorite, "bpm/%", "Please place your index finger gently on the heart rate sensor."),
            VitalSign("bp", "Blood Pressure", Icons.Filled.Compress, "mmHg", "Wrap the cuff around your upper arm, sit still, and breathe normally."),
            VitalSign("temp", "Body Temperature", Icons.Filled.Thermostat, "°C", "Point the sensor at your forehead from a distance of 3-5 cm."),
            VitalSign("ecg", "Stable ECG", Icons.Filled.MonitorHeart, "ms", "Hold the metal sensor pads firmly with both hands.")
        ))
    }

    fun startConsultation(userName: String, initialBalance: Float) {
        _userBalance.value = initialBalance
        motionManager.start()
        viewModelScope.launch {
            delay(2000) // Initial boot simulation
            _isAppLoading.value = false
            // Timer starts when 'Start Consultation' is clicked (Phase change)
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                _sessionDurationSeconds.value = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        }
    }

    fun setPhase(phase: ConsultationPhase) {
        _currentPhase.value = phase
        if (phase == ConsultationPhase.SYMPTOMS && _sessionDurationSeconds.value == 0L) {
            startTimer()
        }
        if (phase == ConsultationPhase.REPORT) runAnalysis()
    }

    fun handleSpeechResult(text: String) {
        if (_currentPhase.value == ConsultationPhase.SYMPTOMS) {
            chatMessages.add(ChatMessage("user", text))
            processAiResponse(text)
        }
    }

    private fun processAiResponse(text: String) {
        viewModelScope.launch {
            try {
                _isThinking.value = true
                val rawResponse = ollamaRepository.generateResponse(text)
                
                // Parse Hybrid JSON
                val jsonObj = try {
                    org.json.JSONObject(rawResponse)
                } catch (e: Exception) {
                    // Fallback if AI fails to output valid JSON
                    org.json.JSONObject().apply {
                        put("english", rawResponse)
                        put("egyptian_arabic", rawResponse)
                    }
                }

                val englishText = jsonObj.optString("english", "")
                val arabicVoice = jsonObj.optString("egyptian_arabic", englishText)

                // Robust check for completion (case-insensitive + handles variations)
                val isComplete = englishText.contains("[CONSULTATION_COMPLETE]", ignoreCase = true) || 
                                 englishText.contains("[COMPLETE]", ignoreCase = true)
                
                val uiText = englishText.replace(Regex("\\[(CONSULTATION_)?COMPLETE\\]", RegexOption.IGNORE_CASE), "").trim()

                voiceManager.speak(
                    text = arabicVoice,
                    waitForCompletion = true,
                    onTextReady = {
                        _isThinking.value = false
                        chatMessages.add(ChatMessage("ai", uiText))
                    },
                    onAudioStart = { _isAiSpeaking.value = true }
                )
                _isAiSpeaking.value = false

                if (isComplete) {
                    setPhase(ConsultationPhase.VITALS)
                    // Egyptian: "تمام يا بطل. دلوقتي هنبدأ نقيس العلامات الحيوية."
                    voiceManager.speak("تمام يا بطل. يلا بينا نقيس العلامات الحيوية دلوقتى. ✨", waitForCompletion = true, onAudioStart = { _isAiSpeaking.value = true })
                    _isAiSpeaking.value = false
                }
            } catch (e: Exception) {
                _isThinking.value = false
                chatMessages.add(ChatMessage("ai", "Error: ${e.localizedMessage}"))
            } finally {
                _isThinking.value = false
                _isAiSpeaking.value = false
            }
        }
    }

    private fun runAnalysis() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val products = NetworkModule.api.getProducts()
                val rawAnalysis = ollamaRepository.generateFinalAnalysis(chatMessages, vitals, products)
                
                val jsonObj = try {
                    org.json.JSONObject(rawAnalysis)
                } catch (e: Exception) {
                    // Fallback
                    org.json.JSONObject().apply {
                        val inner = org.json.JSONObject().apply {
                            put("detected", "Complete")
                            put("recommendation", "Review required")
                            put("medicine", "")
                        }
                        put("english", inner)
                        put("egyptian_arabic", inner)
                    }
                }

                val english = jsonObj.getJSONObject("english")
                val arabic = jsonObj.getJSONObject("egyptian_arabic")

                _detectedType.value = english.getString("detected")
                _aiRecommendation.value = english.getString("recommendation")

                val medName = english.getString("medicine")
                val dbMed = products.find { it.name.contains(medName, true) }
                _recommendedMedicine.value = dbMed?.let { 
                    Medicine(it.name, "${it.price} EGP", it.description, it.id)
                }

                // Egyptian Voice Summary
                val speakText = "التحليل خلص. أنا شايفة إنك غالباً عندك ${arabic.getString("detected")}. التوصية بتاعتي هي: ${arabic.getString("recommendation")}."
                voiceManager.speak(speakText, waitForCompletion = true, onAudioStart = { _isAiSpeaking.value = true })
            } catch (e: Exception) {
                Log.e("VM", "Analysis error", e)
            } finally {
                _isAnalyzing.value = false
                _isAiSpeaking.value = false
            }
        }
    }

    fun selectVital(vital: VitalSign) {
        _selectedVital.value = vital
        viewModelScope.launch {
            when (vital.id) {
                "hr" -> vitalsViewModel.startHeartRate()
                "temp" -> vitalsViewModel.startTemp()
                "bp" -> vitalsViewModel.startBP()
                "ecg" -> vitalsViewModel.startECG()
            }
        }
    }

    fun completeMeasurement(id: String, value: String) {
        _selectedVital.value = null
        val index = vitals.indexOfFirst { it.id == id }
        if (index != -1) {
            vitals[index] = vitals[index].copy(value = value)
            vitalsViewModel.stopSensors()
            com.example.androidapp.data.audio.SoundManager.playSuccess()
            
            // Auto-advance if all vitals are done? Let's check.
            if (vitals.all { it.value != null }) {
                viewModelScope.launch {
                    voiceManager.speak("تمام جداً. كل القراءات كويسة. أنا دلوقتى بطلع التقرير النهائى.", waitForCompletion = true)
                }
                setPhase(ConsultationPhase.REPORT)
            }
        }
    }

    fun connectBluetooth() {
        viewModelScope.launch {
            vitalsViewModel.connect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.cleanup()
        motionManager.stop()
    }
}

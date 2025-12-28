package com.example.androidapp.data.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import android.media.audiofx.Visualizer
import kotlinx.coroutines.*
import android.speech.tts.UtteranceProgressListener

class VoiceManager(private val context: Context) {

    // --- Speech to Text (Native) ---
    private var speechRecognizer: SpeechRecognizer? = null
    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onSpeechPartial: ((String) -> Unit)? = null

    // --- Text to Speech (ElevenLabs + Web/Local Fallback) ---
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val audioCache = java.util.concurrent.ConcurrentHashMap<String, File>()
    private var mediaPlayer: MediaPlayer? = null
    private var visualizer: Visualizer? = null
    private val _voiceAmplitude = MutableStateFlow(0f)
    val voiceAmplitude = _voiceAmplitude.asStateFlow()

    fun initialize() {
        // Native Speech Recognition Setup
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) return
                        val message = when(error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Device busy"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                            else -> "Speech Error: $error"
                        }
                        onSpeechError?.invoke(message)
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) onSpeechResult?.invoke(matches[0])
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) onSpeechPartial?.invoke(matches[0])
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }

        // Local TTS Setup (Fallback) - Use Arabic (Egyptian if possible)
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ar", "EG"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale("ar") // Fallback to general Arabic
                }
                isTtsInitialized = true
            }
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            onSpeechError?.invoke("Recognition not available")
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG") // FORCE EGYPTIAN ARABIC
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() = speechRecognizer?.stopListening()

    suspend fun speak(
        text: String, 
        waitForCompletion: Boolean = false,
        onTextReady: () -> Unit = {},
        onAudioStart: () -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileToPlay = if (audioCache.containsKey(text)) {
                audioCache[text]!!
            } else {
                val response = try {
                    ElevenLabsService.api.generateAudio(
                        voiceId = ElevenLabsService.VOICE_ID_ANTONI,
                        apiKey = ElevenLabsService.API_KEY,
                        request = ElevenLabsRequest(text = text)
                    )
                } catch (e: Exception) { null }

                if (response?.isSuccessful == true && response.body() != null) {
                    val tempFile = File.createTempFile("tts_", ".mp3", context.cacheDir)
                    val outputStream = FileOutputStream(tempFile)
                    response.body()!!.byteStream().use { it.copyTo(outputStream) }
                    tempFile
                } else {
                    Log.w("VoiceManager", "ElevenLabs failed, using local TTS fallback")
                    withContext(Dispatchers.Main) {
                        onTextReady()
                        onAudioStart()
                        speakLocal(text, waitForCompletion)
                    }
                    return@withContext true
                }
            }

            withContext(Dispatchers.Main) {
                onTextReady()
                kotlinx.coroutines.delay(50)
                onAudioStart()
                playFile(fileToPlay, waitForCompletion)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("VoiceManager", "Speak Error", e)
            return@withContext false
        }
    }

    private suspend fun speakLocal(text: String, wait: Boolean) {
        if (!isTtsInitialized) return
        if (wait) {
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { if (cont.isActive) cont.resume(Unit) {} }
                    override fun onError(id: String?) { if (cont.isActive) cont.resume(Unit) {} }
                })
                val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "id") }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "id")
                cont.invokeOnCancellation { tts?.stop() }
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun getPlayer(): MediaPlayer {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        } else {
            mediaPlayer?.reset()
            stopVisualizer()
        }
        return mediaPlayer!!
    }

    private fun startVisualizer(sessionId: Int) {
        try {
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        waveform?.let {
                            var sum = 0f
                            // Titan Optimization: sample every 4th byte for amplitude (75% faster calculation)
                            for (i in it.indices step 4) {
                                val amplitude = (it[i].toInt() + 128).toFloat()
                                sum += Math.abs(amplitude - 128f)
                            }
                            _voiceAmplitude.value = (sum / (it.size / 4)) / 128f
                        }
                    }
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                enabled = true
            }
        } catch (e: Exception) { Log.e("VoiceManager", "Visualizer error", e) }
    }

    private fun stopVisualizer() {
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
        _voiceAmplitude.value = 0f
    }

    private suspend fun playFile(file: File, wait: Boolean) {
        if (wait) {
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                try {
                    val player = getPlayer()
                    player.setDataSource(file.absolutePath)
                    player.prepare()
                    startVisualizer(player.audioSessionId)
                    player.start()
                    player.setOnCompletionListener {
                        stopVisualizer()
                        file.delete()
                        if (cont.isActive) cont.resume(Unit) {}
                    }
                    cont.invokeOnCancellation { try { player.stop() } catch(e: Exception) {} }
                } catch (e: Exception) { if (cont.isActive) cont.resume(Unit) {} }
            }
        } else {
            try {
                val player = getPlayer()
                player.setDataSource(file.absolutePath)
                player.prepare()
                startVisualizer(player.audioSessionId)
                player.start()
                player.setOnCompletionListener { 
                    stopVisualizer()
                    file.delete() 
                }
            } catch (e: Exception) { Log.e("VoiceManager", "Play Error", e) }
        }
    }

    fun cleanup() {
        speechRecognizer?.destroy()
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.stop()
        tts?.shutdown()
    }
}

package com.example.androidapp.data.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoundManager {
    
    private var toneGenerator: ToneGenerator? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 80)
        } catch (e: Exception) {
            // Log error
        }
    }

    fun playClick(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.playSoundEffect(AudioManager.FX_KEY_CLICK)
    }

    fun playTechScan() {
        // AAA: Computerized Neural Chatter Sequence
        scope.launch {
            repeat(3) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                delay(80)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 40)
                delay(60)
            }
        }
    }

    fun playSuccess() {
        // AAA: Rising Melodic Acknowledge
        scope.launch {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
            delay(150)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        }
    }

    fun playAlert() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 400)
    }

    fun cleanup() {
        toneGenerator?.release()
        toneGenerator = null
    }
}

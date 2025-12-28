package com.example.androidapp.data.audio

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

// ElevenLabs Data Models
@Serializable
data class ElevenLabsRequest(
    val text: String,
    val model_id: String = "eleven_monolingual_v1",
    val voice_settings: VoiceSettings = VoiceSettings()
)

@Serializable
data class VoiceSettings(
    val stability: Float = 0.5f,
    val similarity_boost: Float = 0.75f
)

interface ElevenLabsApi {
    @Streaming
    @POST("v1/text-to-speech/{voiceId}")
    suspend fun generateAudio(
        @Path("voiceId") voiceId: String,
        @Header("xi-api-key") apiKey: String,
        @Body request: ElevenLabsRequest
    ): Response<ResponseBody>
}

object ElevenLabsService {
    private const val BASE_URL = "https://api.elevenlabs.io/"
    
    // Provided by user
    const val API_KEY = "sk_2caa8f8d344b2e855f620cee69c58f05c37e237d8d8811db"
    
    // Voice ID: "Rachel" (Professional American Female)
    const val VOICE_ID_RACHEL = "21m00Tcm4TlvDq8ikWAM" 
    
    // Voice ID: "Antoni" (Calm American Male) - Good for doctor
    const val VOICE_ID_ANTONI = "ErXwobaYiN019PkySvjV"

    private val json = Json { ignoreUnknownKeys = true }

    val api: ElevenLabsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(ElevenLabsApi::class.java)
    }
}

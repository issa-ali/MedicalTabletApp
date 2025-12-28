package com.example.androidapp.data.ai

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

@Serializable
data class OllamaMessage(
    val role: String,
    val content: String
)

@Serializable
data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false
)

@Serializable
data class OllamaChatResponse(
    val model: String,
    @SerialName("created_at") val createdAt: String,
    val message: OllamaMessage? = null, // Can be null in "thinking" chunks
    val done: Boolean
)

interface OllamaApi {
    @POST("api/chat")
    @Streaming // Important for reading large streams
    suspend fun chat(@Body request: OllamaChatRequest): Response<ResponseBody>
}

object OllamaService {
    // CORRECTED: User provided correct host
    private const val BASE_URL = "https://ollama.com/"
    
    // API KEY set by user
    private const val API_KEY = "4a53bfadf5b44ca88cef60206d1e5e8a.Sax7PfLtN86qcuB3NuTVUaUN"

    private val json = Json { ignoreUnknownKeys = true }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("User-Agent", "MedicalTabletApp/1.0")
                .build()
            chain.proceed(request)
        }
        .build()

    val api: OllamaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(OllamaApi::class.java)
    }
}

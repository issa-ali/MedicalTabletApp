package com.example.androidapp.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Models
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDto,
    val error: String? = null
)

@Serializable
data class UserDto(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String? = null,
    val balance: Float? = null
)

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val description: String,
    val price: Float,
    val imageUrl: String,
    val category: String
)

// Patient Verification (for QR code scanning)
@Serializable
data class VerifyPatientRequest(
    val userId: String
)

@Serializable
data class VerifyPatientResponse(
    val userId: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val balance: Float? = null,
    val error: String? = null
)

@Serializable
data class PurchaseRequest(
    val userId: String,
    val productId: String,
    val quantity: Int = 1
)

@Serializable
data class PurchaseResponse(
    val success: Boolean,
    val message: String,
    val newBalance: Float? = null,
    val orderId: String? = null,
    val error: String? = null
)

interface MedicalApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @POST("patient/verify")
    suspend fun verifyPatient(@Body request: VerifyPatientRequest): VerifyPatientResponse

    @retrofit2.http.GET("store/products")
    suspend fun getProducts(): List<ProductDto>

    @POST("tablet/purchase")
    suspend fun purchase(@Body request: PurchaseRequest): PurchaseResponse
}

object NetworkModule {
    private var currentBaseUrl = "http://192.168.1.102:3000/api/"
    private var retrofit: Retrofit? = null
    private var _api: MedicalApi? = null

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun initialize(url: String) {
        currentBaseUrl = url
        rebuildRetrofit()
    }

    private fun rebuildRetrofit() {
        retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        _api = null // Force lazy reload
    }

    val api: MedicalApi
        get() {
            if (_api == null) {
                if (retrofit == null) {
                    rebuildRetrofit()
                }
                _api = retrofit!!.create(MedicalApi::class.java)
            }
            return _api!!
        }

    fun updateBaseUrl(newUrl: String) {
        if (currentBaseUrl != newUrl) {
            currentBaseUrl = newUrl
            rebuildRetrofit()
        }
    }
}

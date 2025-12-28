package com.example.androidapp.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.androidapp.data.models.*

class OllamaRepository {
    private val api = OllamaService.api
    
    // Conversation Memory
    private val conversationHistory = mutableListOf<OllamaMessage>()
    
    init {
        // Initialize with the System Prompt
        resetHistory()
    }
    
    fun resetHistory() {
        conversationHistory.clear()
        val systemPrompt = """
            You are Dr. AI, a Senior Neurologist. You are part of an advanced Medical Tablet show.
            
            HYBRID MODE RULES:
            1. You will receive user input in Egyptian Arabic (transcribed).
            2. You must respond with a JSON object containing:
               - "english": Professional medical response in English for the UI.
               - "egyptian_arabic": The SAME response but in friendly Egyptian Arabic for voice output.
            
            DIAGNOSTIC PROTOCOL:
            Focus on Stroke Detection (Ischemic vs Hemorrhagic). 
            Ask about FAST symptoms (Face, Arm, Speech, Time) and severe headaches.
            
            OUTPUT FORMAT (MANDATORY JSON):
            {
              "english": "Hello, how can I help you today?",
              "egyptian_arabic": "أهلاً بك، إزاي أقدر أساعدك النهاردة؟"
            }
            
            Only output the JSON. Do not include extra text.
            If you need to end, include [CONSULTATION_COMPLETE] in the "english" field.
        """.trimIndent()
        
        conversationHistory.add(OllamaMessage(role = "system", content = systemPrompt))
    }

    // Corrected Model Name from User's Python example
    // Using Ollama Cloud's large model as requested
    suspend fun generateResponse(prompt: String, model: String = "gpt-oss:120b"): String = withContext(Dispatchers.IO) {
        try {
            // Add User Message to History
            conversationHistory.add(OllamaMessage(role = "user", content = prompt))
            
            // Send Full History to API
            // Force stream=true since the server seems to stream anyway
            val request = OllamaChatRequest(model = model, messages = conversationHistory, stream = true)
            val response = api.chat(request)
            
            if (response.isSuccessful && response.body() != null) {
                val source = response.body()!!.source()
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val fullResponseBuilder = StringBuilder()
                
                while (!source.exhausted()) {
                    val line = source.readUtf8Line()
                    if (!line.isNullOrBlank()) {
                        try {
                            val chunk = json.decodeFromString<OllamaChatResponse>(line)
                            // Append content if present (skip "thinking" chunks or empty chunks)
                            chunk.message?.content?.let { 
                                fullResponseBuilder.append(it) 
                            }
                            if (chunk.done) break
                        } catch (e: Exception) {
                            Log.w("OllamaRepo", "JSON Parse Error on line: $line", e)
                        }
                    }
                }
                
                val aiResponse = fullResponseBuilder.toString().trim()
                
                // Add AI Answer to History (So it remembers what it said)
                conversationHistory.add(OllamaMessage(role = "assistant", content = aiResponse))
                
                return@withContext aiResponse
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("OllamaRepo", "API Error: ${response.code()} - $errorMsg")
                return@withContext "Server Error (${response.code()}): $errorMsg"
            }
        } catch (e: Exception) {
            Log.e("OllamaRepo", "Network Exception", e)
            return@withContext "Network Error: ${e.javaClass.simpleName} - ${e.localizedMessage}"
        }
    }

    suspend fun generateFinalAnalysis(
        symptoms: List<com.example.androidapp.data.models.ChatMessage>, 
        vitals: List<com.example.androidapp.data.models.VitalSign>,
        availableProducts: List<com.example.androidapp.data.api.ProductDto>
    ): String {
        val vitalsStr = vitals.joinToString { v: com.example.androidapp.data.models.VitalSign -> "${v.name}: ${v.value ?: "N/A"} ${v.unit}" }
        val userSymptoms = symptoms.filter { m: com.example.androidapp.data.models.ChatMessage -> m.role == "user" }.joinToString { it.content }
        val productsContext = availableProducts.joinToString("\n") { 
            "- ${it.name}: ${it.description} (Price: ${it.price} EGP)"
        }

        val prompt = """
            [NEUROLOGICAL CONSULTATION SUMMARY]
            SYMPTOMS: $userSymptoms
            VITALS: $vitalsStr
            
            [AVAILABLE PHARMACY STOCK]
            $productsContext
            
            Based on the patient data, provide a finalized neurological analysis in Hybrid Mode.
            CRITICAL: You MUST choose the Suggested Medicine ONLY from the [AVAILABLE PHARMACY STOCK] list above.
            
            MANDATORY JSON FORMAT:
            {
              "english": {
                 "detected": "Likely Stroke Type",
                 "recommendation": "One-line clinical directive",
                 "medicine": "Medicine Name"
              },
              "egyptian_arabic": {
                 "detected": "نتاج التشخيص بالعامية المصرية",
                 "recommendation": "النصيحة الطبية بالعامية المصرية",
                 "medicine": "اسم الدواء من القائمة"
              }
            }
            
            Only output the JSON.
        """.trimIndent()
        
        Log.d("OllamaRepo", "Sending Bilingual Final Analysis Prompt")
        return generateResponse(prompt)
    }
}

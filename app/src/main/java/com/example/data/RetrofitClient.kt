package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// Moshi data classes for Gemini REST API
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @field:Json(name = "contents") val contents: List<ContentBlock>,
    @field:Json(name = "generationConfig") val generationConfig: GenerationConfigBlock? = null
)

@JsonClass(generateAdapter = true)
data class ContentBlock(
    @field:Json(name = "parts") val parts: List<PartBlock>
)

@JsonClass(generateAdapter = true)
data class PartBlock(
    @field:Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfigBlock(
    @field:Json(name = "temperature") val temperature: Float = 0.2f
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @field:Json(name = "candidates") val candidates: List<CandidateBlock>?
)

@JsonClass(generateAdapter = true)
data class CandidateBlock(
    @field:Json(name = "content") val content: ContentBlock?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun generateWithGemini(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("API Key is missing or default placeholder.")
        }

        val request = GeminiRequest(
            contents = listOf(
                ContentBlock(
                    parts = listOf(PartBlock(text = prompt))
                )
            ),
            generationConfig = GenerationConfigBlock(temperature = 0.3f)
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No text response found from Gemini."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

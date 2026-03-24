package com.example.appcleanhouse.api

import com.example.appcleanhouse.BuildConfig
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ── Gemini REST API DTOs ────────────────────────────────────────────
data class GeminiRequest(val contents: List<GeminiContent>)
data class GeminiContent(val role: String, val parts: List<GeminiPart>)
data class GeminiPart(val text: String)

data class GeminiResponse(val candidates: List<GeminiCandidate>?)
data class GeminiCandidate(val content: GeminiContent?)

// ── Retrofit interface ──────────────────────────────────────────────
interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body body: GeminiRequest
    ): Call<GeminiResponse>
}

object GeminiApi {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    const val DEFAULT_MODEL = "gemini-2.0-flash"
    val MODEL_CANDIDATES = listOf(
        DEFAULT_MODEL,
        "gemini-1.5-flash",
        "gemini-1.5-flash-8b",
        "gemini-2.0-flash-lite"
    )
    val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}

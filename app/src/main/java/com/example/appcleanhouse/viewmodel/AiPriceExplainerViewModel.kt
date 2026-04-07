package com.example.appcleanhouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcleanhouse.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ViewModel giải thích chi tiết giá dịch vụ bằng AI.
 * Phân tích cấu trúc giá, so sánh thị trường, giúp khách an tâm.
 */
class AiPriceExplainerViewModel : ViewModel() {

    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val modelName = "gemini-2.5-flash"
    private val apiUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ─── States ─────────────────────────────────────────────────────
    private val _explanation = MutableStateFlow<String?>(null)
    val explanation: StateFlow<String?> = _explanation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ─── Giải thích giá ─────────────────────────────────────────────
    fun explainPrice(
        serviceName: String,
        pricePerHour: Int,
        hours: Int,
        tax: Double,
        totalPrice: Double
    ) {
        if (apiKey.isBlank()) {
            _error.value = "Chưa cấu hình API key"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    callGemini(serviceName, pricePerHour, hours, tax, totalPrice)
                }
                _explanation.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Lỗi không xác định"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun callGemini(
        serviceName: String,
        pricePerHour: Int,
        hours: Int,
        tax: Double,
        totalPrice: Double
    ): String {
        val systemInstruction = """
            Bạn là chuyên gia tư vấn giá dịch vụ vệ sinh nhà cửa.
            Nhiệm vụ: Giải thích chi tiết cấu trúc giá cho khách hàng một cách chuyên nghiệp và thuyết phục.
            
            QUY TẮC:
            - Trả lời bằng tiếng Việt
            - Sử dụng emoji phù hợp
            - Chia thành 4 phần rõ ràng với tiêu đề in đậm:
              1. **💰 Chi tiết giá** – Phân tích từng thành phần giá
              2. **📊 So sánh thị trường** – So với trung bình thị trường dịch vụ dọn dẹp
              3. **✅ Giá trị bạn nhận được** – Liệt kê những gì bao gồm trong giá
              4. **💡 Mẹo tiết kiệm** – 1-2 lời khuyên để tiết kiệm chi phí
            - Tổng độ dài khoảng 150-200 từ
            - Giọng văn thân thiện, chuyên nghiệp
        """.trimIndent()

        val subtotal = pricePerHour * hours
        val userPrompt = """
            Hãy giải thích chi tiết giá cho khách hàng:
            
            Dịch vụ: $serviceName
            Đơn giá: $pricePerHour$/giờ
            Số giờ: $hours giờ
            Phí dịch vụ: $subtotal$
            Thuế/Phí phát sinh: $tax$
            TỔNG CỘNG: $totalPrice$
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            }))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 1024)
            })
        }

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Phản hồi rỗng từ server")

        if (!response.isSuccessful) {
            val errorMessage = try {
                JSONObject(body).optJSONObject("error")?.optString("message") ?: body
            } catch (_: Exception) { body }
            throw Exception(errorMessage)
        }

        val json = JSONObject(body)
        return json.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.trim() ?: throw Exception("Không nhận được phản hồi")
    }
}

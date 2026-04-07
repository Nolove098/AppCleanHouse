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
 * ViewModel tạo checklist dọn dẹp cá nhân hóa bằng Gemini AI.
 * Dựa trên loại dịch vụ, địa chỉ và thời gian đặt lịch.
 */
class AiChecklistViewModel : ViewModel() {

    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val modelName = "gemini-2.5-flash"
    private val apiUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ─── Data class cho từng mục checklist ──────────────────────────
    data class ChecklistItem(
        val text: String,
        val checked: Boolean = false
    )

    // ─── States ─────────────────────────────────────────────────────
    private val _items = MutableStateFlow<List<ChecklistItem>>(emptyList())
    val items: StateFlow<List<ChecklistItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ─── Toggle checkbox ────────────────────────────────────────────
    fun toggleItem(index: Int) {
        val current = _items.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(checked = !current[index].checked)
            _items.value = current
        }
    }

    // ─── Tạo checklist từ AI ────────────────────────────────────────
    fun generateChecklist(serviceName: String, address: String, date: String, time: String) {
        if (apiKey.isBlank()) {
            _error.value = "Chưa cấu hình API key"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    callGemini(serviceName, address, date, time)
                }
                _items.value = parseChecklist(result)
            } catch (e: Exception) {
                _error.value = e.message ?: "Lỗi không xác định"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun callGemini(serviceName: String, address: String, date: String, time: String): String {
        val systemInstruction = """
            Bạn là chuyên gia vệ sinh nhà cửa. Hãy tạo checklist chuẩn bị cho khách hàng trước khi nhân viên dọn dẹp đến.
            Checklist phải cá nhân hóa dựa trên loại dịch vụ, thời gian.
            
            QUY TẮC:
            - Trả về ĐÚNG 6-8 mục checklist, mỗi mục trên 1 dòng
            - Mỗi dòng bắt đầu bằng "- " (gạch đầu dòng)
            - Viết ngắn gọn, rõ ràng (mỗi mục tối đa 15 từ)
            - Sử dụng emoji ở đầu mỗi mục
            - Chỉ trả về danh sách, KHÔNG viết gì thêm
            - Trả lời bằng tiếng Việt
        """.trimIndent()

        val userPrompt = """
            Dịch vụ: $serviceName
            Địa chỉ: $address
            Ngày: $date
            Giờ: $time
            
            Hãy tạo checklist chuẩn bị cho khách hàng.
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
                put("maxOutputTokens", 512)
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

    private fun parseChecklist(raw: String): List<ChecklistItem> {
        return raw.lines()
            .map { it.trim() }
            .filter { it.startsWith("- ") || it.startsWith("* ") }
            .map { ChecklistItem(text = it.removePrefix("- ").removePrefix("* ").trim()) }
            .ifEmpty {
                // Fallback: mỗi dòng không trống là 1 mục
                raw.lines().filter { it.isNotBlank() }.map { ChecklistItem(text = it.trim()) }
            }
    }
}

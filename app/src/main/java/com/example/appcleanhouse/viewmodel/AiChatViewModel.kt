package com.example.appcleanhouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcleanhouse.BuildConfig
import com.example.appcleanhouse.models.AiChatMessage
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
 * ViewModel cho AI Chat – kiến trúc MVVM.
 *
 * Sử dụng Gemini REST API trực tiếp qua OkHttp (không phụ thuộc SDK deprecated).
 * Quản lý lịch sử hội thoại thủ công để hỗ trợ ngữ cảnh chat.
 */
class AiChatViewModel : ViewModel() {

    // ─── Config ─────────────────────────────────────────────────────
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val modelName = "gemini-3.1-flash-lite-preview"
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

    // ─── System Instruction ─────────────────────────────────────────
    private val systemInstruction = """
        Bạn là trợ lý ảo chăm sóc khách hàng tận tâm và chuyên nghiệp cho ứng dụng đặt lịch dọn dẹp vệ sinh nhà cửa.
        Nhiệm vụ của bạn là:
        - Tư vấn các gói dịch vụ (dọn nhà theo giờ, dọn dẹp định kỳ, tổng vệ sinh sau xây dựng, vệ sinh sofa/rèm/nệm).
        - Hướng dẫn người dùng các bước đặt người giúp việc trên ứng dụng.
        - Giải đáp thắc mắc về giá cả, chính sách đền bù hoặc thay đổi lịch hẹn.
        
        Thông tin dịch vụ của ứng dụng:
        1. Deep Clean (Dọn dẹp sâu) – 45$/giờ – Vệ sinh toàn diện mọi ngóc ngách, thiết bị, chân tường
        2. Standard (Tiêu chuẩn) – 30$/giờ – Dọn dẹp bảo trì thường xuyên cho nhà luôn sạch sẽ
        3. Laundry (Giặt ủi) – 25$/giờ – Giặt, sấy, gấp chuyên nghiệp quần áo và vải
        4. Carpet (Thảm) – 60$/giờ – Giặt hơi nước sâu để loại bỏ vết bẩn cứng đầu và dị ứng nguyên
        
        Bạn phải luôn giữ thái độ lịch sự, nhiệt tình.
        Tuyệt đối KHÔNG trả lời các câu hỏi nằm ngoài phạm vi dịch vụ vệ sinh và ứng dụng này
        (ví dụ: kiến thức chung, code, toán học...).
        Nếu khách hỏi ngoài lề, hãy khéo léo xin lỗi và điều hướng họ về dịch vụ của ứng dụng.
        Hãy trả lời ngắn gọn, rõ ràng (tối đa 3-5 câu). Sử dụng emoji phù hợp để thân thiện.
        Trả lời bằng ngôn ngữ mà khách hàng sử dụng (Tiếng Việt hoặc Tiếng Anh).
    """.trimIndent()

    // ─── OkHttp Client ──────────────────────────────────────────────
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ─── Conversation History (role → text) ─────────────────────────
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    // ─── StateFlow: Danh sách tin nhắn ──────────────────────────────
    private val _messages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                text = "Xin chào! 👋 Tôi là trợ lý ảo CleanHouse.\nTôi có thể giúp bạn:\n• Tư vấn gói dịch vụ dọn dẹp\n• Hướng dẫn đặt lịch\n• Giải đáp về giá cả & chính sách\n\nBạn cần hỗ trợ gì ạ? 🏠✨",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    // ─── StateFlow: Trạng thái đang gõ ──────────────────────────────
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // ─── Gửi tin nhắn ───────────────────────────────────────────────
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 1. Thêm tin nhắn user vào danh sách UI
        val userMessage = AiChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMessage

        // 2. Kiểm tra API key
        if (apiKey.isBlank()) {
            _messages.value = _messages.value + AiChatMessage(
                text = "⚠️ Chưa cấu hình Gemini API key.\nVui lòng thêm GEMINI_API_KEY vào local.properties rồi build lại app.",
                isUser = false,
                isError = true
            )
            return
        }

        // 3. Thêm vào conversation history
        conversationHistory.add("user" to text)

        // 4. Gọi API bất đồng bộ
        viewModelScope.launch {
            _isTyping.value = true
            try {
                val botText = withContext(Dispatchers.IO) { callGeminiApi() }
                conversationHistory.add("model" to botText)
                _messages.value = _messages.value + AiChatMessage(
                    text = botText,
                    isUser = false
                )
            } catch (e: Exception) {
                // Xóa tin nhắn user khỏi history vì API thất bại
                if (conversationHistory.isNotEmpty()) conversationHistory.removeLastOrNull()
                val errorMsg = "❌ ${e.message ?: "Lỗi không xác định"}.\nVui lòng thử lại ạ."
                _messages.value = _messages.value + AiChatMessage(
                    text = errorMsg,
                    isUser = false,
                    isError = true
                )
            } finally {
                _isTyping.value = false
            }
        }
    }

    // ─── Gọi Gemini REST API ────────────────────────────────────────
    private fun callGeminiApi(): String {
        val requestBody = JSONObject().apply {
            // System instruction
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })

            // Conversation history
            put("contents", JSONArray().apply {
                for ((role, msg) in conversationHistory) {
                    put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().put(JSONObject().put("text", msg)))
                    })
                }
            })

            // Generation config
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
        val text = json.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")

        return text?.trim() ?: "Xin lỗi, tôi không thể phản hồi lúc này. Vui lòng thử lại ạ."
    }
}

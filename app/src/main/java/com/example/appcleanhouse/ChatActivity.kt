package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.adapter.SmartMessage
import com.example.appcleanhouse.adapter.SmartMessageAdapter
import com.example.appcleanhouse.api.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: FrameLayout
    private lateinit var typingIndicator: LinearLayout
    private lateinit var adapter: SmartMessageAdapter

    private val messages = mutableListOf<SmartMessage>()

    // System prompt to make it a cleaning expert
    private val systemPrompt = """
        You are Sparkle, a friendly and knowledgeable cleaning expert assistant for a professional home cleaning app called CleanHouse.
        - Keep answers concise but helpful (2-4 sentences max).
        - Focus on cleaning tips, stain removal, product recommendations, and scheduling advice.
        - Use a warm, professional tone.
        - Reply in the same language as the user's latest message (Vietnamese or English).
        - If asked something unrelated to cleaning/home maintenance, politely redirect.
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        rvMessages       = findViewById(R.id.rvMessages)
        etMessage        = findViewById(R.id.etMessage)
        btnSend          = findViewById(R.id.btnSend)
        typingIndicator  = findViewById(R.id.typingIndicator)

        adapter = SmartMessageAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = adapter

        // Welcome message
        messages.add(SmartMessage("model", "Hi! I'm Sparkle, your cleaning expert 🧹✨\nAsk me anything! e.g., \"How do I remove wine stains?\""))
        adapter.notifyItemInserted(0)

        // Send button
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendUserMessage(text)
                etMessage.setText("")
            }
        }

        // Enter key
        etMessage.setOnEditorActionListener { _, _, _ ->
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendUserMessage(text)
                etMessage.setText("")
            }
            true
        }

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_chat
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_bookings -> {
                    startActivity(Intent(this, BookingHistoryActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_chat -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun sendUserMessage(text: String) {
        // Add user message
        messages.add(SmartMessage("user", text))
        adapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()

        // Show typing indicator
        typingIndicator.visibility = View.VISIBLE

        // Disable input while loading
        btnSend.isEnabled = false
        etMessage.isEnabled = false

        // Call Gemini API
        callGeminiApi()
    }

    private fun callGeminiApi() {
        if (GeminiApi.apiKey.isBlank()) {
            restoreInputState()

            addModelMessage(
                "Chưa cấu hình Gemini API key. Hãy thêm GEMINI_API_KEY vào gradle.properties rồi build lại app.",
                isError = true
            )
            return
        }

        // Build conversation history for context
        val contents = mutableListOf<GeminiContent>()

        // System instruction as first user message
        contents.add(GeminiContent("user", listOf(GeminiPart(systemPrompt))))
        contents.add(GeminiContent("model", listOf(GeminiPart("Understood! I'm Sparkle, ready to help with cleaning questions."))))

        // Add conversation history (last 10 messages for context)
        val historyStart = maxOf(0, messages.size - 10)
        for (i in historyStart until messages.size) {
            val msg = messages[i]
            contents.add(GeminiContent(msg.role, listOf(GeminiPart(msg.text))))
        }

        val request = GeminiRequest(contents)
        requestGeminiWithFallback(request, GeminiApi.MODEL_CANDIDATES, 0)
    }

    private fun requestGeminiWithFallback(
        request: GeminiRequest,
        models: List<String>,
        index: Int
    ) {
        val model = models.getOrNull(index)
        if (model == null) {
            restoreInputState()
            addModelMessage(
                "Khong tim thay model Gemini kha dung. Da thu: ${models.joinToString()}.",
                isError = true
            )
            return
        }

        GeminiApi.service.generateContent(model, GeminiApi.apiKey, request)
            .enqueue(object : Callback<GeminiResponse> {
                override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                    if (response.isSuccessful) {
                        restoreInputState()
                        val responseText = response.body()
                            ?.candidates
                            ?.firstOrNull()
                            ?.content
                            ?.parts
                            ?.firstOrNull()
                            ?.text
                            ?: "I couldn't generate a response. Please try again."

                        addModelMessage(responseText)
                    } else if (response.code() == 404 && index < models.lastIndex) {
                        requestGeminiWithFallback(request, models, index + 1)
                    } else {
                        restoreInputState()
                        addModelMessage(parseGeminiError(response, model), isError = true)
                    }
                }

                override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                    restoreInputState()

                    addModelMessage(
                        "Không thể kết nối tới máy chủ AI: ${t.localizedMessage ?: "unknown error"}",
                        isError = true
                    )
                }
            })
    }

    private fun restoreInputState() {
        typingIndicator.visibility = View.GONE
        btnSend.isEnabled = true
        etMessage.isEnabled = true
    }

    private fun addModelMessage(text: String, isError: Boolean = false) {
        messages.add(SmartMessage("model", text, isError))
        adapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    private fun parseGeminiError(response: Response<GeminiResponse>, attemptedModel: String): String {
        val statusCode = response.code()
        val rawBody = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        val apiMessage = parseApiMessage(rawBody)

        return when (statusCode) {
            400 -> "Yeu cau khong hop le (400). ${apiMessage ?: "Kiem tra model/du lieu gui len."}"
            401 -> "API key khong hop le (401). Kiem tra GEMINI_API_KEY."
            403 -> "API key bi tu choi (403). Thuong do chua bat Gemini API hoac bi gioi han key."
            404 -> "Khong tim thay model Gemini ($attemptedModel)."
            429 -> "Da vuot gioi han su dung (429). Hay thu lai sau hoac kiem tra quota."
            else -> "Loi Gemini HTTP $statusCode. ${apiMessage ?: "Khong co them chi tiet."}"
        }
    }

    private fun parseApiMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null
        return try {
            JSONObject(rawBody).optJSONObject("error")?.optString("message")
        } catch (_: Exception) {
            null
        }
    }

    private fun scrollToBottom() {
        rvMessages.scrollToPosition(messages.size - 1)
    }
}

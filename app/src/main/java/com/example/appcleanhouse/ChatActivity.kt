package com.example.appcleanhouse

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.adapter.SmartMessageAdapter
import com.example.appcleanhouse.viewmodel.AiChatViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

/**
 * AI Chatbot Activity – Trợ lý ảo CleanHouse.
 *
 * Sử dụng kiến trúc MVVM:
 * - AiChatViewModel xử lý logic giao tiếp với Gemini AI
 * - SmartMessageAdapter (ListAdapter + DiffUtil) hiển thị tin nhắn
 * - StateFlow cập nhật UI reactive
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: FrameLayout
    private lateinit var typingIndicator: LinearLayout
    private lateinit var adapter: SmartMessageAdapter
    private lateinit var viewModel: AiChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // ── Khởi tạo ViewModel ──────────────────────────────────────
        viewModel = ViewModelProvider(this)[AiChatViewModel::class.java]

        // ── Bind Views ──────────────────────────────────────────────
        rvMessages      = findViewById(R.id.rvMessages)
        etMessage       = findViewById(R.id.etMessage)
        btnSend         = findViewById(R.id.btnSend)
        typingIndicator = findViewById(R.id.typingIndicator)

        // ── Setup RecyclerView ──────────────────────────────────────
        adapter = SmartMessageAdapter()
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = adapter

        // ── Observe messages (StateFlow) ────────────────────────────
        lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                adapter.submitList(messages) {
                    // Scroll xuống cuối sau khi list được cập nhật
                    if (messages.isNotEmpty()) {
                        rvMessages.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        // ── Observe typing state ────────────────────────────────────
        lifecycleScope.launch {
            viewModel.isTyping.collect { isTyping ->
                if (isTyping) {
                    typingIndicator.visibility = View.VISIBLE
                    startTypingAnimation()
                    // Disable input khi đang chờ phản hồi
                    btnSend.isEnabled = false
                    etMessage.isEnabled = false
                } else {
                    typingIndicator.visibility = View.GONE
                    stopTypingAnimation()
                    btnSend.isEnabled = true
                    etMessage.isEnabled = true
                }
            }
        }

        // ── Send button ─────────────────────────────────────────────
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                etMessage.setText("")
            }
        }

        // ── Enter key ───────────────────────────────────────────────
        etMessage.setOnEditorActionListener { _, _, _ ->
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                etMessage.setText("")
            }
            true
        }

        // ── Bottom Navigation ───────────────────────────────────────
        setupBottomNavigation()
    }

    // ─── Bottom Navigation ──────────────────────────────────────────
    private fun setupBottomNavigation() {
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

    // ─── Typing Indicator Animation (pulse dots) ────────────────────
    private var typingAnimatorSet: AnimatorSet? = null

    private fun startTypingAnimation() {
        val dot1 = findViewById<View>(R.id.dot1)
        val dot2 = findViewById<View>(R.id.dot2)
        val dot3 = findViewById<View>(R.id.dot3)

        fun createPulse(view: View, startDelay: Long): AnimatorSet {
            val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.4f, 1f).apply {
                duration = 600
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                this.startDelay = startDelay
            }
            val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.4f, 1f).apply {
                duration = 600
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                this.startDelay = startDelay
            }
            val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f, 1f).apply {
                duration = 600
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                this.startDelay = startDelay
            }
            return AnimatorSet().apply { playTogether(scaleX, scaleY, alpha) }
        }

        typingAnimatorSet = AnimatorSet().apply {
            playTogether(
                createPulse(dot1, 0),
                createPulse(dot2, 150),
                createPulse(dot3, 300)
            )
            start()
        }
    }

    private fun stopTypingAnimation() {
        typingAnimatorSet?.cancel()
        typingAnimatorSet = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTypingAnimation()
    }
}

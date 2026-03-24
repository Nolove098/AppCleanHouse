package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository

class ProfileActivity : AppCompatActivity() {
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvPaymentSubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvUserName  = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvPaymentSubtitle = findViewById(R.id.tvPaymentSubtitle)

        loadUserProfile()

        findViewById<android.view.View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // ── Tải thông tin người dùng từ Firestore ──────────────────
        // ── Back Button ─────────────────────────────────────────────
        findViewById<android.view.View>(R.id.btnBackProfile).setOnClickListener {
            finish()
        }

        // ── Menu Action Listeners ────────────────────────────────────
        // 1. Chỉnh sửa thông tin → EditProfileActivity
        findViewById<android.view.View>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // 2. Payment → PaymentMethodActivity
        findViewById<android.view.View>(R.id.btnPayment).setOnClickListener {
            startActivity(Intent(this, PaymentMethodActivity::class.java))
        }

        // 3. Help & Support → SupportActivity
        findViewById<android.view.View>(R.id.btnSupport).setOnClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
        }

        // ── Đăng xuất Firebase ──────────────────────────────────────
        findViewById<android.view.View>(R.id.btnLogout).setOnClickListener {
            FirebaseAuthRepository.logout()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = FirebaseAuthRepository.currentUserId
        if (userId.isEmpty()) return

        tvUserEmail.text = FirebaseAuthRepository.currentUser?.email ?: ""
        FirestoreRepository.getUserProfile(userId) { user ->
            runOnUiThread {
                if (user != null) {
                    tvUserName.text = user.fullName.ifEmpty { "User" }
                    tvUserEmail.text = user.email.ifEmpty { tvUserEmail.text }
                    tvPaymentSubtitle.text = when (user.paymentMethod) {
                        "paypal" -> "Đang chọn: PayPal"
                        "applepay" -> "Đang chọn: Apple Pay"
                        "wallet" -> "Đang chọn: Momo Wallet"
                        else -> "Đang chọn: Credit/Debit Card"
                    }
                } else {
                    tvUserName.text = FirebaseAuthRepository.currentUser?.displayName
                        ?: FirebaseAuthRepository.currentUser?.email?.substringBefore("@")
                        ?: "User"
                    tvPaymentSubtitle.text = "Cards, wallet & more"
                }
            }
        }
    }
}

package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvUserName  = findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)

        // ── Tải thông tin người dùng từ Firestore ──────────────────
        val userId = FirebaseAuthRepository.currentUserId
        if (userId.isNotEmpty()) {
            // Hiển thị email ngay từ FirebaseAuth (nhanh, không cần Firestore)
            tvUserEmail.text = FirebaseAuthRepository.currentUser?.email ?: ""

            // Tải thêm fullName từ Firestore
            FirestoreRepository.getUserProfile(userId) { user ->
                if (user != null) {
                    tvUserName.text  = user.fullName.ifEmpty { "User" }
                    tvUserEmail.text = user.email.ifEmpty { user.email }
                } else {
                    tvUserName.text = FirebaseAuthRepository.currentUser?.email ?: "User"
                }
            }
        }

        // ── Đăng xuất Firebase ──────────────────────────────────────
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseAuthRepository.logout()          // Xóa session Firebase
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}

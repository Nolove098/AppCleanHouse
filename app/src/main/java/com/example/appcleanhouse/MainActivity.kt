package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Auto-login: nếu đã đăng nhập rồi thì kiểm tra role rồi vào ──
        if (FirebaseAuthRepository.isLoggedIn) {
            routeByRole()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail    = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignIn  = findViewById<Button>(R.id.btnSignIn)
        val tvSignUp   = findViewById<TextView>(R.id.tvSignUp)

        // ── Seed dữ liệu ban đầu lên Firestore (services & cleaners) ──
        FirestoreRepository.seedInitialData()

        // ── Seed cleaner accounts (chỉ chạy 1 lần) ──
        FirestoreRepository.seedCleanerAccounts()

        // ── Nút Đăng nhập ──────────────────────────────────────────
        btnSignIn.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            // Validation cơ bản
            if (email.isEmpty()) {
                etEmail.error = "Vui lòng nhập email"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Vui lòng nhập mật khẩu"
                return@setOnClickListener
            }

            // Hiển thị loading
            btnSignIn.isEnabled = false
            btnSignIn.text = "Đang đăng nhập..."

            FirebaseAuthRepository.login(
                email = email,
                password = password,
                onSuccess = {
                    routeByRole()
                },
                onFailure = { errorMsg ->
                    btnSignIn.isEnabled = true
                    btnSignIn.text = "Sign In"
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }

        // ── Nút Đăng ký ────────────────────────────────────────────
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Kiểm tra role trong Firestore và chuyển hướng phù hợp:
     * - "cleaner" -> CleanerDashboardActivity
     * - "customer" (hoặc mặc định) -> HomeActivity
     */
    private fun routeByRole() {
        val userId = FirebaseAuthRepository.currentUserId
        if (userId.isEmpty()) {
            goToHome()
            return
        }

        FcmTokenManager.syncCurrentUserToken()

        val email = FirebaseAuthRepository.currentUser?.email ?: ""
        
        // Auto-assign cleaner role & map IDs cho các email nhân viên (giúp hỗ trợ tài khoản tạo tay bằng console)
        val cleanerMapping = mapOf(
            "cleaner1@cleanhouse.com" to "c2", // Hung Nguyen
            "cleaner2@cleanhouse.com" to "c1", // Phan Khai
            "cleaner3@cleanhouse.com" to "c3", // Khoa Tran
            "cleaner4@cleanhouse.com" to "c4"  // Khoi Le
        )

        val cId = cleanerMapping[email]
        if (cId != null) {
            val userMap = mapOf("uid" to userId, "email" to email, "role" to "cleaner")
            FirestoreRepository.usersCol.document(userId).set(userMap).addOnCompleteListener {
                FirestoreRepository.cleanersCol.document(cId).update("authUid", userId).addOnCompleteListener {
                    runOnUiThread {
                        startActivity(Intent(this, CleanerJobBoardActivity::class.java))
                        finish()
                    }
                }
            }
            return
        }

        FirestoreRepository.getUserRole(userId) { role ->
            runOnUiThread {
                if (role == "cleaner") {
                    startActivity(Intent(this, CleanerJobBoardActivity::class.java))
                } else {
                    startActivity(Intent(this, HomeActivity::class.java))
                }
                finish()
            }
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
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

        // ── Auto-login: nếu đã đăng nhập rồi thì vào thẳng Home ──
        if (FirebaseAuthRepository.isLoggedIn) {
            goToHome()
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
                    goToHome()
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

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
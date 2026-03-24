package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appcleanhouse.models.User
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.button.MaterialButton

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnBack   = findViewById<ImageButton>(R.id.btnBack)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etPhone    = findViewById<EditText>(R.id.etPhone)
        val etEmail    = findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)

        // ── Nút back ───────────────────────────────────────────────
        btnBack.setOnClickListener { finish() }

        // ── Nút Đăng ký ────────────────────────────────────────────
        btnSignUp.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val phone    = etPhone.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            // Validation
            if (fullName.isEmpty()) { etFullName.error = "Vui lòng nhập họ tên"; return@setOnClickListener }
            if (phone.isEmpty())    { etPhone.error    = "Vui lòng nhập số điện thoại"; return@setOnClickListener }
            if (email.isEmpty())    { etEmail.error    = "Vui lòng nhập email"; return@setOnClickListener }
            if (password.length < 6) { etPassword.error = "Mật khẩu tối thiểu 6 ký tự"; return@setOnClickListener }

            // Loading state
            btnSignUp.isEnabled = false
            btnSignUp.text = "Đang đăng ký..."

            // Gọi Firebase Auth để tạo tài khoản
            FirebaseAuthRepository.register(
                email = email,
                password = password,
                onSuccess = { firebaseUser ->
                    val newUser = User(
                        uid      = firebaseUser.uid,
                        email    = email,
                        fullName = fullName,
                        phone    = phone
                    )
                    FirestoreRepository.saveUserProfile(
                        user = newUser,
                        onSuccess = {
                            FcmTokenManager.syncCurrentUserToken()
                            Toast.makeText(this, "Đăng ký thành công! Chào mừng $fullName 🎉", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        },
                        onFailure = { err ->
                            FcmTokenManager.syncCurrentUserToken()
                            Toast.makeText(this, "Đăng ký thành công nhưng không lưu được thông tin: $err", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        }
                    )
                },
                onFailure = { errorMsg ->
                    btnSignUp.isEnabled = true
                    btnSignUp.text = "Sign Up Now"
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}


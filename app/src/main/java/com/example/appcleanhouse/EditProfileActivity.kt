package com.example.appcleanhouse

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appcleanhouse.models.User
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private var currentPaymentMethod: String = "card"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        etFullName = findViewById(R.id.etFullName)
        etEmail    = findViewById(R.id.etEmail)
        etPhone    = findViewById(R.id.etPhone)
        etAddress  = findViewById(R.id.etAddress)

        // Back button
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        // Load current user info
        val userId = FirebaseAuthRepository.currentUserId
        etEmail.setText(FirebaseAuthRepository.currentUser?.email ?: "")

        if (userId.isNotEmpty()) {
            FirestoreRepository.getUserProfile(userId) { user ->
                runOnUiThread {
                    user?.let {
                        etFullName.setText(it.fullName)
                        etPhone.setText(it.phone)
                        etAddress.setText(it.address)
                        currentPaymentMethod = it.paymentMethod.ifEmpty { "card" }
                    }
                }
            }
        }

        // Save button
        val saveAction = {
            val name    = etFullName.text.toString().trim()
            val phone   = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập họ và tên", Toast.LENGTH_SHORT).show()
            } else {
                val updatedUser = User(
                    uid      = userId,
                    fullName = name,
                    email    = FirebaseAuthRepository.currentUser?.email ?: "",
                    phone    = phone,
                    address  = address,
                    paymentMethod = currentPaymentMethod
                )
                FirestoreRepository.saveUserProfile(
                    user      = updatedUser,
                    onSuccess = {
                        Toast.makeText(this, "Đã lưu thông tin thành công!", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onFailure = { err ->
                        Toast.makeText(this, "Lỗi: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        findViewById<android.view.View>(R.id.btnSave).setOnClickListener { saveAction() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveProfile)
            .setOnClickListener { saveAction() }
    }
}

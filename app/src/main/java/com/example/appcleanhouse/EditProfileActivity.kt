package com.example.appcleanhouse

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appcleanhouse.models.User
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etBirthDate: EditText
    private lateinit var btnSaveTop: TextView
    private lateinit var btnSaveBottom: com.google.android.material.button.MaterialButton
    private var currentPaymentMethod: String = "card"
    private var currentRole: String = "customer"
    private var initialName: String = ""
    private var initialEmail: String = ""
    private var initialPhone: String = ""
    private var initialAddress: String = ""
    private var initialBirthDate: String = ""
    private var isSaving = false
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        etFullName = findViewById(R.id.etFullName)
        etEmail    = findViewById(R.id.etEmail)
        etPhone    = findViewById(R.id.etPhone)
        etAddress  = findViewById(R.id.etAddress)
        etBirthDate = findViewById(R.id.etBirthDate)
        btnSaveTop = findViewById(R.id.btnSave)
        btnSaveBottom = findViewById(R.id.btnSaveProfile)

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnChangeAvatar).setOnClickListener {
            Toast.makeText(this, "Avatar upload will be available soon", Toast.LENGTH_SHORT).show()
        }

        // Load current user info
        val userId = FirebaseAuthRepository.currentUserId
        etEmail.setText(FirebaseAuthRepository.currentUser?.email ?: "")

        if (userId.isNotEmpty()) {
            FirestoreRepository.getUserProfile(userId) { user ->
                runOnUiThread {
                    user?.let {
                        etFullName.setText(it.fullName)
                        etEmail.setText(it.email.ifBlank { FirebaseAuthRepository.currentUser?.email ?: "" })
                        etPhone.setText(it.phone)
                        etAddress.setText(it.address)
                        etBirthDate.setText(it.birthDate)
                        initialName = it.fullName
                        initialEmail = it.email.ifBlank { FirebaseAuthRepository.currentUser?.email ?: "" }
                        initialPhone = it.phone
                        initialAddress = it.address
                        initialBirthDate = it.birthDate
                        currentPaymentMethod = it.paymentMethod.ifEmpty { "card" }
                        currentRole = it.role.ifEmpty { "customer" }
                        updateSaveState()
                    }
                }
            }
        }

        etBirthDate.setOnClickListener { showBirthDatePicker() }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                etFullName.error = null
                etPhone.error = null
                etEmail.error = null
                if (!isSaving) updateSaveState()
            }
        }
        etFullName.addTextChangedListener(watcher)
        etEmail.addTextChangedListener(watcher)
        etPhone.addTextChangedListener(watcher)
        etAddress.addTextChangedListener(watcher)
        etBirthDate.addTextChangedListener(watcher)

        // Save button
        val saveAction: () -> Unit = {
            if (!isSaving) {
                val name    = etFullName.text.toString().trim()
                val email   = etEmail.text.toString().trim()
                val phone   = etPhone.text.toString().trim()
                val address = etAddress.text.toString().trim()
                val birthDate = etBirthDate.text.toString().trim()

                if (name.isEmpty()) {
                    etFullName.error = "Vui lòng nhập họ và tên"
                    Toast.makeText(this, "Vui lòng nhập họ và tên", Toast.LENGTH_SHORT).show()
                } else if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.error = "Email chưa hợp lệ"
                    Toast.makeText(this, "Email chưa hợp lệ", Toast.LENGTH_SHORT).show()
                } else if (phone.isNotBlank() && phone.length < 9) {
                    etPhone.error = "Số điện thoại chưa hợp lệ"
                    Toast.makeText(this, "Số điện thoại chưa hợp lệ", Toast.LENGTH_SHORT).show()
                } else {
                setSavingState(true)

                fun saveProfile() {
                    val updatedUser = User(
                        uid      = userId,
                        fullName = name,
                        email    = email,
                        phone    = phone,
                        address  = address,
                        birthDate = birthDate,
                        paymentMethod = currentPaymentMethod,
                        role = currentRole
                    )
                    FirestoreRepository.saveUserProfile(
                        user      = updatedUser,
                        onSuccess = {
                            initialName = name
                            initialEmail = email
                            initialPhone = phone
                            initialAddress = address
                            initialBirthDate = birthDate
                            setSavingState(false)
                            Toast.makeText(this, "Đã lưu thông tin thành công!", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onFailure = { err ->
                            setSavingState(false)
                            Toast.makeText(this, "Lỗi: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                val authEmail = FirebaseAuthRepository.currentUser?.email.orEmpty()
                if (email != authEmail && FirebaseAuthRepository.currentUser != null) {
                    FirebaseAuthRepository.currentUser!!.updateEmail(email)
                        .addOnSuccessListener { saveProfile() }
                        .addOnFailureListener {
                            setSavingState(false)
                            Toast.makeText(this, "Không thể đổi email, vui lòng đăng nhập lại rồi thử lại", Toast.LENGTH_LONG).show()
                        }
                } else {
                    saveProfile()
                }
                }
            }
        }

        btnSaveTop.setOnClickListener { saveAction() }
        btnSaveBottom.setOnClickListener { saveAction() }
    }

    private fun updateSaveState() {
        val changed = etFullName.text.toString().trim() != initialName ||
            etEmail.text.toString().trim() != initialEmail ||
            etPhone.text.toString().trim() != initialPhone ||
            etAddress.text.toString().trim() != initialAddress ||
            etBirthDate.text.toString().trim() != initialBirthDate

        val enabled = changed && !isSaving
        btnSaveTop.isEnabled = enabled
        btnSaveBottom.isEnabled = enabled
        btnSaveTop.alpha = if (enabled) 1f else 0.5f
        btnSaveBottom.alpha = if (enabled) 1f else 0.5f
    }

    private fun setSavingState(saving: Boolean) {
        isSaving = saving
        btnSaveBottom.text = if (saving) "Saving..." else "Save changes"
        updateSaveState()
    }

    private fun showBirthDatePicker() {
        val calendar = Calendar.getInstance()
        val parsed = runCatching { dateFormatter.parse(etBirthDate.text.toString().trim()) }.getOrNull()
        if (parsed != null) {
            calendar.time = parsed
        } else {
            calendar.set(1992, Calendar.MAY, 15)
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                etBirthDate.setText(dateFormatter.format(picked.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}

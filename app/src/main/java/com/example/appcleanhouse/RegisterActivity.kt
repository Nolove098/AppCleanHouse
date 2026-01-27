package com.example.appcleanhouse

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignUp).setOnClickListener {
            // Navigate back to Login (MainActivity)
            finish()
        }
    }
}

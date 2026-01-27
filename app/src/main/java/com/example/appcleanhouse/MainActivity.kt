package com.example.appcleanhouse

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.widget.TextView>(R.id.tvSignUp).setOnClickListener {
            startActivity(android.content.Intent(this, RegisterActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.btnSignIn).setOnClickListener {
            // In a real app, validate and auth here
            startActivity(android.content.Intent(this, HomeActivity::class.java))
            finish() // Prevent going back to login
        }
    }
}
package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository

class SupportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnChatWithUs).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
    }
}

package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class BookingSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_success)

        val btnViewDetails = findViewById<MaterialButton>(R.id.btnViewDetails)
        btnViewDetails.setOnClickListener {
            val intent = Intent(this, BookingDetailActivity::class.java)
            startActivity(intent)
            finish() // Optional: close success screen so back doesn't return here
        }

        val tvBackToHome = findViewById<TextView>(R.id.tvBackToHome)
        tvBackToHome.setOnClickListener {
             val intent = Intent(this, HomeActivity::class.java)
             intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
             startActivity(intent)
             finish()
        }
    }
}

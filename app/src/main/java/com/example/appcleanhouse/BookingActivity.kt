package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class BookingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val btnContinue = findViewById<MaterialButton>(R.id.btnContinueBooking)
        btnContinue.setOnClickListener {
            val intent = Intent(this, BookingSuccessActivity::class.java)
            startActivity(intent)
        }
    }
}

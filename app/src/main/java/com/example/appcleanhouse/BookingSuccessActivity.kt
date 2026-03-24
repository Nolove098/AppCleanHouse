package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class BookingSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_success)
        val orderId = intent.getStringExtra("ORDER_ID")

        val btnBackToHome = findViewById<MaterialButton>(R.id.btnBackToHome)
        val btnViewBookingDetail = findViewById<MaterialButton>(R.id.btnViewBookingDetail)

        btnBackToHome.setOnClickListener {
            // Navigate back to Home and clear the back stack
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        btnViewBookingDetail.setOnClickListener {
            if (orderId.isNullOrEmpty()) {
                finish()
                return@setOnClickListener
            }
            startActivity(Intent(this, BookingDetailActivity::class.java).apply {
                putExtra("ORDER_ID", orderId)
            })
            finish()
        }
    }
}

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

        // Nhận data từ BookingActivity để truyền sang AI Checklist
        val serviceName = intent.getStringExtra("SERVICE_NAME") ?: ""
        val address = intent.getStringExtra("ADDRESS") ?: ""
        val date = intent.getStringExtra("DATE") ?: ""
        val time = intent.getStringExtra("TIME") ?: ""

        val btnBackToHome = findViewById<MaterialButton>(R.id.btnBackToHome)
        val btnViewBookingDetail = findViewById<MaterialButton>(R.id.btnViewBookingDetail)
        val btnAiChecklist = findViewById<MaterialButton>(R.id.btnAiChecklist)

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

        // Nút mở AI Checklist
        btnAiChecklist.setOnClickListener {
            startActivity(Intent(this, AiChecklistActivity::class.java).apply {
                putExtra("SERVICE_NAME", serviceName)
                putExtra("ADDRESS", address)
                putExtra("DATE", date)
                putExtra("TIME", time)
            })
        }
    }
}

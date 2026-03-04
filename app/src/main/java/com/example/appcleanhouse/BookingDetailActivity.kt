package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.data.MockData
import com.google.android.material.button.MaterialButton

class BookingDetailActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_detail)

        val orderId = intent.getStringExtra("ORDER_ID") ?: "o1"
        val order = MockData.MOCK_ORDERS.find { it.id == orderId } ?: MockData.MOCK_ORDERS[0]
        val service = MockData.MOCK_SERVICES.find { it.id == order.serviceId }
        val cleaner = MockData.MOCK_CLEANERS.find { it.id == order.cleanerId }

        // Set status header color
        val statusHeader = findViewById<View>(R.id.statusHeader)
        statusHeader.setBackgroundColor(ContextCompat.getColor(this, getStatusColor(order.status)))

        // Set status badge
        val tvStatusBadge = findViewById<TextView>(R.id.tvStatusBadge)
        tvStatusBadge.text = order.status.uppercase()

        // Set service name and order ID
        val tvServiceName = findViewById<TextView>(R.id.tvServiceName)
        val tvOrderId = findViewById<TextView>(R.id.tvOrderId)
        tvServiceName.text = service?.name ?: "Cleaning"
        tvOrderId.text = "Order #${order.id.uppercase()}"

        // Set cleaner info
        if (cleaner != null) {
            val ivCleanerAvatar = findViewById<ImageView>(R.id.ivCleanerAvatar)
            val tvCleanerName = findViewById<TextView>(R.id.tvCleanerName)
            val tvCleanerRating = findViewById<TextView>(R.id.tvCleanerRating)
            
            ivCleanerAvatar.setImageResource(cleaner.avatarResId)
            tvCleanerName.text = cleaner.name
            tvCleanerRating.text = "⭐ ${cleaner.rating}"
        }

        // Set job details
        val tvDateTime = findViewById<TextView>(R.id.tvDateTime)
        val tvAddress = findViewById<TextView>(R.id.tvAddress)
        tvDateTime.text = "${order.date} • ${order.time}"
        tvAddress.text = order.address

        // Set payment summary
        val tvServiceFee = findViewById<TextView>(R.id.tvServiceFee)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)
        tvServiceFee.text = "$${String.format("%.2f", order.totalPrice)}"
        tvTotal.text = "$${String.format("%.2f", order.totalPrice + 5.00)}"

        // Back button
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Rebook button
        val btnRebook = findViewById<MaterialButton>(R.id.btnRebook)
        btnRebook.setOnClickListener {
            val intent = Intent(this, BookingActivity::class.java)
            intent.putExtra("SERVICE_ID", service?.id)
            startActivity(intent)
        }

        // Invoice and Help buttons (placeholder)
        val btnInvoice = findViewById<MaterialButton>(R.id.btnInvoice)
        val btnHelp = findViewById<MaterialButton>(R.id.btnHelp)
        
        btnInvoice.setOnClickListener {
            // TODO: Implement invoice download
        }
        
        btnHelp.setOnClickListener {
            // TODO: Implement help/support
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status) {
            "Completed" -> R.color.green_600
            "Upcoming" -> R.color.blue_500
            "Cancelled" -> R.color.red_700
            else -> R.color.slate_500
        }
    }
}

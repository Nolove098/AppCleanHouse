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
import java.text.NumberFormat
import java.util.Locale

class BookingDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_detail)

        // Get order ID
        val orderId = intent.getStringExtra("ORDER_ID") ?: "o1"
        val order = MockData.MOCK_ORDERS.find { it.id == orderId }
            ?: MockData.MOCK_ORDERS.first()

        val service = MockData.MOCK_SERVICES.find { it.id == order.serviceId }
        val cleaner = MockData.MOCK_CLEANERS.find { it.id == order.cleanerId }

        // =========================
        // HEADER
        // =========================
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // =========================
        // STATUS HEADER COLOR
        // =========================
        val statusHeader = findViewById<View>(R.id.statusHeader)
        statusHeader.setBackgroundColor(
            ContextCompat.getColor(this, getStatusColor(order.status))
        )

        // =========================
        // DATE & ADDRESS
        // =========================
        val tvDateTime = findViewById<TextView>(R.id.tvDateTime)
        val tvAddress = findViewById<TextView>(R.id.tvAddress)

        tvDateTime.text = "${order.date} - ${order.time}"
        tvAddress.text = order.address




        // =========================
        // PAYMENT SUMMARY (Optional dynamic)
        // =========================
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

        // Nếu bạn muốn bind dynamic thay vì hardcode $55
        // Cần thêm id vào các TextView trong XML
        // Ví dụ: tvSubtotal, tvTax, tvTotal

        // =========================
        // REBOOK BUTTON
        // =========================
        val btnRebook = findViewById<MaterialButton>(R.id.btnRebook)
        btnRebook.setOnClickListener {
            service?.id?.let {
                val intent = Intent(this, BookingActivity::class.java)
                intent.putExtra("SERVICE_ID", it)
                startActivity(intent)
            }
        }

        // =========================
        // CANCEL TEXT (demo)
        // =========================
        val tvCancel = findViewById<TextView>(R.id.tvCancel)
        tvCancel.setOnClickListener {
            // Ví dụ: đổi trạng thái sang cancelled
            statusHeader.setBackgroundColor(
                ContextCompat.getColor(this, R.color.status_error)
            )
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status.lowercase()) {
            "completed" -> R.color.status_completed
            "upcoming" -> R.color.status_upcoming
            "in progress" -> R.color.status_in_progress
            "cancelled" -> R.color.status_error
            else -> R.color.slate_500
        }
    }
}
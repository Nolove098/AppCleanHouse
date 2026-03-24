package com.example.appcleanhouse

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.repository.FirestoreRepository

class TrackingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        findViewById<ImageButton>(R.id.btnBackTracking).setOnClickListener { finish() }

        val orderId = intent.getStringExtra("ORDER_ID") ?: "o1"
        val mockOrder = MockData.MOCK_ORDERS.find { it.id == orderId } ?: MockData.MOCK_ORDERS.first()

        FirestoreRepository.getOrderById(
            orderId = orderId,
            onResult = { order -> runOnUiThread { bindOrder(order ?: mockOrder) } },
            onFailure = { runOnUiThread { bindOrder(mockOrder) } }
        )
    }

    private fun bindOrder(order: Order) {
        val cleaner = MockData.MOCK_CLEANERS.find { it.id == order.cleanerId }
        findViewById<TextView>(R.id.tvTrackingCleanerName).text = order.cleanerName.ifBlank { cleaner?.name ?: "Cleaner" }
        findViewById<TextView>(R.id.tvTrackingCleanerRating).text = String.format("%.1f ★", cleaner?.rating ?: 0.0)

        val etaText = when (order.status) {
            "Upcoming" -> "Booking confirmed. Cleaner will arrive as scheduled."
            "In Progress" -> "Cleaning is currently in progress."
            "Completed" -> "Service completed successfully."
            "Cancelled" -> "This booking was cancelled."
            else -> "Tracking unavailable."
        }
        findViewById<TextView>(R.id.tvEta).text = etaText

        val primaryColor = ContextCompat.getColor(this, R.color.app_primary)
        val activeColor = ContextCompat.getColor(this, R.color.app_accent)
        val mutedColor = ContextCompat.getColor(this, R.color.slate_300)

        val iconConfirmed = findViewById<android.widget.ImageView>(R.id.iconConfirmed)
        val iconOnWay = findViewById<android.widget.ImageView>(R.id.iconOnWay)
        val iconInProgress = findViewById<android.widget.ImageView>(R.id.iconInProgress)
        val iconCompleted = findViewById<android.widget.ImageView>(R.id.iconCompleted)

        iconConfirmed.setColorFilter(primaryColor)
        iconOnWay.setColorFilter(mutedColor)
        iconInProgress.setColorFilter(mutedColor)
        iconCompleted.setColorFilter(mutedColor)

        when (order.status) {
            "Upcoming" -> {
                iconOnWay.setColorFilter(activeColor)
            }
            "In Progress" -> {
                iconOnWay.setColorFilter(primaryColor)
                iconInProgress.setColorFilter(activeColor)
            }
            "Completed" -> {
                iconOnWay.setColorFilter(primaryColor)
                iconInProgress.setColorFilter(primaryColor)
                iconCompleted.setColorFilter(primaryColor)
            }
        }
    }
}

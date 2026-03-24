package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.button.MaterialButton
import java.util.Locale

class BookingDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_detail)

        val orderId = intent.getStringExtra("ORDER_ID") ?: "o1"
        val mockOrder = MockData.MOCK_ORDERS.find { it.id == orderId } ?: MockData.MOCK_ORDERS.first()

        FirestoreRepository.getOrderById(
            orderId = orderId,
            onResult = { fsOrder ->
                runOnUiThread {
                    bindOrder(fsOrder ?: mockOrder)
                }
            },
            onFailure = {
                runOnUiThread {
                    bindOrder(mockOrder)
                }
            }
        )
    }

    private fun bindOrder(order: Order) {

        val service = MockData.MOCK_SERVICES.find { it.id == order.serviceId }
        val cleaner = MockData.MOCK_CLEANERS.find { it.id == order.cleanerId }

        // ── Back button ──────────────────────────────────────────────
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // ── Hero header color (matches React OrderDetail getStatusColor) ──
        val heroHeader = findViewById<FrameLayout>(R.id.heroHeader)
        heroHeader.setBackgroundColor(
            ContextCompat.getColor(this, getStatusBgColor(order.status))
        )

        // ── Status badge + Service name + Order ID ───────────────────
        findViewById<TextView>(R.id.tvStatusLabel).text = order.status.uppercase(Locale.ROOT)
        findViewById<TextView>(R.id.tvServiceName).text  = service?.name ?: "Cleaning"
        findViewById<TextView>(R.id.tvOrderId).text      = "Order #${order.id.uppercase(Locale.ROOT)}"

        // ── Cleaner info ─────────────────────────────────────────────
        val ivAvatar = findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.ivCleanerAvatar)
        if (cleaner != null) {
            findViewById<TextView>(R.id.tvCleanerName).text   = order.cleanerName.ifEmpty { cleaner.name }
            findViewById<TextView>(R.id.tvCleanerRating).text = cleaner.rating.toString()
            ivAvatar.setImageResource(cleaner.avatarResId)
        } else {
            findViewById<TextView>(R.id.tvCleanerName).text = order.cleanerName.ifEmpty { "Cleaner" }
            ivAvatar.setImageResource(R.drawable.idol1)
        }

        // ── Job details ──────────────────────────────────────────────
        findViewById<TextView>(R.id.tvDateTime).text = "${order.date} • ${order.time}"
        findViewById<TextView>(R.id.tvAddress).text  = order.address

        // ── Payment summary ───────────────────────────────────────────
        val fee = order.totalPrice
        val taxFee = 5.0
        val total = fee + taxFee
        findViewById<TextView>(R.id.tvPaymentFee).text   = "$${"%.2f".format(fee)}"
        findViewById<TextView>(R.id.tvPaymentTotal).text = "$${"%.2f".format(total)}"

        // ── Rebook button ─────────────────────────────────────────────
        val btnPrimaryAction = findViewById<MaterialButton>(R.id.btnRebook)
        val shouldRateCleaner = order.status == "Completed" && order.rating == null
        btnPrimaryAction.text = if (shouldRateCleaner) "Rate Cleaner" else "Rebook Service"
        btnPrimaryAction.setOnClickListener {
            if (shouldRateCleaner) {
                val intent = Intent(this, RatingActivity::class.java)
                intent.putExtra("ORDER_ID", order.id)
                intent.putExtra("CLEANER_ID", order.cleanerId)
                intent.putExtra("CLEANER_NAME", cleaner?.name ?: order.cleanerName)
                intent.putExtra("SERVICE_ID", order.serviceId)
                startActivity(intent)
            } else {
                val intent = Intent(this, BookingActivity::class.java)
                intent.putExtra("SERVICE_ID", order.serviceId)
                intent.putExtra("CLEANER_ID", order.cleanerId)
                startActivity(intent)
            }
        }

        // ── Help button ───────────────────────────────────────────────
        findViewById<android.widget.LinearLayout>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
        }

        findViewById<android.widget.LinearLayout>(R.id.btnInvoice).setOnClickListener {
            startActivity(Intent(this, TrackingActivity::class.java).apply {
                putExtra("ORDER_ID", order.id)
            })
        }

        // ── Chat with Cleaner button ─────────────────────────────────
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnChatCleaner).setOnClickListener {
            val currentUserId = com.example.appcleanhouse.repository.FirebaseAuthRepository.currentUserId
            com.example.appcleanhouse.repository.FirestoreRepository.getUserProfile(currentUserId) { user ->
                val customerName = user?.fullName ?: "Customer"
                val intent = Intent(this, RealtimeChatActivity::class.java)
                intent.putExtra("CUSTOMER_ID", currentUserId)
                intent.putExtra("CUSTOMER_NAME", customerName)
                intent.putExtra("CLEANER_ID", order.cleanerId)
                intent.putExtra("CLEANER_NAME", cleaner?.name ?: order.cleanerName)
                intent.putExtra("ORDER_ID", order.id)
                intent.putExtra("PARTNER_NAME", cleaner?.name ?: order.cleanerName)
                intent.putExtra("USER_ROLE", "customer")
                startActivity(intent)
            }
        }
    }

    private fun getStatusBgColor(status: String): Int = when (status.lowercase(Locale.ROOT)) {
        "completed"   -> R.color.status_completed
        "upcoming"    -> R.color.blue_600
        "in progress" -> R.color.status_in_progress
        "cancelled"   -> R.color.status_error
        else          -> R.color.slate_500
    }
}
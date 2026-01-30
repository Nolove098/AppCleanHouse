package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.data.MockData
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class BookingHistoryActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_history)

        // Setup Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_bookings
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_bookings -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }

        val ordersContainer = findViewById<LinearLayout>(R.id.ordersContainer)
        
        // Populate orders
        MockData.MOCK_ORDERS.forEach { order ->
            val service = MockData.MOCK_SERVICES.find { it.id == order.serviceId }
            val cleaner = MockData.MOCK_CLEANERS.find { it.id == order.cleanerId }
            
            if (service != null) {
                val orderCard = createOrderCard(order, service)
                ordersContainer.addView(orderCard)
            }
        }
    }

    private fun createOrderCard(order: com.example.appcleanhouse.models.Order, service: com.example.appcleanhouse.models.Service): MaterialCardView {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.card_margin_small) * 2
            }
            radius = resources.getDimension(R.dimen.card_corner_radius_large)
            cardElevation = 0f
            strokeWidth = 1
            strokeColor = ContextCompat.getColor(context, R.color.slate_100)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            
            setOnClickListener {
                val intent = Intent(context, BookingDetailActivity::class.java)
                intent.putExtra("ORDER_ID", order.id)
                context.startActivity(intent)
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                resources.getDimensionPixelSize(R.dimen.card_padding),
                resources.getDimensionPixelSize(R.dimen.card_padding),
                resources.getDimensionPixelSize(R.dimen.card_padding),
                resources.getDimensionPixelSize(R.dimen.card_padding)
            )
        }

        // Status and Price Row
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.card_padding)
            }
        }

        // Status Badge
        val statusBadge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 8, 12, 8)
            background = ContextCompat.getDrawable(context, getStatusBackground(order.status))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
        }

        val statusIcon = ImageView(this).apply {
            setImageResource(getStatusIcon(order.status))
            setColorFilter(ContextCompat.getColor(context, getStatusTextColor(order.status)))
            layoutParams = LinearLayout.LayoutParams(16, 16).apply {
                marginEnd = 6
            }
        }

        val statusText = TextView(this).apply {
            text = order.status
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, getStatusTextColor(order.status)))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        statusBadge.addView(statusIcon)
        statusBadge.addView(statusText)

        val priceText = TextView(this).apply {
            text = "$${String.format("%.2f", order.totalPrice)}"
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.slate_900))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        statusRow.addView(statusBadge)
        statusRow.addView(priceText)

        // Service Info Row
        val serviceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.card_padding)
            }
        }

        // Service Icon
        val serviceIcon = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                marginEnd = 16
            }
            background = ContextCompat.getDrawable(context, service.colorResId)
            gravity = android.view.Gravity.CENTER
        }

        val iconText = TextView(this).apply {
            text = service.name[0].toString()
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        serviceIcon.addView(iconText)

        // Service Details
        val serviceDetails = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
        }

        val serviceName = TextView(this).apply {
            text = "${service.name} Service"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.slate_900))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val dateTimeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4
            }
        }

        val dateText = TextView(this).apply {
            text = "📅 ${order.date}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.slate_500))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 12
            }
        }

        val timeText = TextView(this).apply {
            text = "🕐 ${order.time}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.slate_500))
        }

        dateTimeRow.addView(dateText)
        dateTimeRow.addView(timeText)

        serviceDetails.addView(serviceName)
        serviceDetails.addView(dateTimeRow)

        serviceRow.addView(serviceIcon)
        serviceRow.addView(serviceDetails)

        // Divider
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.card_padding)
                bottomMargin = resources.getDimensionPixelSize(R.dimen.card_padding)
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.slate_100))
        }

        // Bottom Row
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val rebookButton = TextView(this).apply {
            text = "🔄 Rebook"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.blue_600))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(12, 12, 12, 12)
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
            setOnClickListener {
                val intent = Intent(context, BookingActivity::class.java)
                intent.putExtra("SERVICE_ID", service.id)
                context.startActivity(intent)
            }
        }

        val arrowIcon = TextView(this).apply {
            text = "→"
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, R.color.slate_400))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(32, 32)
        }

        bottomRow.addView(rebookButton)
        bottomRow.addView(arrowIcon)

        content.addView(statusRow)
        content.addView(serviceRow)
        content.addView(divider)
        content.addView(bottomRow)

        card.addView(content)
        return card
    }

    private fun getStatusBackground(status: String): Int {
        return when (status) {
            "Completed" -> R.drawable.bg_status_completed
            "Upcoming" -> R.drawable.bg_status_upcoming
            "Cancelled" -> R.drawable.bg_status_cancelled
            else -> R.drawable.bg_status_upcoming
        }
    }

    private fun getStatusIcon(status: String): Int {
        return when (status) {
            "Completed" -> R.drawable.ic_check
            "Upcoming" -> R.drawable.ic_clock
            "Cancelled" -> R.drawable.ic_close
            else -> R.drawable.ic_clock
        }
    }

    private fun getStatusTextColor(status: String): Int {
        return when (status) {
            "Completed" -> R.color.green_700
            "Upcoming" -> R.color.blue_700
            "Cancelled" -> R.color.red_700
            else -> R.color.slate_700
        }
    }
}

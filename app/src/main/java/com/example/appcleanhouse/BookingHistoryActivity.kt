package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.bottomnavigation.BottomNavigationView

class BookingHistoryActivity : AppCompatActivity() {

    private lateinit var layoutOrders: LinearLayout
    private lateinit var filterAll: TextView
    private lateinit var filterUpcoming: TextView
    private lateinit var filterInProgress: TextView
    private lateinit var filterCompleted: TextView
    private lateinit var filterCancelled: TextView
    private var selectedStatusFilter: String? = null
    private val orderStatusCache = mutableMapOf<String, String>()
    private var hasObservedRealtimeOrders = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_history)

        layoutOrders = findViewById(R.id.layoutOrders)
        filterAll = findViewById(R.id.filterAll)
        filterUpcoming = findViewById(R.id.filterUpcoming)
        filterInProgress = findViewById(R.id.filterInProgress)
        filterCompleted = findViewById(R.id.filterCompleted)
        filterCancelled = findViewById(R.id.filterCancelled)

        setupFilters()

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_bookings
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_bookings -> true
                R.id.nav_chat -> {
                    startActivity(Intent(this, ChatActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        loadBookings()
    }

    private var ordersListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun loadBookings() {
        val userId = FirebaseAuthRepository.currentUserId
        if (userId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
            return
        }

        ordersListener?.remove()
        ordersListener = FirestoreRepository.getUserOrdersRealtime(userId) { orders ->
            if (orders.isNotEmpty()) {
                if (hasObservedRealtimeOrders) {
                    orders.forEach { order ->
                        val previousStatus = orderStatusCache[order.id]
                        if (previousStatus != null && previousStatus != order.status) {
                            NotificationHelper.notifyBookingStatus(this, order, previousStatus)
                        }
                    }
                }
                orderStatusCache.clear()
                orders.forEach { order -> orderStatusCache[order.id] = order.status }
                hasObservedRealtimeOrders = true
            }

            runOnUiThread {
                layoutOrders.removeAllViews()

                // Use MockData as fallback when Firestore has no orders (demo/dev)
                val displayOrders = if (orders.isEmpty()) {
                    MockData.MOCK_ORDERS.sortedByDescending {
                        FirestoreRepository.resolveOrderTimestamp(
                            storedTimestamp = it.timestamp,
                            date = it.date,
                            time = it.time,
                            fallbackTimestamp = it.timestamp
                        )
                    }
                } else {
                    orders.sortedByDescending {
                        FirestoreRepository.resolveOrderTimestamp(
                            storedTimestamp = it.timestamp,
                            date = it.date,
                            time = it.time,
                            fallbackTimestamp = it.timestamp
                        )
                    }
                }
                val filteredOrders = displayOrders.filterByStatus(selectedStatusFilter)

                val serviceMap = MockData.MOCK_SERVICES.associateBy { it.id }
                val cleanerMap = MockData.MOCK_CLEANERS.associateBy { it.id }

                if (displayOrders === orders && orders.isNotEmpty()) {
                    // Firestore orders – still resolve service names from Firestore
                    FirestoreRepository.getServices(
                        onResult = { services ->
                            val fsServiceMap = services.associateBy { it.id }
                            for (order in filteredOrders) {
                                val serviceName = fsServiceMap[order.serviceId]?.name
                                    ?: serviceMap[order.serviceId]?.name ?: "Service"
                                val cleanerName = order.cleanerName.ifEmpty {
                                    cleanerMap[order.cleanerId]?.name ?: "Cleaner"
                                }
                                layoutOrders.addView(createOrderView(order, serviceName, cleanerName))
                            }
                        },
                        onFailure = {
                            for (order in filteredOrders) {
                                val serviceName = serviceMap[order.serviceId]?.name ?: "Service"
                                val cleanerName = order.cleanerName.ifEmpty {
                                    cleanerMap[order.cleanerId]?.name ?: "Cleaner"
                                }
                                layoutOrders.addView(createOrderView(order, serviceName, cleanerName))
                            }
                        }
                    )
                } else {
                    // MockData fallback
                    for (order in filteredOrders) {
                        val serviceName = serviceMap[order.serviceId]?.name ?: "Service"
                        val cleanerName = order.cleanerName.ifEmpty {
                            cleanerMap[order.cleanerId]?.name ?: "Cleaner"
                        }
                        layoutOrders.addView(createOrderView(order, serviceName, cleanerName))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ordersListener?.remove()
        orderStatusCache.clear()
    }

    private fun setupFilters() {
        filterAll.setOnClickListener { setStatusFilter(null) }
        filterUpcoming.setOnClickListener { setStatusFilter("Upcoming") }
        filterInProgress.setOnClickListener { setStatusFilter("In Progress") }
        filterCompleted.setOnClickListener { setStatusFilter("Completed") }
        filterCancelled.setOnClickListener { setStatusFilter("Cancelled") }
        refreshFilterUi()
    }

    private fun setStatusFilter(filter: String?) {
        selectedStatusFilter = filter
        refreshFilterUi()
        loadBookings()
    }

    private fun refreshFilterUi() {
        renderFilter(filterAll, selectedStatusFilter == null, R.drawable.bg_status_upcoming, R.color.blue_700)
        renderFilter(filterUpcoming, selectedStatusFilter == "Upcoming", R.drawable.bg_status_upcoming, R.color.blue_700)
        renderFilter(filterInProgress, selectedStatusFilter == "In Progress", R.drawable.bg_status_in_progress, R.color.orange_700)
        renderFilter(filterCompleted, selectedStatusFilter == "Completed", R.drawable.bg_status_completed, R.color.green_600)
        renderFilter(filterCancelled, selectedStatusFilter == "Cancelled", R.drawable.bg_status_cancelled, R.color.status_error)
    }

    private fun renderFilter(view: TextView, selected: Boolean, selectedBg: Int, selectedTextColor: Int) {
        if (selected) {
            view.setBackgroundResource(selectedBg)
            view.setTextColor(ContextCompat.getColor(this, selectedTextColor))
        } else {
            view.setBackgroundResource(R.drawable.bg_white_rounded)
            view.setTextColor(ContextCompat.getColor(this, R.color.slate_600))
        }
    }

    private fun List<Order>.filterByStatus(status: String?): List<Order> {
        if (status == null) return this
        return filter { it.status == status }
    }

    private fun createOrderView(order: Order, serviceName: String, cleanerName: String): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_booking, null)

        val tvServiceTitle   = view.findViewById<TextView>(R.id.tvServiceTitle)
        val tvServiceInitial = view.findViewById<TextView>(R.id.tvServiceInitial)
        val tvDate           = view.findViewById<TextView>(R.id.tvDate)
        val tvTime           = view.findViewById<TextView>(R.id.tvTime)
        val tvPrice          = view.findViewById<TextView>(R.id.tvPrice)
        val chipStatus       = view.findViewById<TextView>(R.id.chipStatus)
        val imgService       = view.findViewById<FrameLayout>(R.id.imgService)
        val btnRebook        = view.findViewById<TextView>(R.id.btnRebook)
        val shouldRateCleaner = order.status == "Completed" && order.rating == null

        tvServiceTitle.text   = "$serviceName Service • $cleanerName"
        tvServiceInitial.text = serviceName.firstOrNull()?.toString() ?: "S"
        tvDate.text           = order.date
        tvTime.text           = order.time
        tvPrice.text          = "$${"%.2f".format(order.totalPrice)}"
        chipStatus.text       = order.status

        // ── Service icon: color by SERVICE (D=blue, S=teal, L=orange, C=indigo) ──────────
        val (circleBg, initialColor) = getServiceIconStyle(order.serviceId)
        imgService.setBackgroundResource(circleBg)
        tvServiceInitial.setTextColor(ContextCompat.getColor(this, initialColor))

        // ── Status chip: color by STATUS (Upcoming=blue, In Progress=amber, Completed=green, Cancelled=red) ──
        when (order.status) {
            "Upcoming" -> {
                chipStatus.setBackgroundResource(R.drawable.bg_status_upcoming)
                chipStatus.setTextColor(ContextCompat.getColor(this, R.color.blue_700))
            }
            "In Progress" -> {
                chipStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
                chipStatus.setTextColor(ContextCompat.getColor(this, R.color.orange_700))
            }
            "Completed" -> {
                chipStatus.setBackgroundResource(R.drawable.bg_status_completed)
                chipStatus.setTextColor(ContextCompat.getColor(this, R.color.green_600))
            }
            "Cancelled" -> {
                chipStatus.setBackgroundResource(R.drawable.bg_status_cancelled)
                chipStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error))
            }
            else -> {
                chipStatus.setBackgroundResource(R.drawable.bg_status_upcoming)
                chipStatus.setTextColor(ContextCompat.getColor(this, R.color.blue_700))
            }
        }

        // Rebook tap
        btnRebook.text = if (shouldRateCleaner) "★  Rate Cleaner" else "↺  Rebook"
        btnRebook.setOnClickListener {
            if (shouldRateCleaner) {
                val intent = Intent(this, RatingActivity::class.java)
                intent.putExtra("ORDER_ID", order.id)
                intent.putExtra("CLEANER_ID", order.cleanerId)
                intent.putExtra("CLEANER_NAME", cleanerName)
                intent.putExtra("SERVICE_ID", order.serviceId)
                startActivity(intent)
            } else {
                val intent = Intent(this, BookingActivity::class.java)
                intent.putExtra("SERVICE_ID", order.serviceId)
                intent.putExtra("CLEANER_ID", order.cleanerId)
                startActivity(intent)
            }
        }

        // Card tap → detail
        view.setOnClickListener {
            val intent = Intent(this, BookingDetailActivity::class.java)
            intent.putExtra("ORDER_ID", order.id)
            startActivity(intent)
        }

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = (12 * resources.displayMetrics.density).toInt()
        view.layoutParams = lp
        return view
    }

    /**
     * Maps serviceId → (circle background drawable, icon text color)
     * Each service gets a unique, distinct color so the initials look different:
     *   s1 Deep Clean  → Blue
     *   s2 Standard    → Teal
     *   s3 Laundry     → Orange
     *   s4 Carpet      → Indigo
     */
    private fun getServiceIconStyle(serviceId: String): Pair<Int, Int> = when (serviceId) {
        "s1" -> Pair(R.drawable.bg_circle_blue,   R.color.blue_700)
        "s2" -> Pair(R.drawable.bg_circle_teal,   R.color.teal_500)
        "s3" -> Pair(R.drawable.bg_circle_orange, R.color.orange_700)
        "s4" -> Pair(R.drawable.bg_circle_indigo, R.color.indigo_700)
        else -> Pair(R.drawable.bg_circle_blue,   R.color.blue_700)
    }
}

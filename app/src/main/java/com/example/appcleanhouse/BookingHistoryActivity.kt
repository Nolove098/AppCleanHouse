package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.local.LocalBookingDataSource
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.models.Service
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.concurrent.thread

class BookingHistoryActivity : AppCompatActivity() {

    private lateinit var layoutOrders: LinearLayout
    private lateinit var filterAll: TextView
    private lateinit var filterUpcoming: TextView
    private lateinit var filterInProgress: TextView
    private lateinit var filterCompleted: TextView
    private lateinit var filterCancelled: TextView
    private lateinit var etSearchBookings: EditText
    private var selectedStatusFilter: String? = null
    private var searchQuery: String = ""
    private var latestOrders: List<Order> = emptyList()
    private var isRealtimeFirestoreOrders: Boolean = false
    private var cachedFirestoreServices: Map<String, Service> = emptyMap()
    private var cachedLocalServices: Map<String, Service> = emptyMap()
    private var cachedLocalCleaners: Map<String, com.example.appcleanhouse.models.Cleaner> = emptyMap()
    private lateinit var localBookingDataSource: LocalBookingDataSource
    private val orderStatusCache = mutableMapOf<String, String>()
    private var hasObservedRealtimeOrders = false
    private val referenceSyncIntervalMillis = 6 * 60 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_history)

        layoutOrders = findViewById(R.id.layoutOrders)
        filterAll = findViewById(R.id.filterAll)
        filterUpcoming = findViewById(R.id.filterUpcoming)
        filterInProgress = findViewById(R.id.filterInProgress)
        filterCompleted = findViewById(R.id.filterCompleted)
        filterCancelled = findViewById(R.id.filterCancelled)
        etSearchBookings = findViewById(R.id.etSearchBookings)
        localBookingDataSource = LocalBookingDataSource(this)

        thread {
            localBookingDataSource.upsertServices(MockData.MOCK_SERVICES)
            localBookingDataSource.upsertCleaners(MockData.MOCK_CLEANERS)
            cachedLocalServices = localBookingDataSource.getServicesMap()
            cachedLocalCleaners = localBookingDataSource.getCleanersMap()
            runOnUiThread { renderOrders() }
        }

        syncReferenceDataIfStale()

        setupFilters()
        setupSearch()

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

    private fun syncReferenceDataIfStale() {
        val lastSyncAt = localBookingDataSource.getLastReferenceSyncAt()
        val shouldSync = System.currentTimeMillis() - lastSyncAt >= referenceSyncIntervalMillis
        if (!shouldSync) return

        FirestoreRepository.getServices(
            onResult = { services ->
                thread {
                    localBookingDataSource.upsertServices(services)
                    cachedLocalServices = localBookingDataSource.getServicesMap()
                    runOnUiThread { renderOrders() }
                }
            }
        )

        FirestoreRepository.getCleaners(
            onResult = { cleaners ->
                thread {
                    localBookingDataSource.upsertCleaners(cleaners)
                    cachedLocalCleaners = localBookingDataSource.getCleanersMap()
                    runOnUiThread { renderOrders() }
                }
            }
        )
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
                // Use MockData as fallback when Firestore has no orders (demo/dev)
                val displayOrders = if (orders.isEmpty()) {
                    val localOrders = localBookingDataSource.getOrdersByUser(userId)
                    if (localOrders.isNotEmpty()) {
                        localOrders.sortedByDescending {
                            FirestoreRepository.resolveOrderTimestamp(
                                storedTimestamp = it.timestamp,
                                date = it.date,
                                time = it.time,
                                fallbackTimestamp = it.timestamp
                            )
                        }
                    } else {
                        MockData.MOCK_ORDERS.sortedByDescending {
                            FirestoreRepository.resolveOrderTimestamp(
                                storedTimestamp = it.timestamp,
                                date = it.date,
                                time = it.time,
                                fallbackTimestamp = it.timestamp
                            )
                        }
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
                latestOrders = displayOrders
                isRealtimeFirestoreOrders = (displayOrders === orders && orders.isNotEmpty())

                thread {
                    localBookingDataSource.upsertOrders(displayOrders)
                    cachedLocalServices = localBookingDataSource.getServicesMap()
                    cachedLocalCleaners = localBookingDataSource.getCleanersMap()
                }

                renderOrders()
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

    private fun setupSearch() {
        etSearchBookings.addTextChangedListener { editable ->
            searchQuery = editable?.toString()?.trim().orEmpty()
            renderOrders()
        }
    }

    private fun setStatusFilter(filter: String?) {
        selectedStatusFilter = filter
        refreshFilterUi()
        renderOrders()
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

    private fun List<Order>.filterBySearch(
        query: String,
        serviceMap: Map<String, Service>
    ): List<Order> {
        if (query.isBlank()) return this
        val needle = query.lowercase()
        return filter { order ->
            val serviceName = serviceMap[order.serviceId]?.name.orEmpty()
            serviceName.lowercase().contains(needle)
        }
    }

    private fun renderOrders() {
        layoutOrders.removeAllViews()

        val mockServiceMap = MockData.MOCK_SERVICES.associateBy { it.id }
        val cleanerMap = MockData.MOCK_CLEANERS.associateBy { it.id }
        val localServiceMap = cachedLocalServices
        val localCleanerMap = cachedLocalCleaners

        if (isRealtimeFirestoreOrders) {
            if (cachedFirestoreServices.isEmpty()) {
                FirestoreRepository.getServices(
                    onResult = { services ->
                        cachedFirestoreServices = services.associateBy { it.id }
                        thread {
                            localBookingDataSource.upsertServices(services)
                            cachedLocalServices = localBookingDataSource.getServicesMap()
                        }
                        runOnUiThread { renderOrders() }
                    },
                    onFailure = {
                        runOnUiThread {
                            val mergedServices = mockServiceMap + localServiceMap
                            val mergedCleaners = cleanerMap + localCleanerMap
                            renderOrderList(mergedServices, mergedCleaners)
                        }
                    }
                )
                return
            }
        } else {
            cachedFirestoreServices = emptyMap()
        }

        val serviceMap = if (isRealtimeFirestoreOrders) {
            mockServiceMap + localServiceMap + cachedFirestoreServices
        } else {
            mockServiceMap + localServiceMap
        }
        val mergedCleanerMap = cleanerMap + localCleanerMap
        renderOrderList(serviceMap, mergedCleanerMap)
    }

    private fun renderOrderList(
        serviceMap: Map<String, Service>,
        cleanerMap: Map<String, com.example.appcleanhouse.models.Cleaner>
    ) {
        val filteredOrders = latestOrders
            .filterByStatus(selectedStatusFilter)
            .filterBySearch(searchQuery, serviceMap)

        if (filteredOrders.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No bookings match your filters"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@BookingHistoryActivity, R.color.slate_500))
            }
            layoutOrders.addView(emptyView)
            return
        }

        for (order in filteredOrders) {
            val serviceName = serviceMap[order.serviceId]?.name ?: "Service"
            val cleanerName = order.cleanerName.ifEmpty {
                cleanerMap[order.cleanerId]?.name ?: "Cleaner"
            }
            layoutOrders.addView(createOrderView(order, serviceName, cleanerName))
        }
    }

    private fun updateOrderInMemory(orderId: String, transform: (Order) -> Order) {
        latestOrders = latestOrders.map { order ->
            if (order.id == orderId) transform(order) else order
        }
        renderOrders()
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
        val btnEditService   = view.findViewById<TextView>(R.id.btnEditService)
        val btnCancelBooking = view.findViewById<TextView>(R.id.btnCancelBooking)
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

        btnEditService.visibility = View.VISIBLE
        btnCancelBooking.visibility = View.VISIBLE
        btnEditService.alpha = 1f
        btnCancelBooking.alpha = 1f

        btnEditService.setOnClickListener {
            showServicePickerDialog(order, serviceName)
        }

        btnCancelBooking.setOnClickListener {
            confirmCancelOrder(order)
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

    private fun showServicePickerDialog(order: Order, currentServiceName: String) {
        val serviceMap = if (cachedFirestoreServices.isNotEmpty()) {
            (MockData.MOCK_SERVICES.associateBy { it.id } + cachedFirestoreServices)
        } else {
            MockData.MOCK_SERVICES.associateBy { it.id }
        }

        val selectableServices = serviceMap.values
            .filter { it.id != order.serviceId }
            .sortedBy { it.name }

        if (selectableServices.isEmpty()) {
            Toast.makeText(this, "Không có dịch vụ khác để đổi", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = selectableServices.map { "${it.name} ($${it.pricePerHour}/h)" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Change from $currentServiceName")
            .setItems(labels) { _, which ->
                val newService = selectableServices[which]
                val newTotal = newService.pricePerHour * 3 + 5.0
                if (isRealtimeFirestoreOrders) {
                    FirestoreRepository.updateOrderFields(
                        orderId = order.id,
                        updates = mapOf(
                            "serviceId" to newService.id,
                            "totalPrice" to newTotal
                        ),
                        onSuccess = {
                            thread {
                                localBookingDataSource.updateOrderService(order.id, newService.id, newTotal)
                            }
                            updateOrderInMemory(order.id) {
                                it.copy(serviceId = newService.id, totalPrice = newTotal)
                            }
                            Toast.makeText(this, "Đã đổi sang dịch vụ ${newService.name}", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { message ->
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    thread {
                        localBookingDataSource.updateOrderService(order.id, newService.id, newTotal)
                    }
                    updateOrderInMemory(order.id) {
                        it.copy(serviceId = newService.id, totalPrice = newTotal)
                    }
                    Toast.makeText(this, "Đã cập nhật dịch vụ", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun confirmCancelOrder(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Cancel booking")
            .setMessage("Bạn muốn hủy booking này?")
            .setPositiveButton("Cancel Booking") { _, _ ->
                if (isRealtimeFirestoreOrders) {
                    FirestoreRepository.updateOrderStatus(
                        orderId = order.id,
                        newStatus = "Cancelled",
                        onSuccess = {
                            thread {
                                localBookingDataSource.updateOrderStatus(order.id, "Cancelled")
                            }
                            updateOrderInMemory(order.id) {
                                it.copy(status = "Cancelled")
                            }
                            Toast.makeText(this, "Booking đã được hủy", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { message ->
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    thread {
                        localBookingDataSource.updateOrderStatus(order.id, "Cancelled")
                    }
                    updateOrderInMemory(order.id) {
                        it.copy(status = "Cancelled")
                    }
                    Toast.makeText(this, "Booking đã được hủy", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Keep", null)
            .show()
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

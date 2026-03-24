package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * Màn hình danh sách công việc (Jobs) của Cleaner.
 */
class CleanerJobBoardActivity : AppCompatActivity() {

    private lateinit var layoutOrders: LinearLayout
    private lateinit var emptyState: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var tvCountAll: TextView
    private lateinit var tvCountUpcoming: TextView
    private lateinit var tvCountInProgress: TextView
    private lateinit var filterAll: TextView
    private lateinit var filterUpcoming: TextView
    private lateinit var filterInProgress: TextView
    private lateinit var filterCompleted: TextView
    private var jobsListener: ListenerRegistration? = null
    private var cleanerId: String = ""
    private val knownJobIds = mutableSetOf<String>()
    private var hasObservedJobs = false
    private var allJobs: List<Order> = emptyList()
    private var selectedFilter: String = FILTER_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cleaner_job_board)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcomeCleaner)
        val btnAvailability = findViewById<MaterialButton>(R.id.btnAvailability)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        emptyState = findViewById(R.id.emptyState)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)
        tvCountAll = findViewById(R.id.tvCountAll)
        tvCountUpcoming = findViewById(R.id.tvCountUpcoming)
        tvCountInProgress = findViewById(R.id.tvCountInProgress)
        filterAll = findViewById(R.id.filterAll)
        filterUpcoming = findViewById(R.id.filterUpcoming)
        filterInProgress = findViewById(R.id.filterInProgress)
        filterCompleted = findViewById(R.id.filterCompleted)
        
        // We will dynamically use layoutOrders here.
        layoutOrders = findViewById(R.id.layoutOrders)
        setupFilters()

        val currentUserId = FirebaseAuthRepository.currentUserId

        FirestoreRepository.getUserProfile(currentUserId) { user ->
            runOnUiThread {
                tvWelcome.text = "Hello, ${user?.fullName ?: "Cleaner"}"
            }
        }

        setupBottomNavigation()

        FirestoreRepository.cleanersCol.whereEqualTo("authUid", currentUserId).get()
            .addOnSuccessListener { snapshot ->
                cleanerId = if (!snapshot.isEmpty) snapshot.documents.first().id else currentUserId
                listenForJobs()
            }

        btnAvailability.setOnClickListener {
            startActivity(Intent(this, CleanerAvailabilityActivity::class.java))
        }

        btnLogout.setOnClickListener {
            FirebaseAuthRepository.logout()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun setupFilters() {
        val filterViews = listOf(filterAll, filterUpcoming, filterInProgress, filterCompleted)
        filterViews.forEach { view ->
            view.setOnClickListener {
                selectedFilter = when (view.id) {
                    R.id.filterUpcoming -> FILTER_UPCOMING
                    R.id.filterInProgress -> FILTER_IN_PROGRESS
                    R.id.filterCompleted -> FILTER_COMPLETED
                    else -> FILTER_ALL
                }
                applyFilterStyles()
                renderJobs()
            }
        }
        applyFilterStyles()
    }

    private fun applyFilterStyles() {
        listOf(filterAll, filterUpcoming, filterInProgress, filterCompleted).forEach { chip ->
            val active = when (chip.id) {
                R.id.filterUpcoming -> selectedFilter == FILTER_UPCOMING
                R.id.filterInProgress -> selectedFilter == FILTER_IN_PROGRESS
                R.id.filterCompleted -> selectedFilter == FILTER_COMPLETED
                else -> selectedFilter == FILTER_ALL
            }
            chip.setBackgroundResource(if (active) R.drawable.bg_filter_chip_active else R.drawable.bg_filter_chip_idle)
            chip.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (active) R.color.white else R.color.slate_600
                )
            )
        }
    }

    private fun listenForJobs() {
        jobsListener = FirestoreRepository.ordersCol
            .whereEqualTo("cleanerId", cleanerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    runOnUiThread {
                        emptyState.visibility = View.VISIBLE
                        layoutOrders.visibility = View.GONE
                    }
                    return@addSnapshotListener
                }

                val jobs = snapshot.toObjects(Order::class.java).sortedByDescending { it.timestamp }
                allJobs = jobs
                if (hasObservedJobs) {
                    jobs.filter { it.id.isNotBlank() && it.id !in knownJobIds }.forEach { order ->
                        NotificationHelper.notifyNewCleanerJob(this, order)
                    }
                }
                knownJobIds.clear()
                knownJobIds.addAll(jobs.map { it.id })
                hasObservedJobs = true

                runOnUiThread {
                    renderJobs()
                }
            }
    }

    private fun renderJobs() {
        val filteredJobs = when (selectedFilter) {
            FILTER_UPCOMING -> allJobs.filter { it.status == FILTER_UPCOMING }
            FILTER_IN_PROGRESS -> allJobs.filter { it.status == FILTER_IN_PROGRESS }
            FILTER_COMPLETED -> allJobs.filter { it.status == FILTER_COMPLETED }
            else -> allJobs
        }

        tvCountAll.text = allJobs.size.toString()
        tvCountUpcoming.text = allJobs.count { it.status == FILTER_UPCOMING }.toString()
        tvCountInProgress.text = allJobs.count { it.status == FILTER_IN_PROGRESS }.toString()

        layoutOrders.removeAllViews()
        if (filteredJobs.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            layoutOrders.visibility = View.GONE
            tvEmptyTitle.text = when (selectedFilter) {
                FILTER_UPCOMING -> "No upcoming jobs"
                FILTER_IN_PROGRESS -> "No jobs in progress"
                FILTER_COMPLETED -> "No completed jobs yet"
                else -> "No assigned jobs yet"
            }
            tvEmptySubtitle.text = if (selectedFilter == FILTER_ALL) {
                "New bookings will appear here right away"
            } else {
                "Try switching to another filter"
            }
            return
        }

        emptyState.visibility = View.GONE
        layoutOrders.visibility = View.VISIBLE

        val serviceMap = MockData.MOCK_SERVICES.associateBy { it.id }
        filteredJobs.forEach { order ->
            val serviceName = serviceMap[order.serviceId]?.name ?: "Service"
            layoutOrders.addView(createOrderView(order, serviceName))
        }
    }

    private fun createOrderView(order: Order, serviceName: String): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_booking, layoutOrders, false)

        val tvServiceTitle   = view.findViewById<TextView>(R.id.tvServiceTitle)
        val tvServiceInitial = view.findViewById<TextView>(R.id.tvServiceInitial)
        val tvDate           = view.findViewById<TextView>(R.id.tvDate)
        val tvTime           = view.findViewById<TextView>(R.id.tvTime)
        val tvPrice          = view.findViewById<TextView>(R.id.tvPrice)
        val chipStatus       = view.findViewById<TextView>(R.id.chipStatus)
        val imgService       = view.findViewById<FrameLayout>(R.id.imgService)
        val btnRebook        = view.findViewById<TextView>(R.id.btnRebook)

        tvServiceTitle.text   = serviceName
        tvServiceInitial.text = serviceName.firstOrNull()?.toString() ?: "S"
        tvDate.text           = order.date
        tvTime.text           = order.time
        tvPrice.text          = "$${"%.2f".format(order.totalPrice)}"
        chipStatus.text       = order.status

        // Hide Rebook button for cleaner
        btnRebook.visibility = View.GONE

        // Color by Service
        val (circleBg, initialColor) = getServiceIconStyle(order.serviceId)
        imgService.setBackgroundResource(circleBg)
        tvServiceInitial.setTextColor(ContextCompat.getColor(this, initialColor))

        // Color by Status
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
        }

        // Tap -> open Job Detail
        view.setOnClickListener {
            val intent = Intent(this, CleanerJobDetailActivity::class.java)
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

    private fun getServiceIconStyle(serviceId: String): Pair<Int, Int> = when (serviceId) {
        "s1" -> Pair(R.drawable.bg_circle_blue,   R.color.blue_700)
        "s2" -> Pair(R.drawable.bg_circle_teal,   R.color.teal_500)
        "s3" -> Pair(R.drawable.bg_circle_orange, R.color.orange_700)
        "s4" -> Pair(R.drawable.bg_circle_indigo, R.color.indigo_700)
        else -> Pair(R.drawable.bg_circle_blue,   R.color.blue_700)
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_cleaner_jobs
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_cleaner_jobs -> true
                R.id.nav_cleaner_chats -> {
                    startActivity(Intent(this, CleanerDashboardActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jobsListener?.remove()
        knownJobIds.clear()
    }

    companion object {
        private const val FILTER_ALL = "All"
        private const val FILTER_UPCOMING = "Upcoming"
        private const val FILTER_IN_PROGRESS = "In Progress"
        private const val FILTER_COMPLETED = "Completed"
    }
}

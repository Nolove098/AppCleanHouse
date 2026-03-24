package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
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
    private var jobsListener: ListenerRegistration? = null
    private var cleanerId: String = ""
    private val knownJobIds = mutableSetOf<String>()
    private var hasObservedJobs = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cleaner_job_board)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcomeCleaner)
        val btnAvailability = findViewById<MaterialButton>(R.id.btnAvailability)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        val emptyState = findViewById<View>(R.id.emptyState)
        
        // We will dynamically use layoutOrders here.
        layoutOrders = findViewById(R.id.layoutOrders)

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
                listenForJobs(emptyState)
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

    private fun listenForJobs(emptyState: View) {
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
                if (hasObservedJobs) {
                    jobs.filter { it.id.isNotBlank() && it.id !in knownJobIds }.forEach { order ->
                        NotificationHelper.notifyNewCleanerJob(this, order)
                    }
                }
                knownJobIds.clear()
                knownJobIds.addAll(jobs.map { it.id })
                hasObservedJobs = true

                runOnUiThread {
                    layoutOrders.removeAllViews()
                    if (jobs.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        layoutOrders.visibility = View.GONE
                    } else {
                        emptyState.visibility = View.GONE
                        layoutOrders.visibility = View.VISIBLE
                        
                        val serviceMap = MockData.MOCK_SERVICES.associateBy { it.id }
                        for (order in jobs) {
                            val serviceName = serviceMap[order.serviceId]?.name ?: "Service"
                            layoutOrders.addView(createOrderView(order, serviceName))
                        }
                    }
                }
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

        tvServiceTitle.text   = "$serviceName • $cleanerId"
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
}

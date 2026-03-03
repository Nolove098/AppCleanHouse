package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class BookingHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_history)

        // =========================
        // VIEW BINDING
        // =========================

        val tabUpcoming = findViewById<TextView>(R.id.tabUpcoming)
        val tabHistory = findViewById<TextView>(R.id.tabHistory)

        val layoutUpcoming = findViewById<LinearLayout>(R.id.layoutUpcoming)
        val layoutHistory = findViewById<LinearLayout>(R.id.layoutHistory)

        val cardUpcoming = findViewById<MaterialCardView>(R.id.cardUpcoming)
        val cardCompleted = findViewById<MaterialCardView>(R.id.cardCompleted)
        val cardCancelled = findViewById<MaterialCardView>(R.id.cardCancelled)

        val btnRate = findViewById<MaterialButton>(R.id.btnRate)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // =========================
        // DEFAULT STATE → UPCOMING
        // =========================

        showUpcoming(tabUpcoming, tabHistory, layoutUpcoming, layoutHistory)

        // =========================
        // TAB CLICK
        // =========================

        tabUpcoming.setOnClickListener {
            showUpcoming(tabUpcoming, tabHistory, layoutUpcoming, layoutHistory)
        }

        tabHistory.setOnClickListener {
            showHistory(tabUpcoming, tabHistory, layoutUpcoming, layoutHistory)
        }

        // =========================
        // UPCOMING CLICK → DETAIL
        // =========================

        cardUpcoming.setOnClickListener {
            startActivity(Intent(this, BookingDetailActivity::class.java))
        }

        // =========================
        // COMPLETED
        // =========================

        // Không cho bấm vào card
        cardCompleted.isClickable = false
        cardCompleted.isFocusable = false

        // Nút Rate
        btnRate.setOnClickListener {
            startActivity(Intent(this, RatingActivity::class.java))
        }

        // =========================
        // CANCELLED
        // =========================

        cardCancelled.isClickable = false
        cardCancelled.isFocusable = false

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        bottomNav.selectedItemId = R.id.nav_bookings

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_bookings -> true

                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }

    // =========================
    // SHOW UPCOMING
    // =========================

    private fun showUpcoming(
        tabUpcoming: TextView,
        tabHistory: TextView,
        layoutUpcoming: LinearLayout,
        layoutHistory: LinearLayout
    ) {

        layoutUpcoming.visibility = View.VISIBLE
        layoutHistory.visibility = View.GONE

        tabUpcoming.setBackgroundResource(R.drawable.bg_tab_active)
        tabUpcoming.setTextColor(getColor(android.R.color.white))

        tabHistory.setBackgroundResource(R.drawable.bg_tab_inactive)
        tabHistory.setTextColor(getColor(R.color.text_secondary))
    }

    // =========================
    // SHOW HISTORY
    // =========================

    private fun showHistory(
        tabUpcoming: TextView,
        tabHistory: TextView,
        layoutUpcoming: LinearLayout,
        layoutHistory: LinearLayout
    ) {

        layoutUpcoming.visibility = View.GONE
        layoutHistory.visibility = View.VISIBLE

        tabHistory.setBackgroundResource(R.drawable.bg_tab_active)
        tabHistory.setTextColor(getColor(android.R.color.white))

        tabUpcoming.setBackgroundResource(R.drawable.bg_tab_inactive)
        tabUpcoming.setTextColor(getColor(R.color.text_secondary))
    }
}

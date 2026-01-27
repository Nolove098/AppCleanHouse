package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        // Mock interaction: Launch Detail on Card Click
        val cardBooking1 = findViewById<MaterialCardView>(R.id.cardBooking1)
        cardBooking1.setOnClickListener {
            startActivity(Intent(this, BookingDetailActivity::class.java))
        }

        // Setup Tabs (Mock Logic)
        val tabUpcoming = findViewById<TextView>(R.id.tabUpcoming)
        val tabHistory = findViewById<TextView>(R.id.tabHistory)

        tabUpcoming.setOnClickListener {
            tabUpcoming.setBackgroundResource(R.drawable.bg_tab_active)
            tabUpcoming.setTextColor(getColor(android.R.color.white))
            tabHistory.setBackgroundResource(R.drawable.bg_tab_inactive)
            tabHistory.setTextColor(getColor(R.color.text_secondary)) // Use actual color resource
        }

        tabHistory.setOnClickListener {
            tabHistory.setBackgroundResource(R.drawable.bg_tab_active)
            tabHistory.setTextColor(getColor(android.R.color.white))
            tabUpcoming.setBackgroundResource(R.drawable.bg_tab_inactive)
            tabUpcoming.setTextColor(getColor(R.color.text_secondary))
        }
    }
}

package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_bookings -> {
                    startActivity(Intent(this, BookingHistoryActivity::class.java))
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


        // Mock interaction: Launch Service Detail when "Deep Clean" card is clicked
        val cardDeepClean = findViewById<androidx.cardview.widget.CardView>(R.id.cardServiceDeepClean)
        cardDeepClean.setOnClickListener {
             startActivity(Intent(this, ServiceDetailActivity::class.java))
        }
    }
}

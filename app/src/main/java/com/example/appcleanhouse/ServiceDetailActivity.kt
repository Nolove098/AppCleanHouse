package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.data.MockData
import com.google.android.material.button.MaterialButton

class ServiceDetailActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_detail)

        val serviceId = intent.getStringExtra("SERVICE_ID") ?: "s1"
        val service = MockData.MOCK_SERVICES.find { it.id == serviceId } ?: MockData.MOCK_SERVICES[0]

        // Initialize views
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val ivHeroImage = findViewById<ImageView>(R.id.ivHeroImage)
        val tvServiceName = findViewById<TextView>(R.id.tvServiceName)
        val tvPrice = findViewById<TextView>(R.id.tvPrice)
        val tvRating = findViewById<TextView>(R.id.tvRating)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val btnBookNow = findViewById<MaterialButton>(R.id.btnBookNow)

        // Set service data
        tvServiceName.text = service.name
        tvPrice.text = "$${service.pricePerHour}"
        tvRating.text = service.rating.toString()
        tvDescription.text = service.description

        // Set hero image based on service
        val heroImageRes = when (service.id) {
            "s1" -> R.drawable.deepcleaning
            "s2" -> R.drawable.deepcleaning // Replace with standard cleaning image if available
            "s3" -> R.drawable.deepcleaning // Replace with laundry image if available
            "s4" -> R.drawable.deepcleaning // Replace with carpet image if available
            else -> R.drawable.deepcleaning
        }
        ivHeroImage.setImageResource(heroImageRes)

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Book Now button
        btnBookNow.setOnClickListener {
            val intent = Intent(this, BookingActivity::class.java)
            intent.putExtra("SERVICE_ID", service.id)
            intent.putExtra("SERVICE_NAME", service.name)
            intent.putExtra("SERVICE_PRICE", service.pricePerHour)
            startActivity(intent)
        }
    }
}

package com.example.appcleanhouse

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appcleanhouse.models.Review
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository

class RatingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        val orderId = intent.getStringExtra("ORDER_ID").orEmpty()
        val cleanerId = intent.getStringExtra("CLEANER_ID").orEmpty()
        val cleanerName = intent.getStringExtra("CLEANER_NAME").orEmpty()
        val serviceId = intent.getStringExtra("SERVICE_ID").orEmpty()

        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val edtComment = findViewById<EditText>(R.id.edtComment)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = edtComment.text.toString().trim()

            if (orderId.isBlank() || cleanerId.isBlank()) {
                Toast.makeText(this, "Thiếu thông tin đơn hàng để đánh giá", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rating <= 0) {
                Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUserId = FirebaseAuthRepository.currentUserId
            if (currentUserId.isBlank()) {
                Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            FirestoreRepository.getUserProfile(currentUserId) { user ->
                val review = Review(
                    id = orderId,
                    orderId = orderId,
                    customerId = currentUserId,
                    customerName = user?.fullName.orEmpty(),
                    cleanerId = cleanerId,
                    cleanerName = cleanerName,
                    serviceId = serviceId,
                    rating = rating,
                    comment = comment
                )

                FirestoreRepository.saveReview(
                    review = review,
                    onSuccess = {
                        runOnUiThread {
                            Toast.makeText(this, "Rated $rating ⭐", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            btnSubmit.isEnabled = true
                            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

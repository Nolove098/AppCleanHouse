package com.example.appcleanhouse

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RatingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val edtComment = findViewById<EditText>(R.id.edtComment)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            val comment = edtComment.text.toString()

            Toast.makeText(this, "Rated $rating ⭐", Toast.LENGTH_SHORT).show()

            // TODO: Save rating to database later

            finish() // quay lại BookingHistory
        }
    }
}

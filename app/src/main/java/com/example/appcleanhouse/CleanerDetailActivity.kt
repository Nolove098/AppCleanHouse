package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.models.CleanerAvailability
import com.example.appcleanhouse.models.Review
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class CleanerDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cleaner_detail)

        val cleanerId = intent.getStringExtra("CLEANER_ID") ?: "c1"
        val cleaner = MockData.MOCK_CLEANERS.find { it.id == cleanerId } ?: MockData.MOCK_CLEANERS.first()
        val tvDetailRating = findViewById<TextView>(R.id.tvDetailRating)
        val tvAvailabilitySummary = findViewById<TextView>(R.id.tvAvailabilitySummary)
        val tvReviewSummary = findViewById<TextView>(R.id.tvReviewSummary)
        val layoutReviewList = findViewById<LinearLayout>(R.id.layoutReviewList)

        findViewById<ImageButton>(R.id.btnBackCleanerDetail).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.ivCleanerHero).setImageResource(cleaner.avatarResId)
        findViewById<TextView>(R.id.tvCleanerDetailName).text = cleaner.name
        tvDetailRating.text = String.format("%.1f", cleaner.rating)
        findViewById<TextView>(R.id.tvDetailJobs).text = cleaner.jobCount.toString()
        findViewById<TextView>(R.id.tvDetailExperience).text = cleaner.experience
        findViewById<TextView>(R.id.tvAboutTitle).text = "About ${cleaner.name.substringBefore(" ")}"
        findViewById<TextView>(R.id.tvAboutCleaner).text = cleaner.about
        findViewById<TextView>(R.id.tvCleanerPrice).text = "$${cleaner.pricePerHour} / hour"

        FirestoreRepository.getCleanerAvailability(
            cleanerId = cleaner.id,
            onResult = { availability ->
                runOnUiThread {
                    tvAvailabilitySummary.text = buildAvailabilitySummary(availability)
                }
            },
            onFailure = {
                runOnUiThread {
                    tvAvailabilitySummary.text = buildAvailabilitySummary(CleanerAvailability(cleanerId = cleaner.id))
                }
            }
        )

        FirestoreRepository.getReviewsForCleaner(
            cleanerId = cleaner.id,
            onResult = { reviews ->
                runOnUiThread {
                    bindReviews(
                        reviews = reviews,
                        fallbackRating = cleaner.rating,
                        tvDetailRating = tvDetailRating,
                        tvReviewSummary = tvReviewSummary,
                        layoutReviewList = layoutReviewList
                    )
                }
            },
            onFailure = {
                runOnUiThread {
                    bindReviews(
                        reviews = emptyList(),
                        fallbackRating = cleaner.rating,
                        tvDetailRating = tvDetailRating,
                        tvReviewSummary = tvReviewSummary,
                        layoutReviewList = layoutReviewList
                    )
                }
            }
        )

        val firstName = cleaner.name.substringBefore(" ")
        val btnBook = findViewById<MaterialButton>(R.id.btnBookCleaner)
        btnBook.text = "Book $firstName"

        // Setup Service Spinner
        val spinnerService = findViewById<android.widget.Spinner>(R.id.spinnerService)
        val services = MockData.MOCK_SERVICES
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, services.map { it.name })
        spinnerService.adapter = adapter
        
        var selectedServiceId = "s1"
        spinnerService.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedServiceId = services[position].id
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        btnBook.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java).apply {
                putExtra("SERVICE_ID", selectedServiceId)
                putExtra("SERVICE_PRICE", cleaner.pricePerHour)
                putExtra("CLEANER_ID", cleaner.id)
            })
        }
    }

    private fun bindReviews(
        reviews: List<Review>,
        fallbackRating: Double,
        tvDetailRating: TextView,
        tvReviewSummary: TextView,
        layoutReviewList: LinearLayout
    ) {
        layoutReviewList.removeAllViews()

        if (reviews.isEmpty()) {
            tvDetailRating.text = String.format("%.1f", fallbackRating)
            tvReviewSummary.text = "No reviews yet"
            layoutReviewList.addView(createReviewCard("No customer feedback yet. Be the first to leave a review after a completed booking."))
            return
        }

        val averageRating = reviews.map { it.rating }.average()
        tvDetailRating.text = String.format("%.1f", averageRating)
        tvReviewSummary.text = "${reviews.size} review${if (reviews.size > 1) "s" else ""} from customers"

        reviews.take(3).forEach { review ->
            val cardText = buildString {
                append(review.customerName.ifBlank { "Customer" })
                append(" • ")
                append(review.rating)
                append("/5")
                if (review.comment.isNotBlank()) {
                    append("\n\n")
                    append(review.comment)
                }
            }
            layoutReviewList.addView(createReviewCard(cardText))
        }
    }

    private fun createReviewCard(content: String): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = 20f
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = (12 * resources.displayMetrics.density).toInt()
            layoutParams = params
        }

        val textView = TextView(this).apply {
            text = content
            setPadding(
                (16 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt()
            )
            setTextColor(getColor(R.color.slate_600))
            textSize = 13f
        }

        card.addView(textView)
        return card
    }

    private fun buildAvailabilitySummary(availability: CleanerAvailability): String {
        val upcomingEntries = CleanerAvailability
            .normalizeAvailabilityMap(availability.availabilityByDate)
            .filterValues { it.isNotEmpty() }
            .entries
            .take(3)

        if (upcomingEntries.isEmpty()) {
            return "No open availability right now"
        }

        return upcomingEntries.joinToString(" • ") { entry ->
            "${CleanerAvailability.storageKeyToDisplay(entry.key)}: ${entry.value.joinToString(", ")}"
        }
    }
}

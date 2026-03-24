package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.adapter.TopCleanerAdapter
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.models.Cleaner
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.button.MaterialButton

class TopCleanersActivity : AppCompatActivity() {

    private lateinit var adapter: TopCleanerAdapter
    private var baseList: List<Cleaner> = MockData.MOCK_CLEANERS
    private var currentFilter: String = "Highest Rated"
    private var cleanersListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_cleaners)

        findViewById<ImageButton>(R.id.btnBackCleanerList).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvTopCleaners)
        adapter = TopCleanerAdapter(baseList.toMutableList()) { cleaner ->
            startActivity(Intent(this, CleanerDetailActivity::class.java).apply {
                putExtra("CLEANER_ID", cleaner.id)
            })
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val btnHighest = findViewById<MaterialButton>(R.id.filterHighestRated)
        val btnNearest = findViewById<MaterialButton>(R.id.filterNearest)
        val btnLowestPrice = findViewById<MaterialButton>(R.id.filterLowestPrice)
        val btnMostExperienced = findViewById<MaterialButton>(R.id.filterMostExperienced)

        val allButtons = listOf(btnHighest, btnNearest, btnLowestPrice, btnMostExperienced)

        fun setActive(active: MaterialButton) {
            allButtons.forEach {
                val isActive = it == active
                it.setBackgroundColor(if (isActive) 0xFF80CBC4.toInt() else 0xFFB4EBE6.toInt())
                it.setTextColor(if (isActive) 0xFFFFFFFF.toInt() else 0xFF475569.toInt())
            }
        }

        btnHighest.setOnClickListener {
            setActive(btnHighest)
            currentFilter = "Highest Rated"
            renderCurrentFilter()
        }

        btnNearest.setOnClickListener {
            setActive(btnNearest)
            currentFilter = "Nearest"
            renderCurrentFilter()
        }

        btnLowestPrice.setOnClickListener {
            setActive(btnLowestPrice)
            currentFilter = "Lowest Price"
            renderCurrentFilter()
        }

        btnMostExperienced.setOnClickListener {
            setActive(btnMostExperienced)
            currentFilter = "Most Experienced"
            renderCurrentFilter()
        }

        btnHighest.performClick()
    }

    override fun onStart() {
        super.onStart()
        cleanersListener = FirestoreRepository.listenCleaners(
            onResult = { firestoreCleaners ->
                val source = if (firestoreCleaners.isEmpty()) MockData.MOCK_CLEANERS else firestoreCleaners
                baseList = source.map { enrichCleaner(it) }
                runOnUiThread { renderCurrentFilter() }
            },
            onFailure = {
                baseList = MockData.MOCK_CLEANERS
                runOnUiThread { renderCurrentFilter() }
            }
        )
    }

    override fun onStop() {
        cleanersListener?.remove()
        cleanersListener = null
        super.onStop()
    }

    private fun renderCurrentFilter() {
        val sorted = when (currentFilter) {
            "Nearest" -> baseList.sortedBy { it.distanceKm }
            "Lowest Price" -> baseList.sortedBy { it.pricePerHour }
            "Most Experienced" -> baseList.sortedByDescending { it.experience.substringBefore(" ").toIntOrNull() ?: 0 }
            else -> baseList.sortedByDescending { it.rating }
        }
        adapter.submitData(sorted)
    }

    private fun enrichCleaner(raw: Cleaner): Cleaner {
        val mock = MockData.MOCK_CLEANERS.find { it.id == raw.id }
        return raw.copy(
            avatarResId = if (raw.avatarResId != 0) raw.avatarResId else mock?.avatarResId ?: R.drawable.idol1,
            specialty = raw.specialty.ifEmpty { mock?.specialty ?: "Professional Cleaner" },
            experience = raw.experience.ifEmpty { mock?.experience ?: "2 yrs" },
            about = raw.about.ifEmpty { mock?.about ?: "Professional cleaner with attention to detail." },
            pricePerHour = if (raw.pricePerHour > 0) raw.pricePerHour else mock?.pricePerHour ?: 45,
            tags = if (raw.tags.isNotEmpty()) raw.tags else (mock?.tags ?: listOf("Clean", "Reliable")),
            distanceKm = if (raw.distanceKm > 0.0) raw.distanceKm else (mock?.distanceKm ?: 3.0)
        )
    }
}

package com.example.appcleanhouse

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.models.CleanerAvailability
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CleanerAvailabilityActivity : AppCompatActivity() {

    private lateinit var dateContainer: LinearLayout
    private lateinit var timeContainer: android.widget.GridLayout
    private lateinit var tvDateAvailabilityHint: TextView
    private lateinit var tvTimeAvailabilityHint: TextView
    private lateinit var btnSave: MaterialButton
    private var cleanerId: String? = null
    private var selectedDate: Date? = null
    private var reservedTimeSlots: Set<String> = emptySet()
    private var availabilityByDate: MutableMap<String, MutableSet<String>> = CleanerAvailability
        .upcomingEmptyAvailabilityMap()
        .mapValues { (_, slots) -> slots.toMutableSet() }
        .toMutableMap()

    private val timeSlots = CleanerAvailability.DEFAULT_TIME_SLOTS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cleaner_availability)

        findViewById<ImageButton>(R.id.btnBackAvailability).setOnClickListener { finish() }
        dateContainer = findViewById(R.id.dateContainer)
        timeContainer = findViewById(R.id.timeContainer)
        tvDateAvailabilityHint = findViewById(R.id.tvDateAvailabilityHint)
        tvTimeAvailabilityHint = findViewById(R.id.tvTimeAvailabilityHint)
        btnSave = findViewById(R.id.btnSaveAvailability)

        btnSave.setOnClickListener { saveAvailability() }
        populateDates()
        populateTimeSlots()
        loadCleanerAvailability()
    }

    private fun loadCleanerAvailability() {
        val authUid = FirebaseAuthRepository.currentUserId
        if (authUid.isBlank()) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        FirestoreRepository.getCleanerIdByAuthUid(
            authUid = authUid,
            onResult = { resolvedCleanerId ->
                if (resolvedCleanerId.isNullOrBlank()) {
                    runOnUiThread {
                        Toast.makeText(this, "Không tìm thấy hồ sơ cleaner", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@getCleanerIdByAuthUid
                }

                cleanerId = resolvedCleanerId
                FirestoreRepository.getCleanerAvailability(
                    cleanerId = resolvedCleanerId,
                    onResult = { availability ->
                        runOnUiThread {
                            bindAvailability(availability)
                            loadReservedTimeSlotsForSelectedDate()
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                            bindAvailability(
                                CleanerAvailability(
                                    cleanerId = resolvedCleanerId,
                                    availabilityByDate = emptyMap()
                                )
                            )
                            loadReservedTimeSlotsForSelectedDate()
                        }
                    }
                )
            },
            onFailure = { error ->
                runOnUiThread { Toast.makeText(this, error, Toast.LENGTH_SHORT).show() }
            }
        )
    }

    private fun bindAvailability(availability: CleanerAvailability) {
        availabilityByDate = CleanerAvailability
            .mergeWithUpcomingDates(availability.availabilityByDate)
            .mapValues { (_, slots) -> slots.toMutableSet() }
            .toMutableMap()
        if (selectedDate == null) {
            selectedDate = parseBookingDate(CleanerAvailability.upcomingBookingDates().first())
        }
        populateDates()
        populateTimeSlots()
        updateAvailabilityHints()
    }

    private fun saveAvailability() {
        val resolvedCleanerId = cleanerId
        if (resolvedCleanerId.isNullOrBlank()) {
            Toast.makeText(this, "Cleaner chưa sẵn sàng", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Đang lưu..."

        FirestoreRepository.saveCleanerAvailability(
            availability = CleanerAvailability(
                cleanerId = resolvedCleanerId,
                availabilityByDate = availabilityByDate.mapValues { (_, slots) -> slots.toList().sorted() }
            ),
            onSuccess = {
                runOnUiThread {
                    btnSave.isEnabled = true
                    btnSave.text = "Save Availability"
                    Toast.makeText(this, "Đã lưu lịch rảnh", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    btnSave.isEnabled = true
                    btnSave.text = "Save Availability"
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun populateDates() {
        dateContainer.removeAllViews()
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        var firstSelectableDate: Date? = null

        repeat(7) {
            val date = calendar.time
            val dateKey = formatBookingDate(date)
            if (firstSelectableDate == null) {
                firstSelectableDate = date
            }
            dateContainer.addView(createDateButton(date, dateKey, dayFormat.format(date), calendar.get(Calendar.DAY_OF_MONTH)))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (selectedDate == null) {
            selectedDate = firstSelectableDate
        }
        updateDateButtons()
    }

    private fun createDateButton(date: Date, dateKey: String, dayName: String, dayNumber: Int): MaterialCardView {
        val card = MaterialCardView(this).apply {
            tag = dateKey
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.date_button_size),
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.card_margin_small)
            }
            radius = resources.getDimension(R.dimen.card_corner_radius)
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeWidth = 2
            strokeColor = ContextCompat.getColor(context, R.color.slate_200)
            isClickable = true
            isFocusable = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padding = resources.getDimensionPixelSize(R.dimen.card_padding_small)
            setPadding(padding, padding, padding, padding)
        }

        val tvDay = TextView(this).apply {
            text = dayName
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.slate_600))
            gravity = Gravity.CENTER
            alpha = 0.8f
        }

        val tvDate = TextView(this).apply {
            text = dayNumber.toString()
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.slate_600))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        container.addView(tvDay)
        container.addView(tvDate)
        card.addView(container)

        card.setOnClickListener {
            selectedDate = date
            selectedTimeSlotsForDate()
            updateDateButtons()
            loadReservedTimeSlotsForSelectedDate()
        }

        return card
    }

    private fun updateDateButtons() {
        val selectedDateKey = selectedDate?.let(::formatBookingDate)
        for (index in 0 until dateContainer.childCount) {
            val card = dateContainer.getChildAt(index) as MaterialCardView
            val cardDateKey = card.tag as? String ?: continue
            val isSelected = cardDateKey == selectedDateKey
            val openSlots = availabilityByDate[cardDateKey].orEmpty()
            val container = card.getChildAt(0) as LinearLayout
            val textColor = if (isSelected) R.color.white else R.color.slate_600

            if (isSelected) {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blue_600))
                card.strokeWidth = 0
                card.cardElevation = resources.getDimension(R.dimen.card_elevation_medium)
                card.alpha = 1f
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
                card.strokeWidth = 2
                card.strokeColor = ContextCompat.getColor(this, R.color.slate_200)
                card.cardElevation = 0f
                card.alpha = if (openSlots.isEmpty()) 0.55f else 1f
            }

            for (j in 0 until container.childCount) {
                (container.getChildAt(j) as TextView).setTextColor(ContextCompat.getColor(this, textColor))
            }
        }
    }

    private fun populateTimeSlots() {
        timeContainer.removeAllViews()
        timeSlots.forEachIndexed { index, time ->
            val timeButton = createTimeButton(time)
            val params = android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = android.widget.GridLayout.spec(index % 2, 1f)
                rowSpec = android.widget.GridLayout.spec(index / 2)
                val margin = resources.getDimensionPixelSize(R.dimen.card_margin_small)
                setMargins(
                    if (index % 2 == 0) 0 else margin,
                    0,
                    if (index % 2 == 0) margin else 0,
                    margin * 2
                )
            }
            timeButton.layoutParams = params
            timeContainer.addView(timeButton)
        }
    }

    private fun createTimeButton(time: String): MaterialCardView {
        val isBooked = time in reservedTimeSlots
        val isOpen = time in selectedTimeSlotsForDate()
        val card = MaterialCardView(this).apply {
            radius = resources.getDimension(R.dimen.card_corner_radius)
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeWidth = 2
            strokeColor = ContextCompat.getColor(context, R.color.slate_200)
            isClickable = !isBooked
            isFocusable = !isBooked
            alpha = if (isBooked) 0.45f else 1f
        }

        val textView = TextView(this).apply {
            text = if (isBooked) "$time\nBooked" else time
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val padding = resources.getDimensionPixelSize(R.dimen.card_padding)
            setPadding(padding, padding, padding, padding)
        }
        card.addView(textView)

        if (isBooked) {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.slate_100))
            textView.setTextColor(ContextCompat.getColor(this, R.color.slate_400))
        } else if (isOpen) {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blue_600))
            card.strokeWidth = 0
            card.cardElevation = resources.getDimension(R.dimen.card_elevation_medium)
            textView.setTextColor(ContextCompat.getColor(this, R.color.white))
            card.setOnClickListener {
                toggleTimeSlot(time)
            }
        } else {
            textView.setTextColor(ContextCompat.getColor(this, R.color.slate_600))
            card.setOnClickListener {
                toggleTimeSlot(time)
            }
        }

        return card
    }

    private fun toggleTimeSlot(time: String) {
        val dateKey = selectedDate?.let(::formatBookingDate) ?: return
        if (time in reservedTimeSlots) return
        val selectedSlots = availabilityByDate.getOrPut(dateKey) { mutableSetOf() }
        if (!selectedSlots.add(time)) {
            selectedSlots.remove(time)
        }
        availabilityByDate[dateKey] = selectedSlots
        populateTimeSlots()
        updateDateButtons()
        updateAvailabilityHints()
    }

    private fun loadReservedTimeSlotsForSelectedDate() {
        val resolvedCleanerId = cleanerId ?: return
        val selected = selectedDate ?: return
        FirestoreRepository.getCleanerBookedOrders(
            cleanerId = resolvedCleanerId,
            date = formatBookingDate(selected),
            onResult = { orders ->
                runOnUiThread {
                    reservedTimeSlots = orders.map { it.time }.filter { it.isNotBlank() }.toSet()
                    populateTimeSlots()
                    updateAvailabilityHints()
                }
            },
            onFailure = {
                runOnUiThread {
                    reservedTimeSlots = emptySet()
                    populateTimeSlots()
                    updateAvailabilityHints()
                }
            }
        )
    }

    private fun selectedTimeSlotsForDate(): Set<String> {
        val dateKey = selectedDate?.let(::formatBookingDate) ?: return emptySet()
        return availabilityByDate[dateKey].orEmpty().toSet()
    }

    private fun updateAvailabilityHints() {
        val dateKey = selectedDate?.let(::formatBookingDate)
        val openSlots = selectedTimeSlotsForDate().filterNot { it in reservedTimeSlots }
        tvDateAvailabilityHint.text = if (dateKey == null) {
            "Choose a date to edit availability."
        } else {
            "Editing availability for ${CleanerAvailability.storageKeyToDisplay(dateKey)}"
        }
        tvTimeAvailabilityHint.text = when {
            dateKey == null -> ""
            openSlots.isEmpty() && reservedTimeSlots.isEmpty() -> "No open slots for this date. Tap a card to open it."
            openSlots.isEmpty() -> "All available slots are currently booked. They will reopen after the booking is completed or cancelled."
            reservedTimeSlots.isEmpty() -> "Open slots: ${openSlots.joinToString(", ")}"
            else -> "Open slots: ${openSlots.joinToString(", ")} • Booked: ${reservedTimeSlots.joinToString(", ")}"
        }
    }

    private fun formatBookingDate(date: Date): String {
        return CleanerAvailability.dateToStorageKey(date)
    }

    private fun parseBookingDate(date: String): Date {
        return CleanerAvailability.parseStorageKey(date) ?: Date()
    }
}
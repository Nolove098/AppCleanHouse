package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : AppCompatActivity() {
    private var currentStep = 1
    private var selectedDate: Date? = null
    private var selectedTime: String? = null
    private var selectedAddress: String = "123 Main St, New York"

    private lateinit var btnBack: ImageButton
    private lateinit var btnContinue: MaterialButton
    private lateinit var tvStepTitle: TextView
    private lateinit var step1Indicator: android.view.View
    private lateinit var step2Indicator: android.view.View
    private lateinit var step3Indicator: android.view.View
    private lateinit var step1Content: LinearLayout
    private lateinit var step2Content: LinearLayout
    private lateinit var dateContainer: LinearLayout
    private lateinit var timeContainer: GridLayout
    private lateinit var etAddress: EditText
    private lateinit var tvOrderDateTime: TextView

    private val timeSlots = listOf("09:00 AM", "11:00 AM", "02:00 PM", "04:30 PM")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        initViews()
        setupListeners()
        populateDates()
        populateTimeSlots()
        updateStepUI()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnContinue = findViewById(R.id.btnContinue)
        tvStepTitle = findViewById(R.id.tvStepTitle)
        step1Indicator = findViewById(R.id.step1)
        step2Indicator = findViewById(R.id.step2)
        step3Indicator = findViewById(R.id.step3)
        step1Content = findViewById(R.id.step1Content)
        step2Content = findViewById(R.id.step2Content)
        dateContainer = findViewById(R.id.dateContainer)
        timeContainer = findViewById(R.id.timeContainer)
        etAddress = findViewById(R.id.etAddress)
        tvOrderDateTime = findViewById(R.id.tvOrderDateTime)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            if (currentStep == 1) {
                finish()
            } else {
                currentStep--
                updateStepUI()
            }
        }

        btnContinue.setOnClickListener {
            when (currentStep) {
                1 -> {
                    if (selectedDate != null && selectedTime != null) {
                        currentStep = 2
                        updateStepUI()
                    }
                }
                2 -> {
                    // Navigate to success
                    val intent = Intent(this, BookingSuccessActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun populateDates() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        
        for (i in 0 until 5) {
            val date = calendar.time
            val dateButton = createDateButton(date, dateFormat.format(date), calendar.get(Calendar.DAY_OF_MONTH))
            dateContainer.addView(dateButton)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun createDateButton(date: Date, dayName: String, dayNumber: Int): MaterialCardView {
        val card = MaterialCardView(this).apply {
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
            alpha = 0.8f
            gravity = Gravity.CENTER
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
            updateDateButtons()
            updateContinueButton()
        }

        return card
    }

    private fun updateDateButtons() {
        for (i in 0 until dateContainer.childCount) {
            val card = dateContainer.getChildAt(i) as MaterialCardView
            val isSelected = i == getSelectedDateIndex()
            
            if (isSelected) {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blue_600))
                card.strokeWidth = 0
                card.cardElevation = resources.getDimension(R.dimen.card_elevation_medium)
                
                val container = card.getChildAt(0) as LinearLayout
                for (j in 0 until container.childCount) {
                    (container.getChildAt(j) as TextView).setTextColor(
                        ContextCompat.getColor(this, R.color.white)
                    )
                }
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
                card.strokeWidth = 2
                card.strokeColor = ContextCompat.getColor(this, R.color.slate_200)
                card.cardElevation = 0f
                
                val container = card.getChildAt(0) as LinearLayout
                for (j in 0 until container.childCount) {
                    (container.getChildAt(j) as TextView).setTextColor(
                        ContextCompat.getColor(this, R.color.slate_600)
                    )
                }
            }
        }
    }

    private fun getSelectedDateIndex(): Int {
        if (selectedDate == null) return -1
        val calendar = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply { time = selectedDate!! }
        
        for (i in 0 until 5) {
            if (calendar.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH) &&
                calendar.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)) {
                return i
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return -1
    }

    private fun populateTimeSlots() {
        timeSlots.forEachIndexed { index, time ->
            val timeButton = createTimeButton(time)
            
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2)
                
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
        val card = MaterialCardView(this).apply {
            radius = resources.getDimension(R.dimen.card_corner_radius)
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeWidth = 2
            strokeColor = ContextCompat.getColor(context, R.color.slate_200)
            isClickable = true
            isFocusable = true
        }

        val textView = TextView(this).apply {
            text = time
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.slate_600))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            val padding = resources.getDimensionPixelSize(R.dimen.card_padding)
            setPadding(padding, padding, padding, padding)
        }

        card.addView(textView)

        card.setOnClickListener {
            selectedTime = time
            updateTimeButtons()
            updateContinueButton()
        }

        return card
    }

    private fun updateTimeButtons() {
        for (i in 0 until timeContainer.childCount) {
            val card = timeContainer.getChildAt(i) as MaterialCardView
            val textView = card.getChildAt(0) as TextView
            val isSelected = textView.text == selectedTime
            
            if (isSelected) {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blue_600))
                card.strokeWidth = 0
                card.cardElevation = resources.getDimension(R.dimen.card_elevation_medium)
                textView.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
                card.strokeWidth = 2
                card.strokeColor = ContextCompat.getColor(this, R.color.slate_200)
                card.cardElevation = 0f
                textView.setTextColor(ContextCompat.getColor(this, R.color.slate_600))
            }
        }
    }

    private fun updateContinueButton() {
        btnContinue.isEnabled = when (currentStep) {
            1 -> selectedDate != null && selectedTime != null
            2 -> true
            else -> false
        }
    }

    private fun updateStepUI() {
        // Update title
        tvStepTitle.text = when (currentStep) {
            1 -> "Select Time"
            2 -> "Details"
            else -> "Confirm"
        }

        // Update step indicators
        updateStepIndicator(step1Indicator, currentStep >= 1)
        updateStepIndicator(step2Indicator, currentStep >= 2)
        updateStepIndicator(step3Indicator, currentStep >= 3)
        
        // Update step indicator widths
        val params1 = step1Indicator.layoutParams
        params1.width = if (currentStep == 1) dpToPx(32) else dpToPx(8)
        step1Indicator.layoutParams = params1
        
        val params2 = step2Indicator.layoutParams
        params2.width = if (currentStep == 2) dpToPx(32) else dpToPx(8)
        step2Indicator.layoutParams = params2

        // Update content visibility
        step1Content.visibility = if (currentStep == 1) android.view.View.VISIBLE else android.view.View.GONE
        step2Content.visibility = if (currentStep == 2) android.view.View.VISIBLE else android.view.View.GONE

        // Update button text
        btnContinue.text = when (currentStep) {
            1 -> "Continue"
            2 -> "Confirm Booking"
            else -> "Confirm"
        }

        // Update order summary if on step 2
        if (currentStep == 2) {
            updateOrderSummary()
        }

        updateContinueButton()
    }

    private fun updateStepIndicator(indicator: android.view.View, isActive: Boolean) {
        indicator.setBackgroundResource(
            if (isActive) R.drawable.bg_step_active else R.drawable.bg_step_inactive
        )
    }

    private fun updateOrderSummary() {
        if (selectedDate != null && selectedTime != null) {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val dateStr = dateFormat.format(selectedDate!!)
            tvOrderDateTime.text = "$dateStr at $selectedTime"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}

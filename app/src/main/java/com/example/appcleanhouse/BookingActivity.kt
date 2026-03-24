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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.models.CleanerAvailability
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
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
    private lateinit var step3Content: LinearLayout
    private lateinit var rgPaymentMethod: android.widget.RadioGroup
    private lateinit var tvOrderServicePrice: TextView
    private lateinit var tvOrderTax: TextView
    private lateinit var tvOrderTotal: TextView
    private lateinit var tvDateAvailabilityHint: TextView
    private lateinit var tvTimeAvailabilityHint: TextView

    private val timeSlots = listOf("09:00 AM", "11:00 AM", "02:00 PM", "04:30 PM")
    private var availabilityByDate: Map<String, List<String>> = CleanerAvailability.upcomingEmptyAvailabilityMap()
    private var reservedTimeSlots: Set<String> = emptySet()

    // ID service được truyền vào từ màn trước (mặc định "s1")
    private var serviceId: String = "s1"
    private var cleanerId: String = "c1"
    private var servicePrice: Int = 45

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        // Nhận serviceId từ Intent nếu có
        serviceId  = intent.getStringExtra("SERVICE_ID")  ?: "s1"
        cleanerId  = intent.getStringExtra("CLEANER_ID")  ?: "c1"
        servicePrice = intent.getIntExtra("SERVICE_PRICE", 45)

        initViews()
        setupListeners()
        populateDates()
        populateTimeSlots()
        loadCleanerAvailability()
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
        step3Content = findViewById(R.id.step3Content)
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod)
        tvOrderServicePrice = findViewById(R.id.tvOrderServicePrice)
        tvOrderTax = findViewById(R.id.tvOrderTax)
        tvOrderTotal = findViewById(R.id.tvOrderTotal)
        tvDateAvailabilityHint = findViewById(R.id.tvDateAvailabilityHint)
        tvTimeAvailabilityHint = findViewById(R.id.tvTimeAvailabilityHint)
    }

    private fun loadCleanerAvailability() {
        FirestoreRepository.getCleanerAvailability(
            cleanerId = cleanerId,
            onResult = { availability ->
                runOnUiThread {
                    availabilityByDate = CleanerAvailability.mergeWithUpcomingDates(availability.availabilityByDate)
                    refreshAvailabilityUi()
                    loadReservedTimeSlotsForSelectedDate()
                }
            },
            onFailure = {
                runOnUiThread {
                    availabilityByDate = CleanerAvailability.upcomingEmptyAvailabilityMap()
                    refreshAvailabilityUi()
                    loadReservedTimeSlotsForSelectedDate()
                }
            }
        )
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
                    currentStep = 3
                    updateStepUI()
                }
                3 -> {
                    // ── Lưu đơn hàng lên Firestore ─────────────────
                    confirmBookingToFirestore()
                }
            }
        }
        
        rgPaymentMethod.setOnCheckedChangeListener { _, _ ->
            updateContinueButton()
        }
    }

    /**
     * Tạo Order object và lưu lên Firestore khi user nhấn "Confirm Booking"
     */
    private fun confirmBookingToFirestore() {
        val userId = FirebaseAuthRepository.currentUserId
        if (userId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
            return
        }

        val dateStr = selectedDate?.let { formatBookingDate(it) } ?: ""
        val address = etAddress.text.toString().ifEmpty { selectedAddress }

        val hours = 3
        val subtotal = servicePrice * hours
        val tax = 5.0
        val total = subtotal + tax
        val selectedCleanerName = MockData.MOCK_CLEANERS
            .find { it.id == cleanerId }
            ?.name
            ?: "Cleaner"

        val selectedSlot = selectedTime
        if (dateStr.isBlank() || selectedSlot.isNullOrBlank()) {
            Toast.makeText(this, "Vui lòng chọn ngày và giờ hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isDateAvailable(selectedDate ?: Date()) || !isTimeSlotSelectable(selectedSlot)) {
            Toast.makeText(this, "Khung giờ này hiện không còn khả dụng", Toast.LENGTH_SHORT).show()
            return
        }

        btnContinue.isEnabled = false
        btnContinue.text = "Đang xử lý..."

        FirestoreRepository.getCleanerBookedOrders(
            cleanerId = cleanerId,
            date = dateStr,
            onResult = { orders ->
                runOnUiThread {
                    val conflictingSlots = orders.map { it.time }.toSet()
                    if (selectedSlot in conflictingSlots) {
                        reservedTimeSlots = conflictingSlots
                        selectedTime = null
                        populateTimeSlots()
                        updateTimeButtons()
                        updateAvailabilityHints()
                        updateContinueButton()
                        btnContinue.text = "Confirm Booking"
                        Toast.makeText(this, "Khung giờ này vừa được đặt. Vui lòng chọn giờ khác.", Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }

                    createBookingOrder(
                        userId = userId,
                        selectedCleanerName = selectedCleanerName,
                        dateStr = dateStr,
                        selectedSlot = selectedSlot,
                        total = total,
                        address = address
                    )
                }
            },
            onFailure = { err ->
                runOnUiThread {
                    btnContinue.isEnabled = true
                    btnContinue.text = "Confirm Booking"
                    Toast.makeText(this, "Không thể kiểm tra lịch đặt: $err", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun createBookingOrder(
        userId: String,
        selectedCleanerName: String,
        dateStr: String,
        selectedSlot: String,
        total: Double,
        address: String
    ) {
        val order = Order(
            userId      = userId,
            serviceId   = serviceId,
            cleanerId   = cleanerId,
            cleanerName = selectedCleanerName,
            date        = dateStr,
            time        = selectedSlot,
            status      = "Upcoming",
            totalPrice  = total,
            address     = address
        )

        FirestoreRepository.createOrder(
            order = order,
            onSuccess = { orderId ->
                // Chuyển sang màn Booking Success
                val intent = Intent(this, BookingSuccessActivity::class.java)
                intent.putExtra("ORDER_ID", orderId)
                startActivity(intent)
                finish()
            },
            onFailure = { err ->
                btnContinue.isEnabled = true
                btnContinue.text = "Confirm Booking"
                Toast.makeText(this, "Không thể đặt dịch vụ: $err", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun populateDates() {
        dateContainer.removeAllViews()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        var firstAvailableDate: Date? = null
        
        for (i in 0 until 7) {
            val date = calendar.time
            if (firstAvailableDate == null && isDateAvailable(date)) {
                firstAvailableDate = date
            }
            val dateButton = createDateButton(date, dateFormat.format(date), calendar.get(Calendar.DAY_OF_MONTH))
            dateContainer.addView(dateButton)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (selectedDate == null && firstAvailableDate != null) {
            selectedDate = firstAvailableDate
        }
        updateDateButtons()
    }

    private fun createDateButton(date: Date, dayName: String, dayNumber: Int): MaterialCardView {
        val isAvailable = isDateAvailable(date)
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
            isClickable = isAvailable
            isFocusable = isAvailable
            alpha = if (isAvailable) 1f else 0.45f
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

        if (isAvailable) {
            card.setOnClickListener {
                selectedDate = date
                selectedTime = null
                reservedTimeSlots = emptySet()
                updateDateButtons()
                loadReservedTimeSlotsForSelectedDate()
            }
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
        
        for (i in 0 until 7) {
            if (calendar.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH) &&
                calendar.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)) {
                return i
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return -1
    }

    private fun populateTimeSlots() {
        timeContainer.removeAllViews()
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
        val isAvailable = time in currentAvailableTimeSlots() && time !in reservedTimeSlots
        val card = MaterialCardView(this).apply {
            radius = resources.getDimension(R.dimen.card_corner_radius)
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeWidth = 2
            strokeColor = ContextCompat.getColor(context, R.color.slate_200)
            isClickable = isAvailable
            isFocusable = isAvailable
            alpha = if (isAvailable) 1f else 0.45f
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

        if (isAvailable) {
            card.setOnClickListener {
                selectedTime = time
                updateTimeButtons()
                updateContinueButton()
            }
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

    private fun refreshAvailabilityUi() {
        if (selectedDate != null && !isDateAvailable(selectedDate!!)) {
            selectedDate = null
            selectedTime = null
        }
        if (selectedTime != null && !isTimeSlotSelectable(selectedTime!!)) {
            selectedTime = null
        }

        populateDates()
        populateTimeSlots()
        updateAvailabilityHints()
        updateContinueButton()
    }

    private fun updateAvailabilityHints() {
        val daySummary = availabilityByDate
            .filterValues { it.isNotEmpty() }
            .keys
            .map(CleanerAvailability::storageKeyToDisplay)
            .toList()
        tvDateAvailabilityHint.text = if (daySummary.isEmpty()) {
            "This cleaner has not opened any dates yet."
        } else {
            "Open dates: ${daySummary.joinToString(", ")}"
        }

        val availableTimeSlots = currentAvailableTimeSlots()
        val openSlots = availableTimeSlots.filterNot { it in reservedTimeSlots }
        tvTimeAvailabilityHint.text = if (availableTimeSlots.isEmpty()) {
            "No time slots available right now."
        } else if (openSlots.isEmpty()) {
            "All slots for this date are already booked."
        } else {
            val bookedSummary = if (reservedTimeSlots.isEmpty()) {
                ""
            } else {
                " • Booked: ${reservedTimeSlots.joinToString(", ")}"
            }
            "Open slots: ${openSlots.joinToString(", ")}$bookedSummary"
        }
    }

    private fun loadReservedTimeSlotsForSelectedDate() {
        val selected = selectedDate ?: run {
            reservedTimeSlots = emptySet()
            populateTimeSlots()
            updateTimeButtons()
            updateAvailabilityHints()
            updateContinueButton()
            return
        }

        val formattedDate = formatBookingDate(selected)
        FirestoreRepository.getCleanerBookedOrders(
            cleanerId = cleanerId,
            date = formattedDate,
            onResult = { orders ->
                runOnUiThread {
                    reservedTimeSlots = orders.map { it.time }.filter { it.isNotBlank() }.toSet()
                    if (selectedTime != null && !isTimeSlotSelectable(selectedTime!!)) {
                        selectedTime = null
                    }
                    populateTimeSlots()
                    updateTimeButtons()
                    updateAvailabilityHints()
                    updateContinueButton()
                }
            },
            onFailure = {
                runOnUiThread {
                    reservedTimeSlots = emptySet()
                    populateTimeSlots()
                    updateTimeButtons()
                    updateAvailabilityHints()
                    updateContinueButton()
                }
            }
        )
    }

    private fun isTimeSlotSelectable(time: String): Boolean {
        return time in currentAvailableTimeSlots() && time !in reservedTimeSlots
    }

    private fun formatBookingDate(date: Date): String {
        return CleanerAvailability.dateToStorageKey(date)
    }

    private fun isDateAvailable(date: Date): Boolean {
        return availabilityByDate[formatBookingDate(date)].orEmpty().isNotEmpty()
    }

    private fun currentAvailableTimeSlots(): Set<String> {
        val selected = selectedDate ?: return emptySet()
        return availabilityByDate[formatBookingDate(selected)].orEmpty().toSet()
    }

    private fun updateContinueButton() {
        btnContinue.isEnabled = when (currentStep) {
            1 -> selectedDate != null && selectedTime != null
            2 -> true
            3 -> rgPaymentMethod.checkedRadioButtonId != -1
            else -> false
        }
    }

    private fun updateStepUI() {
        // Update title
        tvStepTitle.text = when (currentStep) {
            1 -> "Select Time"
            2 -> "Details"
            3 -> "Payment"
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
        
        val params3 = step3Indicator.layoutParams
        params3.width = if (currentStep == 3) dpToPx(32) else dpToPx(8)
        step3Indicator.layoutParams = params3

        // Update content visibility
        step1Content.visibility = if (currentStep == 1) android.view.View.VISIBLE else android.view.View.GONE
        step2Content.visibility = if (currentStep == 2) android.view.View.VISIBLE else android.view.View.GONE
        step3Content.visibility = if (currentStep == 3) android.view.View.VISIBLE else android.view.View.GONE

        // Update button text
        btnContinue.text = when (currentStep) {
            1 -> "Continue"
            2 -> "Continue to Payment"
            3 -> "Confirm Booking"
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
        
        val hours = 3
        val subtotal = servicePrice * hours
        val tax = 5.0
        val total = subtotal + tax
        
        tvOrderServicePrice.text = "$${subtotal}.00"
        tvOrderTax.text = "$${tax}0"
        tvOrderTotal.text = "$${total}0"
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}

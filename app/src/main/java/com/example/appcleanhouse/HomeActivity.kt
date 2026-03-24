package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.api.ApiRepository
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.models.Cleaner
import com.example.appcleanhouse.models.Service
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView

class HomeActivity : AppCompatActivity() {
    private lateinit var tvHomeUserName: TextView
    private lateinit var tvHomeAddress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tvHomeUserName = findViewById(R.id.tvHomeUserName)
        tvHomeAddress = findViewById(R.id.tvHomeAddress)
        findViewById<TextView>(R.id.btnSeeAllCleaners).setOnClickListener {
            startActivity(Intent(this, TopCleanersActivity::class.java))
        }

        setupBottomNavigation()
        loadUserHeader()
        populateServices()
        populateCleaners()
        loadWeather()           // ← Gọi Weather API
    }

    override fun onResume() {
        super.onResume()
        loadUserHeader()
    }

    private fun loadUserHeader() {
        val userId = FirebaseAuthRepository.currentUserId
        if (userId.isEmpty()) return

        FirestoreRepository.getUserProfile(userId) { user ->
            runOnUiThread {
                if (user != null) {
                    tvHomeUserName.text = user.fullName.ifEmpty { "User" }
                    tvHomeAddress.text = user.address.ifEmpty { "Chưa cập nhật địa chỉ" }
                }
            }
        }
    }

    /**
     * Gọi Open-Meteo Weather API và cập nhật UI
     * Tìm TextView có id tvWeather trong layout để hiển thị
     */
    private fun loadWeather() {
        // Tìm TextView thời tiết trong layout (nếu có)
        val tvWeather = findViewById<TextView?>(R.id.tvWeather) ?: return

        tvWeather.text = "🌤️ Đang tải thời tiết..."

        ApiRepository.getWeatherHCMC(
            onSuccess = { weather ->
                // Phải cập nhật UI trên Main Thread
                runOnUiThread {
                    tvWeather.text = "${weather.icon} ${weather.temperature}°C – ${weather.description}\n${weather.bookingAdvice}"
                }
            },
            onFailure = { err ->
                runOnUiThread {
                    tvWeather.text = "🌤️ TP. Hồ Chí Minh"
                }
            }
        )
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_bookings -> {
                    startActivity(Intent(this, BookingHistoryActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_chat -> {
                    startActivity(Intent(this, ChatActivity::class.java))
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
    }

    private fun populateServices() {
        val servicesGrid = findViewById<GridLayout>(R.id.servicesGrid)

        MockData.MOCK_SERVICES.forEachIndexed { index, service ->
            val serviceCard = createServiceCard(service)

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2)

                // Add margins
                val margin = resources.getDimensionPixelSize(R.dimen.service_card_margin)
                setMargins(
                    if (index % 2 == 0) 0 else margin,
                    0,
                    if (index % 2 == 0) margin else 0,
                    margin * 2
                )
            }

            serviceCard.layoutParams = params
            servicesGrid.addView(serviceCard)
        }
    }

    // --- SERVICE CARD matching React Home.tsx: white card + centered icon circle + name ---
    private fun createServiceCard(service: Service): CardView {
        val card = CardView(this).apply {
            radius = 28.dp.toFloat()   // rounded-3xl ≈ 28dp
            cardElevation = 2.dp.toFloat()
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            isClickable = true
            isFocusable = true

            // Ripple effect
            val outValue = android.util.TypedValue()
            if (theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true) && outValue.resourceId != 0) {
                try { foreground = ContextCompat.getDrawable(context, outValue.resourceId) } catch (e: Exception) {}
            }

            setOnClickListener {
                val intent = Intent(this@HomeActivity, ServiceDetailActivity::class.java)
                intent.putExtra("SERVICE_ID", service.id)
                intent.putExtra("SERVICE_NAME", service.name)
                startActivity(intent)
            }
        }

        // Vertical container – centered everything
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val pad = 20.dp
            setPadding(pad, pad, pad, pad)
        }

        // Colored oval circle (56dp) – matches React w-14 h-14 rounded-full
        val circle = android.widget.FrameLayout(this).apply {
            val sz = 56.dp
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                bottomMargin = 12.dp
            }
            setBackgroundResource(getCircleBackground(service.id))
        }

        val icon = ImageView(this).apply {
            setImageResource(service.iconResId)
            val iconSz = 26.dp
            layoutParams = android.widget.FrameLayout.LayoutParams(iconSz, iconSz).apply {
                gravity = android.view.Gravity.CENTER
            }
            setColorFilter(getIconTintColor(service.id))
        }
        circle.addView(icon)

        // Service name centered
        val nameText = TextView(this).apply {
            text = service.name
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.slate_700))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        container.addView(circle)
        container.addView(nameText)
        card.addView(container)
        return card
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()


    /** Returns the oval circle drawable resource for this service's icon background */
    private fun getCircleBackground(serviceId: String): Int = when (serviceId) {
        "s1" -> R.drawable.bg_circle_blue
        "s2" -> R.drawable.bg_circle_teal
        "s3" -> R.drawable.bg_circle_indigo
        "s4" -> R.drawable.bg_circle_orange
        else -> R.drawable.bg_circle_blue
    }

    /** Returns the icon tint color matching the React color classes */
    private fun getIconTintColor(serviceId: String): Int {
        val colorRes = when (serviceId) {
            "s1" -> R.color.blue_700   // text-blue-700
            "s2" -> R.color.teal_500   // text-teal-700
            "s3" -> R.color.indigo_700 // text-indigo-700
            "s4" -> R.color.orange_700  // text-orange-700
            else -> R.color.blue_700
        }
        return ContextCompat.getColor(this, colorRes)
    }

    private fun populateCleaners() {
        val cleanersList = findViewById<LinearLayout>(R.id.cleanersList)

        MockData.MOCK_CLEANERS.forEach { cleaner ->
            val cleanerCard = createCleanerCard(cleaner)
            cleanersList.addView(cleanerCard)
        }
    }

    private fun createCleanerCard(cleaner: Cleaner): CardView {
        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12.dp
            }
            radius = 24.dp.toFloat()  // rounded-3xl
            cardElevation = 2.dp.toFloat()
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            isClickable = true
            isFocusable = true
        }

        card.setOnClickListener {
            startActivity(Intent(this, CleanerDetailActivity::class.java).apply {
                putExtra("CLEANER_ID", cleaner.id)
            })
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.card_padding)
            setPadding(padding, padding, padding, padding)
        }

        // Avatar – rounded-2xl (12dp radius)
        val avatar = ShapeableImageView(this).apply {
            val sz = 64.dp
            layoutParams = LinearLayout.LayoutParams(sz, sz)
            setImageResource(cleaner.avatarResId)
            scaleType = ImageView.ScaleType.CENTER_CROP
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(12.dp.toFloat())  // rounded-2xl
                .build()
        }

        container.addView(avatar)

        // Info container
        val infoContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.card_padding)
            }
        }

        // Name
        val nameText = TextView(this).apply {
            text = cleaner.name
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.slate_900)) // Sửa lỗi getColor
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        // Specialty
        val specialtyText = TextView(this).apply {
            text = cleaner.specialty
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.slate_500)) // Sửa lỗi getColor
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.text_margin_small)
            }
        }

        // Rating container
        val ratingContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.text_margin_small)
            }
        }

        val starIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_star)
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.icon_size_small),
                resources.getDimensionPixelSize(R.dimen.icon_size_small)
            )
            setColorFilter(ContextCompat.getColor(context, R.color.yellow_400)) // Sửa lỗi getColor
        }

        val ratingText = TextView(this).apply {
            text = "${cleaner.rating}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.slate_700)) // Sửa lỗi getColor
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.text_margin_small)
            }
        }

        val jobCountText = TextView(this).apply {
            text = "(${cleaner.jobCount} jobs)"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.slate_500)) // Sửa lỗi getColor
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.text_margin_small)
            }
        }

        ratingContainer.addView(starIcon)
        ratingContainer.addView(ratingText)
        ratingContainer.addView(jobCountText)

        infoContainer.addView(nameText)
        infoContainer.addView(specialtyText)
        infoContainer.addView(ratingContainer)

        container.addView(infoContainer)

        // View button – matches React `bg-slate-50 rounded-full text-blue-600`
        val viewButton = TextView(this).apply {
            text = "View"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.blue_600))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            val padH = 16.dp
            val padV = 8.dp
            setPadding(padH, padV, padH, padV)
            setBackgroundResource(R.drawable.bg_circle_blue)  // soft blue circle pill
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(this@HomeActivity, CleanerDetailActivity::class.java).apply {
                    putExtra("CLEANER_ID", cleaner.id)
                })
            }
        }
        container.addView(viewButton)
        card.addView(container)
        return card
    }
}
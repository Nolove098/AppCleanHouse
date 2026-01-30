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
import androidx.core.content.ContextCompat // Đã thêm thư viện này để sửa lỗi màu sắc
import com.example.appcleanhouse.data.MockData
import com.example.appcleanhouse.models.Cleaner
import com.example.appcleanhouse.models.Service
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupBottomNavigation()
        populateServices()
        populateCleaners()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home
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

    // --- HÀM ĐÃ ĐƯỢC SỬA LỖI HOÀN CHỈNH ---
    private fun createServiceCard(service: Service): CardView {
        val card = CardView(this).apply {
            radius = resources.getDimension(R.dimen.card_corner_radius)
            cardElevation = resources.getDimension(R.dimen.card_elevation_small)

            // SỬA LỖI getColor: Dùng ContextCompat để an toàn hơn
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))

            isClickable = true
            isFocusable = true

            // SỬA LỖI CRASH (Resource ID): Kiểm tra kỹ trước khi lấy hiệu ứng click
            val outValue = android.util.TypedValue()
            val resolved = theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)

            if (resolved && outValue.resourceId != 0) {
                try {
                    foreground = ContextCompat.getDrawable(context, outValue.resourceId)
                } catch (e: Exception) {
                    // Bỏ qua nếu lỗi, tránh crash app
                }
            }

            setOnClickListener {
                val intent = Intent(this@HomeActivity, ServiceDetailActivity::class.java)
                intent.putExtra("SERVICE_ID", service.id)
                intent.putExtra("SERVICE_NAME", service.name)
                startActivity(intent)
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padding = resources.getDimensionPixelSize(R.dimen.card_padding)
            setPadding(padding, padding, padding, padding)
        }

        // Icon with colored background
        val iconContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.service_icon_size),
                resources.getDimensionPixelSize(R.dimen.service_icon_size)
            )
            setBackgroundResource(service.colorResId)
        }

        val icon = ImageView(this).apply {
            setImageResource(service.iconResId)
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.icon_size),
                resources.getDimensionPixelSize(R.dimen.icon_size)
            )
            setColorFilter(getIconColor(service.id))
        }

        iconContainer.addView(icon)
        container.addView(iconContainer)

        // Service name
        val nameText = TextView(this).apply {
            text = service.name
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.slate_700)) // Sửa lỗi getColor
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.text_margin)
            }
        }

        container.addView(nameText)
        card.addView(container)

        return card
    }

    private fun getIconColor(serviceId: String): Int {
        // Sửa lỗi getColor bằng ContextCompat
        val colorRes = when (serviceId) {
            "s1" -> R.color.blue_700
            "s2" -> R.color.teal_700
            "s3" -> R.color.indigo_700
            "s4" -> R.color.orange_700
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
                bottomMargin = resources.getDimensionPixelSize(R.dimen.card_margin)
            }
            radius = resources.getDimension(R.dimen.card_corner_radius_large)
            cardElevation = resources.getDimension(R.dimen.card_elevation_small)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white)) // Sửa lỗi getColor
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.card_padding)
            setPadding(padding, padding, padding, padding)
        }

        // Avatar
        val avatar = ShapeableImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.avatar_size),
                resources.getDimensionPixelSize(R.dimen.avatar_size)
            )
            setImageResource(cleaner.avatarResId)
            scaleType = ImageView.ScaleType.CENTER_CROP
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(resources.getDimension(R.dimen.avatar_corner_radius))
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

        // View button
        val viewButton = TextView(this).apply {
            text = "View"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.blue_600)) // Sửa lỗi getColor
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            val padding = resources.getDimensionPixelSize(R.dimen.button_padding_small)
            setPadding(padding, padding, padding, padding)
            setBackgroundResource(R.drawable.bg_tab_inactive)
        }
        container.addView(viewButton)
        card.addView(container)
        return card
    }
}
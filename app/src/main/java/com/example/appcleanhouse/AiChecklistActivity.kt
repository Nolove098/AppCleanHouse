package com.example.appcleanhouse

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.appcleanhouse.viewmodel.AiChecklistViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class AiChecklistActivity : AppCompatActivity() {

    private val viewModel: AiChecklistViewModel by viewModels()

    private lateinit var tvChecklistService: TextView
    private lateinit var tvChecklistDateTime: TextView
    private lateinit var tvChecklistAddress: TextView
    private lateinit var loadingContainer: LinearLayout
    private lateinit var errorContainer: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var btnRetry: MaterialButton
    private lateinit var checklistContainer: LinearLayout
    private lateinit var checklistScrollView: View
    private lateinit var progressSection: LinearLayout
    private lateinit var tvProgress: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var progressBar: ProgressBar

    // Booking info từ Intent
    private var serviceName = ""
    private var address = ""
    private var date = ""
    private var time = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_checklist)

        // Nhận data từ BookingSuccessActivity
        serviceName = intent.getStringExtra("SERVICE_NAME") ?: "Deep Clean"
        address = intent.getStringExtra("ADDRESS") ?: ""
        date = intent.getStringExtra("DATE") ?: ""
        time = intent.getStringExtra("TIME") ?: ""

        initViews()
        setupListeners()
        observeViewModel()

        // Gọi AI tạo checklist
        viewModel.generateChecklist(serviceName, address, date, time)
    }

    private fun initViews() {
        tvChecklistService = findViewById(R.id.tvChecklistService)
        tvChecklistDateTime = findViewById(R.id.tvChecklistDateTime)
        tvChecklistAddress = findViewById(R.id.tvChecklistAddress)
        loadingContainer = findViewById(R.id.loadingContainer)
        errorContainer = findViewById(R.id.errorContainer)
        tvError = findViewById(R.id.tvError)
        btnRetry = findViewById(R.id.btnRetry)
        checklistContainer = findViewById(R.id.checklistContainer)
        checklistScrollView = findViewById(R.id.checklistScrollView)
        progressSection = findViewById(R.id.progressSection)
        tvProgress = findViewById(R.id.tvProgress)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        progressBar = findViewById(R.id.progressBar)

        // Cập nhật thông tin booking
        tvChecklistService.text = "🏠 Dịch vụ: $serviceName"
        tvChecklistDateTime.text = "📅 $date – $time"
        tvChecklistAddress.text = "📍 ${address.ifEmpty { "Chưa có địa chỉ" }}"

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Back to Home
        findViewById<MaterialButton>(R.id.btnBackToHome).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupListeners() {
        btnRetry.setOnClickListener {
            viewModel.generateChecklist(serviceName, address, date, time)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                loadingContainer.visibility = if (loading) View.VISIBLE else View.GONE
                if (loading) {
                    errorContainer.visibility = View.GONE
                    checklistScrollView.visibility = View.GONE
                    progressSection.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { err ->
                if (err != null) {
                    errorContainer.visibility = View.VISIBLE
                    tvError.text = "❌ $err"
                    checklistScrollView.visibility = View.GONE
                    progressSection.visibility = View.GONE
                } else {
                    errorContainer.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.items.collect { items ->
                if (items.isNotEmpty()) {
                    checklistScrollView.visibility = View.VISIBLE
                    progressSection.visibility = View.VISIBLE
                    renderChecklist(items)
                    updateProgress(items)
                }
            }
        }
    }

    private fun renderChecklist(items: List<AiChecklistViewModel.ChecklistItem>) {
        checklistContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val card = createChecklistItemCard(index, item)
            checklistContainer.addView(card)
        }
    }

    private fun createChecklistItemCard(
        index: Int,
        item: AiChecklistViewModel.ChecklistItem
    ): MaterialCardView {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dp
            }
            radius = 16.dp.toFloat()
            cardElevation = 1.dp.toFloat()
            setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (item.checked) R.color.blue_50 else R.color.white
                )
            )
            strokeWidth = if (item.checked) 2 else 1
            strokeColor = ContextCompat.getColor(
                context,
                if (item.checked) R.color.blue_600 else R.color.slate_200
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val pad = 14.dp
            setPadding(pad, pad, pad, pad)
        }

        val checkBox = CheckBox(this).apply {
            isChecked = item.checked
            buttonTintList = ContextCompat.getColorStateList(context, R.color.blue_600)
            setOnCheckedChangeListener { _, _ ->
                viewModel.toggleItem(index)
            }
        }

        val textView = TextView(this).apply {
            text = item.text
            textSize = 15f
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (item.checked) R.color.slate_500 else R.color.slate_800
                )
            )
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 8.dp
            }
            if (item.checked) {
                paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            }
        }

        container.addView(checkBox)
        container.addView(textView)
        card.addView(container)

        card.setOnClickListener {
            viewModel.toggleItem(index)
        }

        return card
    }

    private fun updateProgress(items: List<AiChecklistViewModel.ChecklistItem>) {
        val total = items.size
        val done = items.count { it.checked }
        val percent = if (total > 0) (done * 100 / total) else 0

        tvProgress.text = "$done/$total hoàn thành"
        tvProgressPercent.text = "$percent%"
        progressBar.max = 100
        progressBar.progress = percent
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}

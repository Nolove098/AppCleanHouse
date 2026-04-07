package com.example.appcleanhouse

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appcleanhouse.viewmodel.AiPriceExplainerViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class AiPriceExplainerActivity : AppCompatActivity() {

    private val viewModel: AiPriceExplainerViewModel by viewModels()

    private lateinit var tvPriceService: TextView
    private lateinit var tvPricePerHour: TextView
    private lateinit var tvHours: TextView
    private lateinit var tvTax: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var loadingContainer: LinearLayout
    private lateinit var errorContainer: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var btnRetry: MaterialButton
    private lateinit var explanationScrollView: View
    private lateinit var tvExplanation: TextView

    private var serviceName = ""
    private var pricePerHour = 0
    private var hours = 3
    private var tax = 5.0
    private var totalPrice = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_price_explainer)

        serviceName = intent.getStringExtra("SERVICE_NAME") ?: "Deep Clean"
        pricePerHour = intent.getIntExtra("PRICE_PER_HOUR", 45)
        hours = intent.getIntExtra("HOURS", 3)
        tax = intent.getDoubleExtra("TAX", 5.0)
        totalPrice = intent.getDoubleExtra("TOTAL_PRICE", (pricePerHour * hours + tax))

        initViews()
        observeViewModel()

        viewModel.explainPrice(serviceName, pricePerHour, hours, tax, totalPrice)
    }

    private fun initViews() {
        tvPriceService = findViewById(R.id.tvPriceService)
        tvPricePerHour = findViewById(R.id.tvPricePerHour)
        tvHours = findViewById(R.id.tvHours)
        tvTax = findViewById(R.id.tvTax)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        loadingContainer = findViewById(R.id.loadingContainer)
        errorContainer = findViewById(R.id.errorContainer)
        tvError = findViewById(R.id.tvError)
        btnRetry = findViewById(R.id.btnRetry)
        explanationScrollView = findViewById(R.id.explanationScrollView)
        tvExplanation = findViewById(R.id.tvExplanation)

        // Set price info
        tvPriceService.text = serviceName
        tvPricePerHour.text = "$$pricePerHour/giờ"
        tvHours.text = "$hours giờ"
        tvTax.text = "$${String.format("%.1f", tax)}"
        tvTotalPrice.text = "$${String.format("%.1f", totalPrice)}"

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Close button
        findViewById<MaterialButton>(R.id.btnClose).setOnClickListener { finish() }

        // Retry
        btnRetry.setOnClickListener {
            viewModel.explainPrice(serviceName, pricePerHour, hours, tax, totalPrice)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                loadingContainer.visibility = if (loading) View.VISIBLE else View.GONE
                if (loading) {
                    errorContainer.visibility = View.GONE
                    explanationScrollView.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { err ->
                if (err != null) {
                    errorContainer.visibility = View.VISIBLE
                    tvError.text = "❌ $err"
                    explanationScrollView.visibility = View.GONE
                } else {
                    errorContainer.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.explanation.collect { text ->
                if (text != null) {
                    explanationScrollView.visibility = View.VISIBLE
                    tvExplanation.text = text
                }
            }
        }
    }
}

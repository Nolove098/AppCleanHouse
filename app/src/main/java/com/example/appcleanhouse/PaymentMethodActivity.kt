package com.example.appcleanhouse

import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.card.MaterialCardView

class PaymentMethodActivity : AppCompatActivity() {
    private lateinit var rbMethodCard: RadioButton
    private lateinit var rbMethodWallet: RadioButton
    private lateinit var cardMethodCard: MaterialCardView
    private lateinit var cardMethodWallet: MaterialCardView
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_method)

        rbMethodCard = findViewById(R.id.rbMethodCard)
        rbMethodWallet = findViewById(R.id.rbMethodWallet)
        cardMethodCard = findViewById(R.id.cardMethodCard)
        cardMethodWallet = findViewById(R.id.cardMethodWallet)
        userId = FirebaseAuthRepository.currentUserId

        loadPaymentMethodFromDb()
        setupSelectionEvents()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadPaymentMethodFromDb() {
        if (userId.isEmpty()) return
        FirestoreRepository.getUserProfile(userId) { user ->
            runOnUiThread {
                val method = user?.paymentMethod ?: "card"
                applySelection(method)
            }
        }
    }

    private fun setupSelectionEvents() {
        rbMethodCard.setOnClickListener { selectAndSave("card") }
        rbMethodWallet.setOnClickListener { selectAndSave("wallet") }
        cardMethodCard.setOnClickListener { selectAndSave("card") }
        cardMethodWallet.setOnClickListener { selectAndSave("wallet") }
    }

    private fun selectAndSave(method: String) {
        applySelection(method)
        if (userId.isEmpty()) return

        FirestoreRepository.updateUserPaymentMethod(
            userId = userId,
            paymentMethod = method,
            onFailure = { err ->
                runOnUiThread {
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun applySelection(method: String) {
        val isCard = method == "card"
        rbMethodCard.isChecked = isCard
        rbMethodWallet.isChecked = !isCard
    }
}

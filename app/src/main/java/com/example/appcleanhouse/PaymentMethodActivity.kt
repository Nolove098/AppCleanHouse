package com.example.appcleanhouse

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appcleanhouse.models.PaymentCard
import com.example.appcleanhouse.repository.FirebaseAuthRepository
import com.example.appcleanhouse.repository.FirestoreRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.ListenerRegistration
import kotlin.random.Random

class PaymentMethodActivity : AppCompatActivity() {
    private lateinit var layoutCards: LinearLayout
    private lateinit var tvNoCards: TextView
    private lateinit var btnAddCard: MaterialButton
    private lateinit var cardPaypal: MaterialCardView
    private lateinit var cardApplePay: MaterialCardView
    private var userId: String = ""
    private var currentMethod: String = "card"
    private var cardsListener: ListenerRegistration? = null
    private var currentCards: List<PaymentCard> = emptyList()
    private var selectedCardId: String? = null
    private var isHealingDefaultCard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_method)

        layoutCards = findViewById(R.id.layoutCards)
        tvNoCards = findViewById(R.id.tvNoCards)
        btnAddCard = findViewById(R.id.btnAddCard)
        cardPaypal = findViewById(R.id.cardPaypal)
        cardApplePay = findViewById(R.id.cardApplePay)
        userId = FirebaseAuthRepository.currentUserId

        setupOtherMethods()
        setupAddCard()
        loadPaymentMethodFromDb()
        listenCards()
        seedDefaultCardsIfNeeded()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cardsListener?.remove()
    }

    private fun seedDefaultCardsIfNeeded() {
        if (userId.isBlank()) return
        FirestoreRepository.ensureDefaultPaymentCards(userId)
    }

    private fun loadPaymentMethodFromDb() {
        if (userId.isEmpty()) return
        FirestoreRepository.getUserProfile(userId) { user ->
            runOnUiThread {
                currentMethod = user?.paymentMethod ?: "card"
                updateOtherMethodHighlights()
            }
        }
    }

    private fun setupOtherMethods() {
        cardPaypal.setOnClickListener { selectAndSaveMethod("paypal") }
        cardApplePay.setOnClickListener { selectAndSaveMethod("applepay") }
    }

    private fun setupAddCard() {
        btnAddCard.setOnClickListener {
            showAddCardDialog()
        }
    }

    private fun showAddCardDialog() {
        if (userId.isBlank()) return

        val inputLast4 = EditText(this).apply {
            hint = "Last 4 digits"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText((1000 + Random.nextInt(9000)).toString())
        }
        val inputExpiry = EditText(this).apply {
            hint = "Expiry (MM/YY)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setText("${1 + Random.nextInt(12)}/${24 + Random.nextInt(6)}")
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(8))
            addView(inputLast4)
            addView(inputExpiry)
        }

        val brandOptions = arrayOf("Visa", "Mastercard")
        var selectedBrand = brandOptions.first()

        AlertDialog.Builder(this)
            .setTitle("Add New Card")
            .setSingleChoiceItems(brandOptions, 0) { _, which ->
                selectedBrand = brandOptions[which]
            }
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val last4 = inputLast4.text.toString().trim().takeLast(4).padStart(4, '0')
                val expiry = inputExpiry.text.toString().trim().ifBlank { "12/30" }

                val card = PaymentCard(
                    type = selectedBrand,
                    last4 = last4,
                    expiry = expiry,
                    isDefault = currentCards.isEmpty(),
                    createdAt = System.currentTimeMillis()
                )
                FirestoreRepository.addPaymentCard(
                    userId = userId,
                    card = card,
                    onFailure = { err ->
                        runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
                    }
                )
            }
            .show()
    }

    private fun listenCards() {
        if (userId.isBlank()) return
        cardsListener?.remove()
        cardsListener = FirestoreRepository.listenUserPaymentCards(
            userId = userId,
            onResult = { cards ->
                runOnUiThread {
                    currentCards = cards
                    val defaultCardId = cards.firstOrNull { it.isDefault }?.id
                    selectedCardId = when {
                        !defaultCardId.isNullOrBlank() -> defaultCardId
                        !selectedCardId.isNullOrBlank() && cards.any { it.id == selectedCardId } -> selectedCardId
                        cards.isNotEmpty() -> cards.first().id
                        else -> null
                    }

                    if (!isHealingDefaultCard && cards.isNotEmpty() && defaultCardId.isNullOrBlank()) {
                        isHealingDefaultCard = true
                        val fallbackId = cards.first().id
                        FirestoreRepository.setDefaultPaymentCard(
                            userId = userId,
                            cardId = fallbackId,
                            onSuccess = {
                                isHealingDefaultCard = false
                                runOnUiThread { selectAndSaveMethod("card", silent = true) }
                            },
                            onFailure = {
                                isHealingDefaultCard = false
                            }
                        )
                    }
                    renderCards(cards)
                }
            },
            onFailure = { err ->
                runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
            }
        )
    }

    private fun renderCards(cards: List<PaymentCard>) {
        layoutCards.removeAllViews()
        tvNoCards.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE

        cards.forEach { card ->
            val item = LayoutInflater.from(this).inflate(R.layout.item_payment_card, layoutCards, false)
            val cardContainer = item.findViewById<MaterialCardView>(R.id.cardContainer)
            val tvDefaultBadge = item.findViewById<TextView>(R.id.tvDefaultBadge)
            val tvCardTitle = item.findViewById<TextView>(R.id.tvCardTitle)
            val tvCardSubtitle = item.findViewById<TextView>(R.id.tvCardSubtitle)
            val btnSetDefault = item.findViewById<ImageView>(R.id.btnSetDefault)
            val btnDelete = item.findViewById<ImageView>(R.id.btnDeleteCard)
            val iconWrap = item.findViewById<View>(R.id.iconWrap)
            val isSelected = card.id == selectedCardId

            tvCardTitle.text = "${card.type} •••• ${card.last4}"
            tvCardSubtitle.text = "Expires ${card.expiry}"
            tvDefaultBadge.visibility = if (isSelected) View.VISIBLE else View.GONE
            cardContainer.strokeWidth = if (isSelected) dp(2) else dp(1)
            cardContainer.strokeColor = ContextCompat.getColor(this, if (isSelected) R.color.blue_600 else R.color.slate_100)
            cardContainer.setCardBackgroundColor(ContextCompat.getColor(this, if (isSelected) R.color.blue_50 else R.color.white))
            btnSetDefault.setColorFilter(
                ContextCompat.getColor(this, if (isSelected) R.color.blue_700 else R.color.slate_300)
            )

            if (card.type.equals("Mastercard", ignoreCase = true)) {
                iconWrap.setBackgroundResource(R.drawable.bg_circle_orange)
            } else {
                iconWrap.setBackgroundResource(R.drawable.bg_circle_blue)
            }

            cardContainer.setOnClickListener {
                selectDefaultCard(card)
            }

            btnSetDefault.setOnClickListener {
                selectDefaultCard(card)
            }

            btnDelete.setOnClickListener {
                FirestoreRepository.deletePaymentCard(
                    userId = userId,
                    cardId = card.id,
                    onSuccess = {
                        val remaining = currentCards.filter { it.id != card.id }
                        if (card.isDefault && remaining.isNotEmpty()) {
                            FirestoreRepository.setDefaultPaymentCard(userId, remaining.first().id)
                        }
                    },
                    onFailure = { err -> runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() } }
                )
            }

            layoutCards.addView(item)
        }
    }

    private fun selectDefaultCard(card: PaymentCard) {
        val wasSelected = card.id == selectedCardId
        if (!wasSelected) {
            selectedCardId = card.id
            renderCards(currentCards)
        }

        if (card.isDefault || wasSelected) {
            selectAndSaveMethod("card", silent = true)
            return
        }

        FirestoreRepository.setDefaultPaymentCard(
            userId = userId,
            cardId = card.id,
            onSuccess = { runOnUiThread { selectAndSaveMethod("card", silent = true) } },
            onFailure = { err -> runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() } }
        )
    }

    private fun selectAndSaveMethod(method: String, silent: Boolean = false) {
        currentMethod = method
        updateOtherMethodHighlights()
        if (userId.isEmpty()) return

        FirestoreRepository.updateUserPaymentMethod(
            userId = userId,
            paymentMethod = method,
            onSuccess = {
                if (!silent) {
                    runOnUiThread {
                        Toast.makeText(this, "Default method updated", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onFailure = { err ->
                runOnUiThread {
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun updateOtherMethodHighlights() {
        val paypalSelected = currentMethod == "paypal"
        val appleSelected = currentMethod == "applepay"

        cardPaypal.strokeWidth = if (paypalSelected) dp(2) else dp(1)
        cardPaypal.strokeColor = ContextCompat.getColor(this, if (paypalSelected) R.color.blue_500 else R.color.slate_100)

        cardApplePay.strokeWidth = if (appleSelected) dp(2) else dp(1)
        cardApplePay.strokeColor = ContextCompat.getColor(this, if (appleSelected) R.color.blue_500 else R.color.slate_100)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

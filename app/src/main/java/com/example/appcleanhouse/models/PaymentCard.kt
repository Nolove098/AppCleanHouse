package com.example.appcleanhouse.models

data class PaymentCard(
    val id: String = "",
    val type: String = "Visa",
    val last4: String = "0000",
    val expiry: String = "12/30",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

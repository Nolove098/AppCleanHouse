package com.example.appcleanhouse.models

data class Review(
    val id: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val cleanerId: String = "",
    val cleanerName: String = "",
    val serviceId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
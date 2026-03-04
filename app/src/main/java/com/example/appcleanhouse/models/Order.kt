package com.example.appcleanhouse.models

data class Order(
    val id: String = "",
    val userId: String = "",
    val serviceId: String = "",
    val cleanerId: String = "",
    val date: String = "",
    val time: String = "",
    val status: String = "Upcoming", // "Upcoming", "Completed", "Cancelled"
    val totalPrice: Double = 0.0,
    val address: String = "",
    val rating: Int? = null
)

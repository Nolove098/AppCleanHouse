package com.example.appcleanhouse.models

data class Service(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val pricePerHour: Int = 0,
    val rating: Double = 0.0,
    // colorResId & iconResId được map lại ở UI dựa theo id
    val colorResId: Int = 0,
    val iconResId: Int = 0
)

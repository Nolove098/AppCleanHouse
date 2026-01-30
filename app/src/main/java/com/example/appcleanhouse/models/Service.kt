package com.example.appcleanhouse.models

data class Service(
    val id: String,
    val name: String,
    val description: String,
    val pricePerHour: Int,
    val rating: Double,
    val colorResId: Int,
    val iconResId: Int
)

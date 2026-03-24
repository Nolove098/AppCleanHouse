package com.example.appcleanhouse.models

data class Cleaner(
    val id: String = "",
    val name: String = "",
    // avatarResId được map lại ở UI; không lưu Firestore
    val avatarResId: Int = 0,
    val rating: Double = 0.0,
    val jobCount: Int = 0,
    val specialty: String = "",
    val experience: String = "2 yrs",
    val about: String = "Professional cleaner with attention to detail.",
    val pricePerHour: Int = 45,
    val tags: List<String> = emptyList(),
    val distanceKm: Double = 0.0
)

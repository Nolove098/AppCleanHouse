package com.example.appcleanhouse.models

data class Cleaner(
    val id: String = "",
    val name: String = "",
    // avatarResId được map lại ở UI; không lưu Firestore
    val avatarResId: Int = 0,
    val rating: Double = 0.0,
    val jobCount: Int = 0,
    val specialty: String = ""
)

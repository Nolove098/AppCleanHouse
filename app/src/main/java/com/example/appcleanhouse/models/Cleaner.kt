package com.example.appcleanhouse.models

data class Cleaner(
    val id: String,
    val name: String,
    val avatarResId: Int,
    val rating: Double,
    val jobCount: Int,
    val specialty: String
)

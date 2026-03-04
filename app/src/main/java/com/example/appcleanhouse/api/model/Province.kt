package com.example.appcleanhouse.api.model

import com.google.gson.annotations.SerializedName

/**
 * Response từ Vietnam Provinces API
 * Endpoint: https://provinces.open-api.vn/api/
 */
data class Province(
    val code: Int,
    val name: String,
    @SerializedName("name_en")
    val nameEn: String,
    val codename: String,
    @SerializedName("division_type")
    val divisionType: String
)

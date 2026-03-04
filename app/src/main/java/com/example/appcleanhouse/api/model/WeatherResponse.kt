package com.example.appcleanhouse.api.model

import com.google.gson.annotations.SerializedName

/**
 * Response từ Open-Meteo Weather API
 * Endpoint: https://api.open-meteo.com/v1/forecast?latitude=...&longitude=...&current_weather=true
 */
data class WeatherResponse(
    @SerializedName("current_weather")
    val currentWeather: CurrentWeather,
    val latitude: Double,
    val longitude: Double
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    @SerializedName("weathercode")
    val weatherCode: Int,    // Mã thời tiết WMO
    val time: String
) {
    /** Chuyển weathercode thành mô tả tiếng Việt */
    val description: String get() = when (weatherCode) {
        0            -> "Trời quang ☀️"
        in 1..3      -> "Có mây ⛅"
        in 45..48    -> "Sương mù 🌫️"
        in 51..67    -> "Mưa phùn 🌧️"
        in 71..77    -> "Tuyết ❄️"
        in 80..82    -> "Mưa rào 🌦️"
        in 85..86    -> "Bão tuyết 🌨️"
        in 95..99    -> "Dông bão ⛈️"
        else         -> "Không xác định"
    }

    /** Icon emoji theo thời tiết */
    val icon: String get() = when (weatherCode) {
        0            -> "☀️"
        in 1..3      -> "⛅"
        in 45..48    -> "🌫️"
        in 51..67    -> "🌧️"
        in 80..82    -> "🌦️"
        in 95..99    -> "⛈️"
        else         -> "🌤️"
    }

    /** Gợi ý đặt dịch vụ dọn nhà dựa theo thời tiết */
    val bookingAdvice: String get() = when (weatherCode) {
        0           -> "Thời tiết đẹp! Lý tưởng để đặt vệ sinh nhà cửa 🏠"
        in 1..3     -> "Thời tiết ổn, phù hợp đặt dịch vụ dọn nhà"
        in 80..99   -> "Trời mưa - Đặt dịch vụ trong nhà ngay hôm nay!"
        else        -> "Hãy đặt dịch vụ để căn nhà luôn sạch sẽ"
    }
}

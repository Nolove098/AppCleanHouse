package com.example.appcleanhouse.api.service

import com.example.appcleanhouse.api.model.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface cho Open-Meteo Weather API
 * Base URL: https://api.open-meteo.com/
 * Docs: https://open-meteo.com/en/docs
 *
 * ✅ MIỄN PHÍ – KHÔNG CẦN API KEY
 */
interface WeatherApiService {

    /**
     * Lấy thời tiết hiện tại theo tọa độ GPS
     * @param latitude  Vĩ độ (TP.HCM ≈ 10.82)
     * @param longitude Kinh độ (TP.HCM ≈ 106.63)
     */
    @GET("v1/forecast")
    fun getCurrentWeather(
        @Query("latitude")        latitude: Double,
        @Query("longitude")       longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true
    ): Call<WeatherResponse>
}

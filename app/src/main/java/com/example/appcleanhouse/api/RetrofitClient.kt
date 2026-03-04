package com.example.appcleanhouse.api

import com.example.appcleanhouse.api.service.ProvinceApiService
import com.example.appcleanhouse.api.service.WeatherApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton cung cấp các Retrofit API service instance.
 *
 * Cách dùng:
 *   RetrofitClient.weatherApi.getCurrentWeather(lat, lon).enqueue(...)
 *   RetrofitClient.provinceApi.getAllProvinces().enqueue(...)
 */
object RetrofitClient {

    // ── OkHttp Client chung (log mọi request/response khi debug) ──
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // ── API 1: Open-Meteo Weather API ─────────────────────────────
    val weatherApi: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    // ── API 2: Vietnam Provinces API ──────────────────────────────
    val provinceApi: ProvinceApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://provinces.open-api.vn/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProvinceApiService::class.java)
    }
}

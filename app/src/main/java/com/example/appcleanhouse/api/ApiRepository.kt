package com.example.appcleanhouse.api

import android.util.Log
import com.example.appcleanhouse.api.model.CurrentWeather
import com.example.appcleanhouse.api.model.Province
import com.example.appcleanhouse.api.model.WeatherResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Repository tập trung xử lý tất cả REST API calls.
 * Wrap Retrofit callbacks thành lambda đơn giản hơn để dùng trong Activity.
 */
object ApiRepository {

    private const val TAG = "ApiRepository"

    // ─── WEATHER API ───────────────────────────────────────────────

    /**
     * Lấy thời tiết hiện tại tại một địa điểm.
     *
     * Ví dụ toạ độ Việt Nam:
     *  - TP.HCM: lat=10.8231, lon=106.6297
     *  - Hà Nội: lat=21.0285, lon=105.8542
     *  - Đà Nẵng: lat=16.0544, lon=108.2022
     *
     * @param latitude  Vĩ độ
     * @param longitude Kinh độ
     * @param onSuccess Callback khi thành công, trả về CurrentWeather
     * @param onFailure Callback khi thất bại, trả về message lỗi
     */
    fun getWeather(
        latitude: Double,
        longitude: Double,
        onSuccess: (CurrentWeather) -> Unit,
        onFailure: (String) -> Unit
    ) {
        RetrofitClient.weatherApi
            .getCurrentWeather(latitude, longitude)
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val weather = response.body()?.currentWeather
                        if (weather != null) {
                            Log.d(TAG, "Weather: ${weather.temperature}°C, code=${weather.weatherCode}")
                            onSuccess(weather)
                        } else {
                            onFailure("Không có dữ liệu thời tiết")
                        }
                    } else {
                        onFailure("Lỗi API: ${response.code()} ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e(TAG, "Weather API failed", t)
                    onFailure("Không kết nối được: ${t.message}")
                }
            })
    }

    /**
     * Lấy thời tiết tại TP.HCM (mặc định cho app)
     */
    fun getWeatherHCMC(
        onSuccess: (CurrentWeather) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        getWeather(
            latitude  = 10.8231,
            longitude = 106.6297,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    // ─── PROVINCE API ──────────────────────────────────────────────

    /**
     * Lấy danh sách 63 tỉnh/thành phố Việt Nam.
     *
     * @param onSuccess Callback khi thành công, trả về List<Province>
     * @param onFailure Callback khi thất bại
     */
    fun getAllProvinces(
        onSuccess: (List<Province>) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        RetrofitClient.provinceApi
            .getAllProvinces()
            .enqueue(object : Callback<List<Province>> {
                override fun onResponse(
                    call: Call<List<Province>>,
                    response: Response<List<Province>>
                ) {
                    if (response.isSuccessful) {
                        val provinces = response.body() ?: emptyList()
                        Log.d(TAG, "Provinces: ${provinces.size} items")
                        onSuccess(provinces)
                    } else {
                        onFailure("Lỗi API: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<Province>>, t: Throwable) {
                    Log.e(TAG, "Province API failed", t)
                    onFailure("Không kết nối được: ${t.message}")
                }
            })
    }
}

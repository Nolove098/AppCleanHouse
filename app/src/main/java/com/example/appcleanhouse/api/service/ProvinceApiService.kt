package com.example.appcleanhouse.api.service

import com.example.appcleanhouse.api.model.Province
import retrofit2.Call
import retrofit2.http.GET

/**
 * Retrofit interface cho Vietnam Provinces API
 * Base URL: https://provinces.open-api.vn/
 * Docs: https://provinces.open-api.vn/
 *
 * ✅ MIỄN PHÍ – KHÔNG CẦN API KEY
 */
interface ProvinceApiService {

    /**
     * Lấy toàn bộ danh sách tỉnh/thành phố Việt Nam (63 tỉnh)
     */
    @GET("api/")
    fun getAllProvinces(): Call<List<Province>>
}

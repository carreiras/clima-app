package com.example.climaapp.data.remote

import com.example.climaapp.data.remote.dto.GeocodingResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiService {
    @GET("v1/search")
    suspend fun searchCity(@Query("name") name: String): GeocodingResponseDto
}

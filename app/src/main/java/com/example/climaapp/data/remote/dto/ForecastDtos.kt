package com.example.climaapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ForecastResponseDto(
    val current: CurrentDto,
    val daily: DailyDto
)

data class CurrentDto(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("weather_code") val weatherCode: Int
)

data class DailyDto(
    @SerializedName("temperature_2m_max") val maxTemperatures: List<Double>,
    @SerializedName("temperature_2m_min") val minTemperatures: List<Double>
)

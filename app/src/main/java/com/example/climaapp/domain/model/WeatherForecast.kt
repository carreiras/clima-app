package com.example.climaapp.domain.model

data class WeatherForecast(
    val currentTemperature: Double,
    val weatherCode: Int,
    val dailyMaxTemperatures: List<Double>,
    val dailyMinTemperatures: List<Double>
)

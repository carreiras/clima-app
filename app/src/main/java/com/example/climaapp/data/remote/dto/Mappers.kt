package com.example.climaapp.data.remote.dto

import com.example.climaapp.domain.model.City
import com.example.climaapp.domain.model.WeatherForecast

fun GeocodingResultDto.toDomain(): City = City(
    id = id,
    name = name,
    country = country ?: "",
    latitude = latitude,
    longitude = longitude,
    region = listOfNotNull(admin3, admin2, admin1).distinct().take(2).joinToString(", ").ifBlank { null }
)

fun ForecastResponseDto.toDomain(): WeatherForecast = WeatherForecast(
    currentTemperature = current.temperature,
    weatherCode = current.weatherCode,
    dailyMaxTemperatures = daily.maxTemperatures,
    dailyMinTemperatures = daily.minTemperatures
)

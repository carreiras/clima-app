package com.example.climaapp.data.remote.dto

import com.example.climaapp.domain.model.City
import com.example.climaapp.domain.model.WeatherForecast

fun GeocodingResultDto.toDomain(): City = City(
    id = id,
    name = name,
    country = country ?: "",
    latitude = latitude,
    longitude = longitude
)

fun ForecastResponseDto.toDomain(): WeatherForecast = WeatherForecast(
    currentTemperature = current.temperature,
    weatherCode = current.weatherCode,
    dailyMaxTemperatures = daily.maxTemperatures,
    dailyMinTemperatures = daily.minTemperatures
)

package com.example.climaapp.domain.repository

import com.example.climaapp.domain.model.City
import com.example.climaapp.domain.model.WeatherForecast
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun searchCity(query: String): Flow<Result<List<City>>>
    fun getForecast(city: City): Flow<Result<WeatherForecast>>
    fun getFavorites(): Flow<List<City>>
    suspend fun toggleFavorite(city: City): Boolean
    fun isFavorite(cityId: Long): Flow<Boolean>
}

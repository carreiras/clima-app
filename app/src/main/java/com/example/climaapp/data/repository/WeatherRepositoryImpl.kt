package com.example.climaapp.data.repository

import com.example.climaapp.data.remote.ForecastApiService
import com.example.climaapp.data.remote.GeocodingApiService
import com.example.climaapp.data.remote.dto.toDomain
import com.example.climaapp.domain.model.City
import com.example.climaapp.domain.model.WeatherForecast
import com.example.climaapp.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val geocodingApi: GeocodingApiService,
    private val forecastApi: ForecastApiService
) : WeatherRepository {

    override fun searchCity(query: String): Flow<Result<List<City>>> = flow {
        try {
            val response = geocodingApi.searchCity(query)
            val cities = response.results.orEmpty().map { it.toDomain() }
            emit(Result.success(cities))
        } catch (e: IOException) {
            emit(Result.failure(e))
        } catch (e: HttpException) {
            emit(Result.failure(e))
        }
    }

    override fun getForecast(city: City): Flow<Result<WeatherForecast>> = flow {
        try {
            val response = forecastApi.getForecast(city.latitude, city.longitude)
            emit(Result.success(response.toDomain()))
        } catch (e: IOException) {
            emit(Result.failure(e))
        } catch (e: HttpException) {
            emit(Result.failure(e))
        }
    }
}

package com.example.climaapp.data.repository

import com.example.climaapp.data.local.CityDao
import com.example.climaapp.data.local.CityEntity
import com.example.climaapp.data.remote.ForecastApiService
import com.example.climaapp.data.remote.GeocodingApiService
import com.example.climaapp.data.remote.dto.toDomain
import com.example.climaapp.domain.model.City
import com.example.climaapp.domain.model.WeatherForecast
import com.example.climaapp.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val geocodingApi: GeocodingApiService,
    private val forecastApi: ForecastApiService,
    private val cityDao: CityDao
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
        cityDao.getById(city.id)?.lastTemperature?.let { cachedTemp ->
            emit(Result.success(WeatherForecast(cachedTemp, 0, emptyList(), emptyList())))
        }
        try {
            val response = forecastApi.getForecast(city.latitude, city.longitude)
            val forecast = response.toDomain()
            cityDao.getById(city.id)?.let { existing ->
                cityDao.upsert(existing.copy(lastTemperature = forecast.currentTemperature))
            }
            emit(Result.success(forecast))
        } catch (e: IOException) {
            emit(Result.failure(e))
        } catch (e: HttpException) {
            emit(Result.failure(e))
        }
    }

    override fun getFavorites(): Flow<List<City>> = cityDao.getAll().map { entities ->
        entities.map { City(it.id, it.name, it.country, it.latitude, it.longitude) }
    }

    override suspend fun toggleFavorite(city: City): Boolean {
        val existing = cityDao.getById(city.id)
        return if (existing != null) {
            cityDao.delete(city.id)
            false
        } else {
            cityDao.upsert(
                CityEntity(city.id, city.name, city.country, city.latitude, city.longitude, lastTemperature = null)
            )
            true
        }
    }

    override fun isFavorite(cityId: Long): Flow<Boolean> = cityDao.getAll().map { entities ->
        entities.any { it.id == cityId }
    }
}

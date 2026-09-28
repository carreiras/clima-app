package com.example.climaapp.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.climaapp.domain.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: WeatherRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val favorites = repository.getFavorites().first()
        favorites.forEach { city ->
            val result = repository.getForecast(city).last()
            result.getOrNull()?.let { forecast ->
                NotificationHelper.notifyIfThresholdCrossed(applicationContext, city, forecast.currentTemperature)
            }
        }
        return Result.success()
    }
}

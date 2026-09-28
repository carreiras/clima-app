package com.example.climaapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.climaapp.data.settings.SettingsRepository
import com.example.climaapp.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SyncIntervalOption(val label: String, val minutes: Long)

val SYNC_INTERVAL_OPTIONS = listOf(
    SyncIntervalOption("15 minutos", 15),
    SyncIntervalOption("1 hora", 60),
    SyncIntervalOption("6 horas", 360),
    SyncIntervalOption("12 horas", 720),
    SyncIntervalOption("24 horas", 1440)
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _syncIntervalMinutes = MutableStateFlow(settingsRepository.getSyncIntervalMinutes())
    val syncIntervalMinutes: StateFlow<Long> = _syncIntervalMinutes.asStateFlow()

    private val _temperatureThreshold = MutableStateFlow(settingsRepository.getTemperatureThreshold())
    val temperatureThreshold: StateFlow<Float> = _temperatureThreshold.asStateFlow()

    fun setSyncInterval(minutes: Long) {
        settingsRepository.setSyncIntervalMinutes(minutes)
        _syncIntervalMinutes.value = minutes
        syncScheduler.schedule(minutes, policy = ExistingPeriodicWorkPolicy.REPLACE)
    }

    fun setTemperatureThreshold(value: Float) {
        settingsRepository.setTemperatureThreshold(value)
        _temperatureThreshold.value = value
    }

    fun syncNow() {
        syncScheduler.scheduleOnce()
    }
}

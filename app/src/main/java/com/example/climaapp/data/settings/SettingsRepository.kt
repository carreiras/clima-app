package com.example.climaapp.data.settings

import android.content.SharedPreferences
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val preferences: SharedPreferences
) {
    companion object {
        private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"
        private const val KEY_TEMPERATURE_THRESHOLD = "temperature_threshold"

        const val DEFAULT_SYNC_INTERVAL_MINUTES = 360L // 6 horas
        const val DEFAULT_TEMPERATURE_THRESHOLD = 30.0f
    }

    fun getSyncIntervalMinutes(): Long =
        preferences.getLong(KEY_SYNC_INTERVAL_MINUTES, DEFAULT_SYNC_INTERVAL_MINUTES)

    fun setSyncIntervalMinutes(minutes: Long) {
        preferences.edit().putLong(KEY_SYNC_INTERVAL_MINUTES, minutes).apply()
    }

    fun getTemperatureThreshold(): Float =
        preferences.getFloat(KEY_TEMPERATURE_THRESHOLD, DEFAULT_TEMPERATURE_THRESHOLD)

    fun setTemperatureThreshold(value: Float) {
        preferences.edit().putFloat(KEY_TEMPERATURE_THRESHOLD, value).apply()
    }
}

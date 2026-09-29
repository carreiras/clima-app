package com.example.climaapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.climaapp.data.settings.SettingsRepository
import com.example.climaapp.ui.navigation.ClimaNavHost
import com.example.climaapp.ui.theme.ClimaAppTheme
import com.example.climaapp.work.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var syncScheduler: SyncScheduler

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* resultado tratado silenciosamente — notificação só some se negado */ }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        syncScheduler.schedule(settingsRepository.getSyncIntervalMinutes())

        setContent {
            ClimaAppTheme {
                ClimaNavHost()
            }
        }
    }
}

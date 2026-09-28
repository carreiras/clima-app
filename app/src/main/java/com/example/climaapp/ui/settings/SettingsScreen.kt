package com.example.climaapp.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val syncIntervalMinutes by viewModel.syncIntervalMinutes.collectAsState()
    val temperatureThreshold by viewModel.temperatureThreshold.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Intervalo de sincronização", style = MaterialTheme.typography.titleMedium)
        SYNC_INTERVAL_OPTIONS.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option.minutes == syncIntervalMinutes,
                        onClick = { viewModel.setSyncInterval(option.minutes) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = option.minutes == syncIntervalMinutes,
                    onClick = { viewModel.setSyncInterval(option.minutes) }
                )
                Text(text = option.label, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Text(
            text = "Limiar de notificação: ${temperatureThreshold.toInt()}°C",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp)
        )
        Slider(
            value = temperatureThreshold,
            onValueChange = { viewModel.setTemperatureThreshold(it) },
            valueRange = 0f..50f
        )

        Text(
            text = "O Android não permite sincronização periódica mais rápida que 15 minutos. " +
                "Pra testar agora, sem esperar:",
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )
        Button(onClick = { viewModel.syncNow() }) {
            Text("Sincronizar agora")
        }
    }
}

package com.example.climaapp.ui.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DetailsScreen(modifier: Modifier = Modifier, viewModel: DetailsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "${viewModel.city.name}, ${viewModel.city.country}")
        when (val state = uiState) {
            is DetailsUiState.Loading -> CircularProgressIndicator()
            is DetailsUiState.Success -> Text(text = "${state.forecast.currentTemperature}°C")
            is DetailsUiState.Error -> Text(text = state.message)
        }
        Button(onClick = { viewModel.toggleFavorite() }) {
            Text(text = if (isFavorite) "Remover dos favoritos" else "Favoritar")
        }
    }
}

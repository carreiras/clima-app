package com.example.climaapp.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climaapp.domain.model.City

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
    onCityClick: (City) -> Unit = {}
) {
    val favorites by viewModel.favorites.collectAsState()

    if (favorites.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Favorite uma cidade na tela de Busca para acompanhar a temperatura dela aqui.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
            items(favorites) { city ->
                ListItem(
                    headlineContent = {
                        Text(text = city.name, style = MaterialTheme.typography.titleMedium)
                    },
                    supportingContent = {
                        val subtitle = city.region?.let { "$it, ${city.country}" } ?: city.country
                        Text(text = subtitle)
                    },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    trailingContent = {
                        val temperatureText = city.lastTemperature?.let { "${it.toInt()}°" } ?: "—"
                        Text(text = temperatureText, style = MaterialTheme.typography.headlineLarge)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCityClick(city) }
                )
            }
        }
    }
}

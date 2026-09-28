package com.example.climaapp.ui.favorites

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FavoritesScreen(modifier: Modifier = Modifier, viewModel: FavoritesViewModel = hiltViewModel()) {
    val favorites by viewModel.favorites.collectAsState()

    if (favorites.isEmpty()) {
        Text(text = "Nenhuma cidade favoritada ainda", modifier = modifier.fillMaxSize().padding(16.dp))
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(favorites) { city ->
                ListItem(headlineContent = { Text(text = "${city.name}, ${city.country}") })
            }
        }
    }
}

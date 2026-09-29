package com.example.climaapp.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climaapp.domain.model.City

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    onCityClick: (City) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.search(it)
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Buscar cidade") },
            singleLine = true
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is SearchUiState.Idle -> EmptyState(
                    text = "Digite o nome de uma cidade para ver a previsão do tempo."
                )

                is SearchUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp)
                )

                is SearchUiState.Error -> EmptyState(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )

                is SearchUiState.Success -> LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(state.cities) { city ->
                        ListItem(
                            headlineContent = {
                                Text(text = city.name, style = MaterialTheme.typography.titleMedium)
                            },
                            supportingContent = {
                                val subtitle = city.region?.let { "$it, ${city.country}" } ?: city.country
                                Text(text = subtitle)
                            },
                            leadingContent = {
                                Icon(Icons.Filled.LocationOn, contentDescription = null)
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
    }
}

@Composable
private fun EmptyState(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Box(modifier = Modifier.fillMaxSize().padding(top = 48.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = color
        )
    }
}

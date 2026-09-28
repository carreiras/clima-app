package com.example.climaapp.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.search(it)
            },
            label = { Text("Buscar cidade") }
        )
        when (val state = uiState) {
            is SearchUiState.Idle -> Unit
            is SearchUiState.Loading -> CircularProgressIndicator()
            is SearchUiState.Error -> Text(text = state.message)
            is SearchUiState.Success -> LazyColumn {
                items(state.cities) { city ->
                    ListItem(
                        headlineContent = { Text(text = "${city.name}, ${city.country}") },
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable { onCityClick(city) }
                    )
                }
            }
        }
    }
}

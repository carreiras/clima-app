package com.example.climaapp.ui.search

import com.example.climaapp.domain.model.City

sealed class SearchUiState {
    data object Idle : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val cities: List<City>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

package com.example.climaapp.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climaapp.domain.model.City
import com.example.climaapp.domain.model.WeatherForecast
import com.example.climaapp.domain.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

sealed class DetailsUiState {
    data object Loading : DetailsUiState()
    data class Success(val forecast: WeatherForecast) : DetailsUiState()
    data class Error(val message: String) : DetailsUiState()
}

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WeatherRepository
) : ViewModel() {

    val city: City = City(
        id = checkNotNull<Long>(savedStateHandle["cityId"]),
        name = URLDecoder.decode(checkNotNull<String>(savedStateHandle["name"]), "UTF-8"),
        country = URLDecoder.decode(checkNotNull<String>(savedStateHandle["country"]), "UTF-8"),
        latitude = checkNotNull<Float>(savedStateHandle["lat"]).toDouble(),
        longitude = checkNotNull<Float>(savedStateHandle["lon"]).toDouble()
    )

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getForecast(city).collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { DetailsUiState.Success(it) },
                    onFailure = { DetailsUiState.Error(it.message ?: "Erro ao buscar previsão") }
                )
            }
        }
    }
}

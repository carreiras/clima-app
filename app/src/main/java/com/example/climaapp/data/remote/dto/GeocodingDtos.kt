package com.example.climaapp.data.remote.dto

data class GeocodingResponseDto(
    val results: List<GeocodingResultDto>?
)

data class GeocodingResultDto(
    val id: Long,
    val name: String,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val admin1: String?,
    val admin2: String?,
    val admin3: String?
)

package com.example.climaapp.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object Details : Screen("details/{cityId}/{name}/{country}/{lat}/{lon}/{region}") {
        fun createRoute(
            cityId: Long,
            name: String,
            country: String,
            lat: Double,
            lon: Double,
            region: String? = null
        ): String {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val encodedCountry = URLEncoder.encode(country, "UTF-8")
            val encodedRegion = URLEncoder.encode(region ?: "", "UTF-8")
            return "details/$cityId/$encodedName/$encodedCountry/$lat/$lon/$encodedRegion"
        }
    }
}

package com.example.climaapp.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Details : Screen("details/{cityId}/{name}/{country}/{lat}/{lon}") {
        fun createRoute(cityId: Long, name: String, country: String, lat: Double, lon: Double): String {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val encodedCountry = URLEncoder.encode(country, "UTF-8")
            return "details/$cityId/$encodedName/$encodedCountry/$lat/$lon"
        }
    }
}

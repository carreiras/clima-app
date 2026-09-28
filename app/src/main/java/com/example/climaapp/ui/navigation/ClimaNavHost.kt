package com.example.climaapp.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.climaapp.ui.details.DetailsScreen
import com.example.climaapp.ui.favorites.FavoritesScreen
import com.example.climaapp.ui.search.SearchScreen

@Composable
fun ClimaNavHost(innerPadding: PaddingValues) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Search.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Search.route) {
            SearchScreen(
                onCityClick = { city ->
                    navController.navigate(
                        Screen.Details.createRoute(
                            cityId = city.id,
                            name = city.name,
                            country = city.country,
                            lat = city.latitude,
                            lon = city.longitude
                        )
                    )
                },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) }
            )
        }
        composable(Screen.Favorites.route) { FavoritesScreen() }
        composable(
            route = Screen.Details.route,
            arguments = listOf(
                navArgument("cityId") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType },
                navArgument("country") { type = NavType.StringType },
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lon") { type = NavType.FloatType }
            )
        ) {
            DetailsScreen()
        }
    }
}

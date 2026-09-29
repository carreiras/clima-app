package com.example.climaapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.climaapp.ui.details.DetailsScreen
import com.example.climaapp.ui.favorites.FavoritesScreen
import com.example.climaapp.ui.search.SearchScreen
import com.example.climaapp.ui.settings.SettingsScreen
import java.net.URLDecoder

private val topLevelScreens = listOf(Screen.Search, Screen.Favorites, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimaNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = topLevelScreens.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = screenTitle(currentRoute, backStackEntry)) },
                navigationIcon = {
                    if (!isTopLevel && navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    topLevelScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Search.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(screenIcon(screen), contentDescription = null) },
                            label = { Text(screenLabel(screen)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
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
                                lon = city.longitude,
                                region = city.region
                            )
                        )
                    }
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onCityClick = { city ->
                        navController.navigate(
                            Screen.Details.createRoute(
                                cityId = city.id,
                                name = city.name,
                                country = city.country,
                                lat = city.latitude,
                                lon = city.longitude,
                                region = city.region
                            )
                        )
                    }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(
                route = Screen.Details.route,
                arguments = listOf(
                    navArgument("cityId") { type = NavType.LongType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("country") { type = NavType.StringType },
                    navArgument("lat") { type = NavType.FloatType },
                    navArgument("lon") { type = NavType.FloatType },
                    navArgument("region") { type = NavType.StringType }
                )
            ) {
                DetailsScreen()
            }
        }
    }
}

private fun screenLabel(screen: Screen): String = when (screen) {
    Screen.Search -> "Buscar"
    Screen.Favorites -> "Favoritos"
    Screen.Settings -> "Configurações"
    else -> ""
}

private fun screenIcon(screen: Screen) = when (screen) {
    Screen.Search -> Icons.Filled.Search
    Screen.Favorites -> Icons.Filled.Favorite
    Screen.Settings -> Icons.Filled.Settings
    else -> Icons.Filled.Search
}

private fun screenTitle(route: String?, backStackEntry: NavBackStackEntry?): String = when (route) {
    Screen.Search.route -> "Clima"
    Screen.Favorites.route -> "Favoritos"
    Screen.Settings.route -> "Configurações"
    Screen.Details.route -> backStackEntry?.arguments?.getString("name")
        ?.let { URLDecoder.decode(it, "UTF-8") } ?: "Detalhes"
    else -> "Clima"
}

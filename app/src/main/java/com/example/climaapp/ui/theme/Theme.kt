package com.example.climaapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DuskBlueLight,
    onPrimary = NavyInk,
    primaryContainer = DuskBlueContainerDark,
    onPrimaryContainer = DuskBlueLight,
    secondary = SunsetCoralLight,
    onSecondary = NavyInk,
    secondaryContainer = CoralContainerDark,
    onSecondaryContainer = SunsetCoralLight,
    tertiary = SunsetCoralLight,
    onTertiary = NavyInk,
    background = NavyInk,
    onBackground = NavyInkLight,
    surface = NavySurfaceDark,
    onSurface = NavyInkLight,
    surfaceVariant = NavySurfaceVariantDark,
    onSurfaceVariant = MutedMist,
    outline = SlateOutlineDark,
    error = ErrorRedLight,
    onError = NavyInk
)

private val LightColorScheme = lightColorScheme(
    primary = DuskBlue,
    onPrimary = SkyMist,
    primaryContainer = DuskBluePale,
    onPrimaryContainer = DuskBlue,
    secondary = SunsetCoral,
    onSecondary = SkyMist,
    secondaryContainer = CoralPale,
    onSecondaryContainer = SunsetCoral,
    tertiary = SunsetCoral,
    onTertiary = SkyMist,
    background = SkyMist,
    onBackground = NavyInk,
    surface = SkySurface,
    onSurface = NavyInk,
    surfaceVariant = SkySurfaceVariant,
    onSurfaceVariant = MutedNavy,
    outline = SlateOutline,
    error = ErrorRed,
    onError = SkyMist
)

@Composable
fun ClimaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desligado por padrão: preferimos a paleta "entardecer" do app à cor
    // extraída do papel de parede do usuário (Material You).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrismCyan,
    onPrimary = Color(0xFF003548),
    primaryContainer = Color(0xFF004D67),
    onPrimaryContainer = Color(0xFFC0E8FF),
    secondary = PrismViolet,
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Color(0xFF4F378B),
    onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = PrismPink,
    onTertiary = Color(0xFF492532),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = PrismSky,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC0E8FF),
    onPrimaryContainer = Color(0xFF001E2B),
    secondary = PrismPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DDFF),
    onSecondaryContainer = Color(0xFF23005B),
    tertiary = PrismRose,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant
)

@Composable
fun FluffyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.ui.glass.LocalColoredContainerBorders

@Composable
fun FluffyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePreset: AppThemePreset = if (darkTheme) AppThemeRegistry.defaultDark else AppThemeRegistry.defaultLight,
    coloredContainerBorders: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (themePreset.isDark) {
        darkColorScheme(
            primary = themePreset.primary,
            onPrimary = themePreset.background,
            primaryContainer = themePreset.surfaceVariant,
            onPrimaryContainer = themePreset.onSurface,
            secondary = themePreset.secondary,
            onSecondary = themePreset.background,
            secondaryContainer = themePreset.surface,
            onSecondaryContainer = themePreset.onSurface,
            tertiary = themePreset.tertiary,
            onTertiary = themePreset.background,
            background = themePreset.background,
            onBackground = themePreset.onBackground,
            surface = themePreset.surface,
            onSurface = themePreset.onSurface,
            surfaceVariant = themePreset.surfaceVariant,
            onSurfaceVariant = themePreset.onSurfaceVariant
        )
    } else {
        lightColorScheme(
            primary = themePreset.primary,
            onPrimary = Color.White,
            primaryContainer = themePreset.surfaceVariant,
            onPrimaryContainer = themePreset.onSurface,
            secondary = themePreset.secondary,
            onSecondary = Color.White,
            secondaryContainer = themePreset.surface,
            onSecondaryContainer = themePreset.onSurface,
            tertiary = themePreset.tertiary,
            onTertiary = Color.White,
            background = themePreset.background,
            onBackground = themePreset.onBackground,
            surface = themePreset.surface,
            onSurface = themePreset.onSurface,
            surfaceVariant = themePreset.surfaceVariant,
            onSurfaceVariant = themePreset.onSurfaceVariant
        )
    }

    CompositionLocalProvider(
        LocalAppThemePreset provides themePreset,
        LocalColoredContainerBorders provides coloredContainerBorders
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

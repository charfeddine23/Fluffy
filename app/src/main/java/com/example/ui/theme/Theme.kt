package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.glass.LocalColoredContainerBorders

val LocalAppFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }
val LocalAppFontOption = staticCompositionLocalOf<AppFontOption> { AppFontCatalog.defaultFont }

@Composable
fun FluffyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePreset: AppThemePreset = if (darkTheme) AppThemeRegistry.defaultDark else AppThemeRegistry.defaultLight,
    coloredContainerBorders: Boolean = true,
    fontOption: AppFontOption = AppFontCatalog.defaultFont,
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
        LocalColoredContainerBorders provides coloredContainerBorders,
        LocalAppFontFamily provides fontOption.fontFamily,
        LocalAppFontOption provides fontOption
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildAppTypography(fontOption.fontFamily),
            content = content
        )
    }
}

package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NaturalPrimary,
    onPrimary = NaturalOnPrimary,
    primaryContainer = NaturalPrimaryContainer,
    onPrimaryContainer = Color(0xFFB8F5E8),
    secondary = NaturalSecondary,
    onSecondary = Color(0xFF0A2A3D),
    secondaryContainer = Color(0xFF1E4054),
    onSecondaryContainer = NaturalSecondary,
    tertiary = NaturalTertiary,
    onTertiary = Color(0xFF33260B),
    tertiaryContainer = Color(0xFF4C3A16),
    onTertiaryContainer = NaturalTertiary,
    background = NaturalDarkBackground,
    onBackground = NaturalDarkText,
    surface = NaturalDarkSurface,
    onSurface = NaturalDarkText,
    surfaceVariant = NaturalDarkSurfaceVariant,
    onSurfaceVariant = NaturalDarkTextMuted,
    outline = NaturalDarkBorder,
    outlineVariant = NaturalDarkSurfaceElevated
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalLightPrimary,
    onPrimary = NaturalLightOnPrimary,
    primaryContainer = Color(0xFFB8F2E4),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF41627A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE4F2),
    onSecondaryContainer = Color(0xFF071E2C),
    tertiary = Color(0xFF6B4E00),
    onTertiary = Color.White,
    background = NaturalLightBackground,
    onBackground = NaturalLightText,
    surface = NaturalLightSurface,
    onSurface = NaturalLightText,
    surfaceVariant = NaturalLightSurfaceVariant,
    onSurfaceVariant = NaturalLightTextSecondary,
    outline = Color(0xFF6B7783),
    outlineVariant = Color(0xFFBFC8D1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep cohesive branded colors by default
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
        shapes = SecureMindShapes,
        content = content
    )
}

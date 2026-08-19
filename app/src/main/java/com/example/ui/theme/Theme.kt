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
    onPrimaryContainer = NaturalPrimary,
    secondary = NaturalSecondary,
    onSecondary = Color(0xFF0C314B),
    secondaryContainer = Color(0xFF264963),
    onSecondaryContainer = NaturalSecondary,
    tertiary = NaturalTertiary,
    onTertiary = Color(0xFF3C2E15),
    tertiaryContainer = Color(0xFF55442A),
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
    primaryContainer = Color(0xFFD1E8FF),
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFF4F616E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E5F5),
    onSecondaryContainer = Color(0xFF0B1D29),
    tertiary = Color(0xFF705D00),
    onTertiary = Color.White,
    background = NaturalLightBackground,
    onBackground = NaturalLightText,
    surface = NaturalLightSurface,
    onSurface = NaturalLightText,
    surfaceVariant = NaturalLightSurfaceVariant,
    onSurfaceVariant = NaturalLightTextSecondary,
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFC0C7CD)
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
        content = content
    )
}

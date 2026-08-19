package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class FontFamilyPreference(val displayName: String) {
    SERIF("Serif (Classic Book)"),
    SANS_SERIF("Sans-Serif (Modern Clean)"),
    MONOSPACE("Monospace (Code / Focus)"),
    CURSIVE("Literary Cursive")
}

data class ReaderTheme(
    val id: String,
    val name: String,
    val backgroundColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val surfaceColor: Color,
    val isDark: Boolean = false
) {
    companion object {
        val NaturalTones = ReaderTheme(
            id = "natural_tones",
            name = "Natural Graphite",
            backgroundColor = NaturalDarkBackground,
            textColor = NaturalDarkText,
            accentColor = NaturalPrimary,
            surfaceColor = NaturalDarkSurface,
            isDark = true
        )

        val NaturalSage = ReaderTheme(
            id = "natural_sage",
            name = "Natural Sage",
            backgroundColor = NaturalSageBg,
            textColor = NaturalSageAccent,
            accentColor = NaturalSageAccent,
            surfaceColor = Color(0xFF2E3729),
            isDark = true
        )

        val NaturalOchre = ReaderTheme(
            id = "natural_ochre",
            name = "Natural Sand",
            backgroundColor = Color(0xFFF7F4EB),
            textColor = Color(0xFF2C2825),
            accentColor = Color(0xFF9E651E),
            surfaceColor = Color(0xFFECE7D9),
            isDark = false
        )

        val Obsidian = ReaderTheme(
            id = "obsidian",
            name = "Obsidian Dark",
            backgroundColor = Color(0xFF0D1117),
            textColor = Color(0xFFE6EDF3),
            accentColor = Color(0xFF58A6FF),
            surfaceColor = Color(0xFF161B22),
            isDark = true
        )

        val PaperWhite = ReaderTheme(
            id = "paper_white",
            name = "Paper White",
            backgroundColor = Color(0xFFFBFBFD),
            textColor = Color(0xFF1A1F2C),
            accentColor = Color(0xFF2563EB),
            surfaceColor = Color(0xFFFFFFFF),
            isDark = false
        )

        val WarmSepia = ReaderTheme(
            id = "warm_sepia",
            name = "Warm Sepia",
            backgroundColor = Color(0xFFF5EFE0),
            textColor = Color(0xFF423425),
            accentColor = Color(0xFFD97706),
            surfaceColor = Color(0xFFECE3CE),
            isDark = false
        )

        val PitchAMOLED = ReaderTheme(
            id = "pitch_amoled",
            name = "Pitch AMOLED",
            backgroundColor = Color(0xFF000000),
            textColor = Color(0xFFE2E8F0),
            accentColor = Color(0xFF818CF8),
            surfaceColor = Color(0xFF111111),
            isDark = true
        )

        val ALL_THEMES = listOf(
            NaturalTones,
            NaturalSage,
            NaturalOchre,
            Obsidian,
            PaperWhite,
            WarmSepia,
            PitchAMOLED
        )
    }
}

data class ReaderPreferences(
    val themeId: String = ReaderTheme.NaturalTones.id,
    val fontSizeSp: Float = 17f,
    val lineSpacingMultiplier: Float = 1.6f,
    val letterSpacingSp: Float = 0.3f,
    val horizontalMarginDp: Float = 20f,
    val fontFamily: FontFamilyPreference = FontFamilyPreference.SERIF,
    val isNightLightFilter: Boolean = false,
    val brightnessLevel: Float = 1.0f,
    val isAutoScrollActive: Boolean = false,
    val autoScrollSpeedWpm: Int = 180,
    val isPagedMode: Boolean = true // Page turn vs continuous vertical scroll
)


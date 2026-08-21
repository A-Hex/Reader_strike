package com.example.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.model.FontFamilyPreference
import com.example.model.ReaderPreferences
import com.example.model.ReaderTheme
import com.example.ui.theme.*

enum class ReadingMode(val id: String, val displayName: String, val iconName: String) {
    LIGHT("light", "Light", "light_mode"),
    SEPIA("sepia", "Sepia", "menu_book"),
    DARK("dark", "Dark", "dark_mode")
}

object ThemeManager {

    private const val PREFS_NAME = "ahex_theme_manager_prefs"
    private const val KEY_PREFIX_BOOK_THEME = "book_theme_"
    private const val KEY_GLOBAL_THEME = "global_reader_theme"
    private const val KEY_GLOBAL_MODE = "global_reading_mode"

    // Standard Theme Definitions
    val PaperWhite = ReaderTheme(
        id = "paper_white",
        name = "Paper White",
        backgroundColor = Color(0xFFFBFBFD),
        textColor = Color(0xFF1A1F2C),
        accentColor = Color(0xFF2563EB),
        surfaceColor = Color(0xFFFFFFFF),
        isDark = false,
        readingMode = ReadingMode.LIGHT
    )

    val NaturalOchre = ReaderTheme(
        id = "natural_ochre",
        name = "Natural Sand",
        backgroundColor = Color(0xFFF7F4EB),
        textColor = Color(0xFF2C2825),
        accentColor = Color(0xFF9E651E),
        surfaceColor = Color(0xFFECE7D9),
        isDark = false,
        readingMode = ReadingMode.LIGHT
    )

    val WarmSepia = ReaderTheme(
        id = "warm_sepia",
        name = "Warm Sepia",
        backgroundColor = Color(0xFFF5EFE0),
        textColor = Color(0xFF423425),
        accentColor = Color(0xFFD97706),
        surfaceColor = Color(0xFFECE3CE),
        isDark = false,
        readingMode = ReadingMode.SEPIA
    )

    val VintageAmber = ReaderTheme(
        id = "vintage_amber",
        name = "Vintage Amber",
        backgroundColor = Color(0xFFEFE6D5),
        textColor = Color(0xFF382B1B),
        accentColor = Color(0xFFB45309),
        surfaceColor = Color(0xFFE5DAC4),
        isDark = false,
        readingMode = ReadingMode.SEPIA
    )

    val Obsidian = ReaderTheme(
        id = "obsidian",
        name = "Obsidian Dark",
        backgroundColor = Color(0xFF0D1117),
        textColor = Color(0xFFE6EDF3),
        accentColor = Color(0xFF58A6FF),
        surfaceColor = Color(0xFF161B22),
        isDark = true,
        readingMode = ReadingMode.DARK
    )

    val NaturalTones = ReaderTheme(
        id = "natural_tones",
        name = "Natural Graphite",
        backgroundColor = NaturalDarkBackground,
        textColor = NaturalDarkText,
        accentColor = NaturalPrimary,
        surfaceColor = NaturalDarkSurface,
        isDark = true,
        readingMode = ReadingMode.DARK
    )

    val NaturalSage = ReaderTheme(
        id = "natural_sage",
        name = "Natural Sage",
        backgroundColor = NaturalSageBg,
        textColor = NaturalSageAccent,
        accentColor = NaturalSageAccent,
        surfaceColor = Color(0xFF2E3729),
        isDark = true,
        readingMode = ReadingMode.DARK
    )

    val PitchAMOLED = ReaderTheme(
        id = "pitch_amoled",
        name = "Pitch AMOLED",
        backgroundColor = Color(0xFF000000),
        textColor = Color(0xFFE2E8F0),
        accentColor = Color(0xFF818CF8),
        surfaceColor = Color(0xFF111111),
        isDark = true,
        readingMode = ReadingMode.DARK
    )

    val ALL_THEMES = listOf(
        PaperWhite,
        NaturalOchre,
        WarmSepia,
        VintageAmber,
        Obsidian,
        NaturalTones,
        NaturalSage,
        PitchAMOLED
    )

    fun getThemeById(themeId: String): ReaderTheme {
        return ALL_THEMES.find { it.id == themeId } ?: NaturalTones
    }

    fun getThemesForMode(mode: ReadingMode): List<ReaderTheme> {
        return ALL_THEMES.filter { it.readingMode == mode }
    }

    fun getDefaultThemeForMode(mode: ReadingMode): ReaderTheme {
        return when (mode) {
            ReadingMode.LIGHT -> PaperWhite
            ReadingMode.SEPIA -> WarmSepia
            ReadingMode.DARK -> NaturalTones
        }
    }

    /**
     * Get the configured theme for a specific book.
     * Falls back to global reader theme if no book-specific theme was saved.
     */
    fun getThemeForBook(context: Context, bookId: String): ReaderTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedThemeId = prefs.getString(KEY_PREFIX_BOOK_THEME + bookId, null)
            ?: prefs.getString(KEY_GLOBAL_THEME, NaturalTones.id)
            ?: NaturalTones.id
        return getThemeById(savedThemeId)
    }

    /**
     * Save the theme preference for a specific book.
     */
    fun saveThemeForBook(context: Context, bookId: String, themeId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PREFIX_BOOK_THEME + bookId, themeId)
            .putString(KEY_GLOBAL_THEME, themeId)
            .apply()
    }

    /**
     * Switch reading mode (Light, Dark, Sepia) for the active book and return the new theme.
     */
    fun switchModeForBook(context: Context, bookId: String?, mode: ReadingMode): ReaderTheme {
        val targetTheme = getDefaultThemeForMode(mode)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_GLOBAL_THEME, targetTheme.id)
        editor.putString(KEY_GLOBAL_MODE, mode.id)
        if (!bookId.isNullOrBlank()) {
            editor.putString(KEY_PREFIX_BOOK_THEME + bookId, targetTheme.id)
        }
        editor.apply()
        return targetTheme
    }

    /**
     * PDF ColorFilter for rendering PDF pages in Light, Dark, and Sepia modes.
     */
    fun getPdfColorFilter(theme: ReaderTheme): ColorFilter? {
        return when (theme.readingMode) {
            ReadingMode.LIGHT -> null // Natural full-color rendering
            ReadingMode.SEPIA -> {
                // Warm Sepia color matrix transformation
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            0.393f * 1.1f, 0.769f * 0.9f, 0.189f * 0.7f, 0f, 20f,
                            0.349f * 1.0f, 0.686f * 0.9f, 0.168f * 0.7f, 0f, 15f,
                            0.272f * 0.8f, 0.534f * 0.8f, 0.131f * 0.6f, 0f, 5f,
                            0f,            0f,            0f,            1f, 0f
                        )
                    )
                )
            }
            ReadingMode.DARK -> {
                // Invert colors matrix for high-comfort night reading on PDF
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            -0.9f, 0f,    0f,    0f, 235f,
                            0f,    -0.9f, 0f,    0f, 235f,
                            0f,    0f,    -0.9f, 0f, 235f,
                            0f,    0f,    0f,    1f, 0f
                        )
                    )
                )
            }
        }
    }
}

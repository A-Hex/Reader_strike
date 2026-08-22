package com.example.util

import androidx.compose.ui.unit.LayoutDirection
import androidx.core.text.BidiFormatter
import java.util.Locale

object LocaleResolver {

    /**
     * Maps AppLanguage to preferred java.util.Locale for UI formatting
     */
    fun getUiLocale(language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.ENGLISH -> Locale.US
            AppLanguage.ARABIC -> Locale("ar", "SA")
            AppLanguage.FRENCH -> Locale.FRANCE
        }
    }

    /**
     * Resolves the list of candidate locales for TTS in priority order.
     */
    fun getTtsPreferredLocales(language: AppLanguage): List<Locale> {
        return when (language) {
            AppLanguage.ARABIC -> listOf(
                Locale("ar", "SA"),
                Locale("ar", "EG"),
                Locale("ar", "AE"),
                Locale("ar")
            )
            AppLanguage.FRENCH -> listOf(
                Locale.FRANCE,
                Locale.CANADA_FRENCH,
                Locale("fr")
            )
            AppLanguage.ENGLISH -> listOf(
                Locale.US,
                Locale.UK,
                Locale.CANADA,
                Locale("en")
            )
        }
    }

    /**
     * Resolves candidate locales for a specific book language code or content
     */
    fun resolveLocalesForBook(bookLanguageCode: String?, contentSample: String?): List<Locale> {
        val detected = if (!bookLanguageCode.isNullOrBlank()) {
            when {
                bookLanguageCode.startsWith("ar", ignoreCase = true) -> AppLanguage.ARABIC
                bookLanguageCode.startsWith("fr", ignoreCase = true) -> AppLanguage.FRENCH
                else -> AppLanguage.ENGLISH
            }
        } else if (!contentSample.isNullOrBlank()) {
            detectContentLanguage(contentSample)
        } else {
            AppLanguage.ENGLISH
        }
        return getTtsPreferredLocales(detected)
    }

    /**
     * Detects primary language from sample content.
     */
    fun detectContentLanguage(sample: String): AppLanguage {
        val arabicCharCount = sample.count { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\u08A0'..'\u08FF' }
        if (arabicCharCount > sample.length * 0.15 || arabicCharCount > 20) {
            return AppLanguage.ARABIC
        }

        val frenchTokens = listOf(" le ", " la ", " les ", " un ", " une ", " des ", " dans ", " avec ", " pour ", " que ", " qui ", " est ", " sont ", " été ", " d'", " l'", " c'")
        val sampleLower = " ${sample.take(1000).lowercase()} "
        val hasFrenchAccents = sample.take(1000).any { it in "éèêëàâîïôùûçÉÈÊËÀÂÎÏÔÙÛÇ" }
        val frenchHits = frenchTokens.count { sampleLower.contains(it) }

        if (hasFrenchAccents && frenchHits >= 2) {
            return AppLanguage.FRENCH
        }

        return AppLanguage.ENGLISH
    }

    /**
     * Detects reading layout direction strictly from text content.
     * Guarantees that English books render LTR in Arabic UI, and Arabic books render RTL in English UI.
     */
    fun detectContentLayoutDirection(content: String): LayoutDirection {
        val arabicCount = content.take(300).count { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' }
        return if (arabicCount > 5) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    /**
     * Safe Bidi wrapper for mixed text (book titles, author names, numbers, paths).
     */
    fun unicodeWrap(text: String, isRtlUi: Boolean = false): String {
        return try {
            val formatter = BidiFormatter.getInstance(isRtlUi)
            formatter.unicodeWrap(text)
        } catch (_: Exception) {
            text
        }
    }
}

package com.example.ai

import android.content.Context
import com.example.BuildConfig

/**
 * Resolves the Gemini API key.
 *
 * Priority:
 *  1. Runtime override saved by the user in Settings → Gemini API Key (survives without a rebuild).
 *  2. `BuildConfig.GEMINI_API_KEY`, populated from the Gradle property, the `GEMINI_API_KEY`
 *     environment variable, or the `.env` file that the build injects.
 *
 * The key is never logged and never written into exportable app data.
 */
object AiCredentials {

    private const val PREFS_NAME = "ahex_ai_settings"
    private const val PREF_API_KEY = "gemini_api_key"

    fun apiKey(context: Context): String {
        val stored = prefs(context).getString(PREF_API_KEY, null)?.trim()
        if (!stored.isNullOrBlank()) return stored

        return BuildConfig.GEMINI_API_KEY.trim()
    }

    fun hasApiKey(context: Context): Boolean = apiKey(context).isNotBlank()

    /** The key the user typed into Settings, or null when only a build-time key exists. */
    fun runtimeApiKey(context: Context): String? =
        prefs(context).getString(PREF_API_KEY, null)?.trim()?.takeIf { it.isNotBlank() }

    fun hasRuntimeApiKey(context: Context): Boolean = runtimeApiKey(context) != null

    /**
     * A non-reversible display form of the active key so Settings can show *which* key is in
     * use without ever rendering the credential itself.
     */
    fun maskedApiKey(context: Context): String? {
        val key = apiKey(context).takeIf { it.isNotBlank() } ?: return null
        if (key.length <= 8) return "••••"
        return "${key.take(4)}••••${key.takeLast(4)}"
    }

    /** Where the active key came from, so Settings can explain what clearing will do. */
    fun isUsingBuildTimeKey(context: Context): Boolean =
        !hasRuntimeApiKey(context) && hasBuildTimeApiKey()

    fun setRuntimeApiKey(context: Context, key: String?) {
        val clean = key?.trim()
        prefs(context).edit().apply {
            if (clean.isNullOrBlank()) remove(PREF_API_KEY) else putString(PREF_API_KEY, clean)
        }.apply()
    }

    fun clearRuntimeApiKey(context: Context) = setRuntimeApiKey(context, null)

    /** Build-time key only; used to explain setup problems to the user without leaking the key. */
    fun hasBuildTimeApiKey(): Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

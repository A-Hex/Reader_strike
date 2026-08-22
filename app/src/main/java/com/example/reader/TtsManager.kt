package com.example.reader

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.util.AppLanguage
import com.example.util.LocaleResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

sealed class TtsEngineState {
    object Initializing : TtsEngineState()
    data class Ready(val locale: Locale, val voiceName: String?) : TtsEngineState()
    data class Playing(val segmentIndex: Int, val totalSegments: Int, val segment: TextSegment?) : TtsEngineState()
    data class Paused(val segmentIndex: Int, val totalSegments: Int, val segment: TextSegment?) : TtsEngineState()
    object Completed : TtsEngineState()
    data class Unsupported(val locale: Locale) : TtsEngineState()
    data class MissingVoiceData(val locale: Locale) : TtsEngineState()
    data class Error(val message: String) : TtsEngineState()
}

class TtsManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _engineState = MutableStateFlow<TtsEngineState>(TtsEngineState.Initializing)
    val engineState: StateFlow<TtsEngineState> = _engineState.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private var currentSegments: List<TextSegment> = emptyList()
    private var currentSegmentIndex = 0
    private var currentSessionToken = UUID.randomUUID().toString()
    private var currentConfiguredLocale: Locale = Locale.US

    init {
        initTts(Locale.getDefault())
    }

    fun initTts(preferredLocale: Locale) {
        currentConfiguredLocale = preferredLocale
        _engineState.value = TtsEngineState.Initializing

        try {
            tts?.shutdown()
        } catch (_: Exception) {}

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                applyAudioAttributes()
                configureLocaleAndVoice(currentConfiguredLocale)
                setupProgressListener()
            } else {
                isInitialized = false
                _engineState.value = TtsEngineState.Error("Text-to-speech engine failed to initialize.")
            }
        }
    }

    private fun applyAudioAttributes() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Configures the voice engine for the requested language or candidate locales.
     * Does NOT silently fallback to US English if Arabic or French is requested.
     */
    fun configureForLanguage(language: AppLanguage, candidateLocales: List<Locale>? = null): Boolean {
        val candidates = candidateLocales ?: LocaleResolver.getTtsPreferredLocales(language)
        for (targetLocale in candidates) {
            if (configureLocaleAndVoice(targetLocale)) {
                return true
            }
        }
        return false
    }

    private fun configureLocaleAndVoice(targetLocale: Locale): Boolean {
        val currentTts = tts ?: return false
        val availability = currentTts.isLanguageAvailable(targetLocale)

        when (availability) {
            TextToSpeech.LANG_MISSING_DATA -> {
                _engineState.value = TtsEngineState.MissingVoiceData(targetLocale)
                return false
            }
            TextToSpeech.LANG_NOT_SUPPORTED -> {
                _engineState.value = TtsEngineState.Unsupported(targetLocale)
                return false
            }
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                currentTts.language = targetLocale
                currentConfiguredLocale = targetLocale

                // Select the best matching high quality voice
                var chosenVoiceName: String? = null
                try {
                    val matchingVoices = currentTts.voices?.filter { it.locale.language == targetLocale.language }
                    val highQualityVoice = matchingVoices?.firstOrNull { it.quality >= Voice.QUALITY_HIGH && !it.isNetworkConnectionRequired }
                        ?: matchingVoices?.firstOrNull { !it.isNetworkConnectionRequired }
                        ?: matchingVoices?.firstOrNull()

                    if (highQualityVoice != null) {
                        currentTts.voice = highQualityVoice
                        chosenVoiceName = highQualityVoice.name
                    }
                } catch (_: Exception) {}

                currentTts.setSpeechRate(_speechRate.value)
                currentTts.setPitch(_speechPitch.value)
                _engineState.value = TtsEngineState.Ready(targetLocale, chosenVoiceName)
                return true
            }
            else -> {
                _engineState.value = TtsEngineState.Unsupported(targetLocale)
                return false
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == null || !utteranceId.startsWith(currentSessionToken)) return
                val idx = utteranceId.substringAfterLast("_").toIntOrNull() ?: currentSegmentIndex
                currentSegmentIndex = idx
                val segment = currentSegments.getOrNull(idx)
                _engineState.value = TtsEngineState.Playing(idx, currentSegments.size, segment)
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == null || !utteranceId.startsWith(currentSessionToken)) return
                val idx = utteranceId.substringAfterLast("_").toIntOrNull() ?: currentSegmentIndex
                if (idx + 1 < currentSegments.size) {
                    speakSegment(idx + 1)
                } else {
                    _engineState.value = TtsEngineState.Completed
                }
            }

            override fun onError(utteranceId: String?) {
                if (utteranceId == null || !utteranceId.startsWith(currentSessionToken)) return
                _engineState.value = TtsEngineState.Error("TTS playback interrupted")
            }
        })
    }

    fun startReadingSegments(
        segments: List<TextSegment>,
        startSegmentIndex: Int = 0,
        preferredLanguage: AppLanguage? = null,
        bookLanguageCode: String? = null
    ) {
        currentSessionToken = UUID.randomUUID().toString()
        currentSegments = segments

        if (segments.isEmpty()) {
            _engineState.value = TtsEngineState.Completed
            return
        }

        // Configure candidate locales based on text/book language
        val sampleText = segments.take(3).joinToString(" ") { it.text }
        val candidateLocales = if (preferredLanguage != null) {
            LocaleResolver.getTtsPreferredLocales(preferredLanguage)
        } else {
            LocaleResolver.resolveLocalesForBook(bookLanguageCode, sampleText)
        }

        val configured = candidateLocales.any { configureLocaleAndVoice(it) }
        if (!configured && (_engineState.value is TtsEngineState.MissingVoiceData || _engineState.value is TtsEngineState.Unsupported)) {
            // Keep the actionable error state for the user
            return
        }

        val validStart = startSegmentIndex.coerceIn(0, segments.size - 1)
        speakSegment(validStart)
    }

    private fun speakSegment(index: Int) {
        if (index < 0 || index >= currentSegments.size) return
        currentSegmentIndex = index
        val segment = currentSegments[index]
        val utteranceId = "${currentSessionToken}_seg_$index"

        val params = android.os.Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        tts?.speak(segment.text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun pause() {
        tts?.stop()
        val seg = currentSegments.getOrNull(currentSegmentIndex)
        _engineState.value = TtsEngineState.Paused(currentSegmentIndex, currentSegments.size, seg)
    }

    fun resume() {
        if (currentSegments.isNotEmpty()) {
            speakSegment(currentSegmentIndex)
        }
    }

    fun nextSegment() {
        if (currentSegmentIndex + 1 < currentSegments.size) {
            speakSegment(currentSegmentIndex + 1)
        }
    }

    fun previousSegment() {
        if (currentSegmentIndex - 1 >= 0) {
            speakSegment(currentSegmentIndex - 1)
        }
    }

    fun seekToSegment(index: Int) {
        if (index in currentSegments.indices) {
            speakSegment(index)
        }
    }

    fun setSpeed(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.5f)
        _speechRate.value = clamped
        tts?.setSpeechRate(clamped)
    }

    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        _speechPitch.value = clamped
        tts?.setPitch(clamped)
    }

    fun stop() {
        currentSessionToken = UUID.randomUUID().toString()
        tts?.stop()
        _engineState.value = TtsEngineState.Ready(currentConfiguredLocale, null)
    }

    fun getVoiceDataInstallIntent(): Intent {
        return Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tts = null
        isInitialized = false
    }
}

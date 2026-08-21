package com.example.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var sentences: List<String> = emptyList()

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val currentTts = tts ?: return@TextToSpeech
                var langResult = currentTts.setLanguage(Locale.getDefault())
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to US English
                    langResult = currentTts.setLanguage(Locale.US)
                }

                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    _errorMessage.value = "TTS voice data is missing or unsupported for this locale."
                } else {
                    currentTts.setSpeechRate(_speechRate.value)
                    isInitialized = true
                    _errorMessage.value = null
                    setupProgressListener()
                }
            } else {
                _errorMessage.value = "Text-to-speech engine failed to initialize."
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                val index = utteranceId?.substringAfter("sentence_")?.toIntOrNull()
                if (index != null) {
                    _currentSentenceIndex.value = index
                }
            }

            override fun onDone(utteranceId: String?) {
                val index = utteranceId?.substringAfter("sentence_")?.toIntOrNull() ?: 0
                if (index + 1 < sentences.size) {
                    speakSentence(index + 1)
                } else {
                    _isPlaying.value = false
                    _currentSentenceIndex.value = 0
                }
            }

            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
            }
        })
    }

    fun startReading(text: String, startIndex: Int = 0) {
        if (!isInitialized) {
            _errorMessage.value = "TTS is initializing, please try again."
            return
        }
        sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        if (sentences.isEmpty()) return

        val validStart = startIndex.coerceIn(0, sentences.size - 1)
        speakSentence(validStart)
    }

    private fun speakSentence(index: Int) {
        if (index < 0 || index >= sentences.size) return
        _currentSentenceIndex.value = index
        _isPlaying.value = true
        val utteranceId = "sentence_$index"
        tts?.speak(sentences[index], TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun pause() {
        tts?.stop()
        _isPlaying.value = false
    }

    fun resume() {
        speakSentence(_currentSentenceIndex.value)
    }

    fun nextSentence() {
        if (_currentSentenceIndex.value + 1 < sentences.size) {
            speakSentence(_currentSentenceIndex.value + 1)
        }
    }

    fun previousSentence() {
        if (_currentSentenceIndex.value - 1 >= 0) {
            speakSentence(_currentSentenceIndex.value - 1)
        }
    }

    fun setSpeed(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.5f)
        _speechRate.value = clamped
        tts?.setSpeechRate(clamped)
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
        _currentSentenceIndex.value = 0
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

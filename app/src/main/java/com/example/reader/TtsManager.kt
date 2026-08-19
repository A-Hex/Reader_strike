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

    private var sentences: List<String> = emptyList()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(_speechRate.value)
                isInitialized = true
                setupProgressListener()
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
        if (!isInitialized) return
        sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
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
        tts?.shutdown()
        tts = null
    }
}

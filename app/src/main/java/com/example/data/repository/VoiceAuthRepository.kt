package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.reader.VoiceprintEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class VoiceAuthRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("voice_auth_secure_prefs", Context.MODE_PRIVATE)
    private val engine = VoiceprintEngine()

    private val _isEnrolled = MutableStateFlow(false)
    val isEnrolled: StateFlow<Boolean> = _isEnrolled.asStateFlow()

    private val _isVoiceAuthEnabled = MutableStateFlow(false)
    val isVoiceAuthEnabled: StateFlow<Boolean> = _isVoiceAuthEnabled.asStateFlow()

    var isVerified: Boolean = false
        private set

    init {
        _isEnrolled.value = prefs.contains("enrolled_voiceprint_vector")
        _isVoiceAuthEnabled.value = prefs.getBoolean("voice_auth_enabled", false)
    }

    fun setVoiceAuthEnabled(enabled: Boolean) {
        _isVoiceAuthEnabled.value = enabled
        prefs.edit().putBoolean("voice_auth_enabled", enabled).apply()
    }

    fun enrollUser(audioSamples: FloatArray): Boolean {
        if (audioSamples.isEmpty()) return false
        val embedding = engine.extractEmbedding(audioSamples)

        val jsonArray = JSONArray()
        for (value in embedding) {
            jsonArray.put(value.toDouble())
        }

        prefs.edit()
            .putString("enrolled_voiceprint_vector", jsonArray.toString())
            .putLong("enrolled_timestamp", System.currentTimeMillis())
            .apply()

        _isEnrolled.value = true
        return true
    }

    fun verifyUser(audioSamples: FloatArray, threshold: Float = VoiceprintEngine.DEFAULT_SIMILARITY_THRESHOLD): Pair<Boolean, Float> {
        val enrolledJson = prefs.getString("enrolled_voiceprint_vector", null) ?: return Pair(false, 0.0f)
        try {
            val arr = JSONArray(enrolledJson)
            val enrolledVector = FloatArray(arr.length())
            for (i in 0 until arr.length()) {
                enrolledVector[i] = arr.getDouble(i).toFloat()
            }

            val testVector = engine.extractEmbedding(audioSamples)
            val similarity = engine.calculateCosineSimilarity(enrolledVector, testVector)
            val isMatch = similarity >= threshold
            if (isMatch) {
                isVerified = true
            }

            return Pair(isMatch, similarity)
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(false, 0.0f)
        }
    }

    fun deleteVoiceprint() {
        prefs.edit()
            .remove("enrolled_voiceprint_vector")
            .remove("enrolled_timestamp")
            .putBoolean("voice_auth_enabled", false)
            .apply()

        _isEnrolled.value = false
        _isVoiceAuthEnabled.value = false
    }
}

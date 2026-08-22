package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.CustomVoiceProfile
import com.example.model.VoiceMode
import com.example.reader.VoiceProfileEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class VoiceProfileRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("voice_narration_profile_prefs", Context.MODE_PRIVATE)
    private val engine = VoiceProfileEngine()

    private val _voiceProfile = MutableStateFlow<CustomVoiceProfile?>(null)
    val voiceProfile: StateFlow<CustomVoiceProfile?> = _voiceProfile.asStateFlow()

    private val _voiceMode = MutableStateFlow(VoiceMode.SYSTEM_DEFAULT)
    val voiceMode: StateFlow<VoiceMode> = _voiceMode.asStateFlow()

    init {
        loadSavedProfile()
    }

    private fun loadSavedProfile() {
        val hasProfile = prefs.getBoolean("has_voice_profile", false)
        if (hasProfile) {
            val name = prefs.getString("voice_profile_name", "My Voice Narrator") ?: "My Voice Narrator"
            val timestamp = prefs.getLong("voice_profile_timestamp", System.currentTimeMillis())
            val pitch = prefs.getFloat("voice_profile_pitch", 1.0f)
            val speed = prefs.getFloat("voice_profile_speed", 1.0f)
            val timbre = prefs.getString("voice_profile_timbre", "Warm & Natural") ?: "Warm & Natural"
            val durationSec = prefs.getInt("voice_profile_duration", 3)
            val isActive = prefs.getBoolean("voice_profile_active", true)

            val embeddingJson = prefs.getString("voice_profile_embedding", null)
            val embedding = if (embeddingJson != null) {
                try {
                    val arr = JSONArray(embeddingJson)
                    FloatArray(arr.length()) { i -> arr.getDouble(i).toFloat() }
                } catch (_: Exception) {
                    FloatArray(0)
                }
            } else {
                FloatArray(0)
            }

            val profile = CustomVoiceProfile(
                id = prefs.getString("voice_profile_id", "default_user_voice") ?: "default_user_voice",
                name = name,
                sampleRecordedTimestamp = timestamp,
                durationSeconds = durationSec,
                estimatedPitch = pitch,
                timbreDescriptor = timbre,
                preferredSpeed = speed,
                acousticEmbedding = embedding,
                isClonedVoiceActive = isActive
            )
            _voiceProfile.value = profile
            _voiceMode.value = if (isActive) VoiceMode.USER_CLONED_VOICE else VoiceMode.SYSTEM_DEFAULT
        } else {
            _voiceProfile.value = null
            _voiceMode.value = VoiceMode.SYSTEM_DEFAULT
        }
    }

    fun trainAndSaveProfile(audioSamples: FloatArray, name: String = "My Voice Narrator"): CustomVoiceProfile? {
        if (audioSamples.isEmpty()) return null
        val profile = engine.analyzeAndCreateProfile(audioSamples, voiceName = name)

        val jsonArray = JSONArray()
        for (value in profile.acousticEmbedding) {
            jsonArray.put(value.toDouble())
        }

        prefs.edit()
            .putBoolean("has_voice_profile", true)
            .putString("voice_profile_id", profile.id)
            .putString("voice_profile_name", profile.name)
            .putLong("voice_profile_timestamp", profile.sampleRecordedTimestamp)
            .putFloat("voice_profile_pitch", profile.estimatedPitch)
            .putFloat("voice_profile_speed", profile.preferredSpeed)
            .putString("voice_profile_timbre", profile.timbreDescriptor)
            .putInt("voice_profile_duration", profile.durationSeconds)
            .putBoolean("voice_profile_active", true)
            .putString("voice_profile_embedding", jsonArray.toString())
            .apply()

        _voiceProfile.value = profile
        _voiceMode.value = VoiceMode.USER_CLONED_VOICE
        return profile
    }

    fun setVoiceMode(mode: VoiceMode) {
        _voiceMode.value = mode
        val current = _voiceProfile.value
        if (current != null) {
            val updated = current.copy(isClonedVoiceActive = (mode == VoiceMode.USER_CLONED_VOICE))
            _voiceProfile.value = updated
            prefs.edit().putBoolean("voice_profile_active", updated.isClonedVoiceActive).apply()
        }
    }

    fun updatePitch(pitch: Float) {
        val current = _voiceProfile.value ?: return
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        val updated = current.copy(estimatedPitch = clamped)
        _voiceProfile.value = updated
        prefs.edit().putFloat("voice_profile_pitch", clamped).apply()
    }

    fun updateSpeed(speed: Float) {
        val current = _voiceProfile.value ?: return
        val clamped = speed.coerceIn(0.5f, 2.5f)
        val updated = current.copy(preferredSpeed = clamped)
        _voiceProfile.value = updated
        prefs.edit().putFloat("voice_profile_speed", clamped).apply()
    }

    fun deleteProfile() {
        prefs.edit()
            .remove("has_voice_profile")
            .remove("voice_profile_id")
            .remove("voice_profile_name")
            .remove("voice_profile_timestamp")
            .remove("voice_profile_pitch")
            .remove("voice_profile_speed")
            .remove("voice_profile_timbre")
            .remove("voice_profile_duration")
            .remove("voice_profile_active")
            .remove("voice_profile_embedding")
            .apply()

        _voiceProfile.value = null
        _voiceMode.value = VoiceMode.SYSTEM_DEFAULT
    }
}

package com.example.model

/**
 * A measured vocal profile for the text-to-speech narrator.
 *
 * The engine measures the user's fundamental pitch from a recorded sample and uses it to tune
 * the system TTS engine's pitch/rate. This is pitch-matched narration — NOT voice cloning.
 * Android's system TTS cannot reproduce a user's timbre, so the app never claims it does.
 */
data class CustomVoiceProfile(
    val id: String = "default_user_voice",
    val name: String = "My Narration Profile",
    val sampleRecordedTimestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 3,
    val estimatedPitch: Float = 1.0f,
    val timbreDescriptor: String = "Warm & Natural",
    val preferredSpeed: Float = 1.0f,
    val acousticEmbedding: FloatArray = FloatArray(0),
    val isPitchMatchedNarrationActive: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomVoiceProfile

        if (id != other.id) return false
        if (name != other.name) return false
        if (sampleRecordedTimestamp != other.sampleRecordedTimestamp) return false
        if (durationSeconds != other.durationSeconds) return false
        if (estimatedPitch != other.estimatedPitch) return false
        if (timbreDescriptor != other.timbreDescriptor) return false
        if (preferredSpeed != other.preferredSpeed) return false
        if (!acousticEmbedding.contentEquals(other.acousticEmbedding)) return false
        if (isPitchMatchedNarrationActive != other.isPitchMatchedNarrationActive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + sampleRecordedTimestamp.hashCode()
        result = 31 * result + durationSeconds
        result = 31 * result + estimatedPitch.hashCode()
        result = 31 * result + timbreDescriptor.hashCode()
        result = 31 * result + preferredSpeed.hashCode()
        result = 31 * result + acousticEmbedding.contentHashCode()
        result = 31 * result + isPitchMatchedNarrationActive.hashCode()
        return result
    }
}

enum class VoiceMode {
    SYSTEM_DEFAULT,
    PITCH_MATCHED
}

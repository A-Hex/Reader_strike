package com.example.model

data class CustomVoiceProfile(
    val id: String = "default_user_voice",
    val name: String = "My Voice Narrator",
    val sampleRecordedTimestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 3,
    val estimatedPitch: Float = 1.0f,
    val timbreDescriptor: String = "Warm & Natural",
    val preferredSpeed: Float = 1.0f,
    val acousticEmbedding: FloatArray = FloatArray(0),
    val isClonedVoiceActive: Boolean = true
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
        if (isClonedVoiceActive != other.isClonedVoiceActive) return false

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
        result = 31 * result + isClonedVoiceActive.hashCode()
        return result
    }
}

enum class VoiceMode {
    SYSTEM_DEFAULT,
    USER_CLONED_VOICE
}

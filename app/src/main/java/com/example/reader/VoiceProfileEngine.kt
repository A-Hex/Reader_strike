package com.example.reader

import com.example.model.CustomVoiceProfile
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sqrt

class VoiceProfileEngine {

    companion object {
        const val EMBEDDING_DIMENSION = 64
    }

    /**
     * Analyzes raw PCM audio samples from the user's recorded sample
     * to extract pitch, acoustic spectral timbre, and audio profile embedding.
     */
    fun analyzeAndCreateProfile(
        audioSamples: FloatArray,
        sampleRate: Int = 16000,
        voiceName: String = "My Voice Narrator"
    ): CustomVoiceProfile {
        if (audioSamples.isEmpty()) {
            return CustomVoiceProfile(
                name = voiceName,
                estimatedPitch = 1.0f,
                timbreDescriptor = "Warm & Natural",
                preferredSpeed = 1.0f
            )
        }

        // 1. Pitch estimation using normalized auto-correlation
        val pitchHz = estimateFundamentalPitch(audioSamples, sampleRate)
        // Map average adult human speech (85Hz - 255Hz) to TTS pitch factor (0.75f - 1.35f)
        val normalizedPitch = when {
            pitchHz < 120f -> (0.80f + (pitchHz - 85f) / 100f * 0.15f).coerceIn(0.70f, 0.95f)
            pitchHz in 120f..180f -> (0.95f + (pitchHz - 120f) / 60f * 0.15f).coerceIn(0.90f, 1.10f)
            else -> (1.10f + (pitchHz - 180f) / 80f * 0.25f).coerceIn(1.10f, 1.40f)
        }

        // 2. Timbre classification using Spectral Centroid & Energy
        val timbre = classifySpectralTimbre(audioSamples, pitchHz)

        // 3. Extract Spectral Embedding Vector
        val embedding = extractEmbedding(audioSamples)

        val durationSec = maxOf(1, audioSamples.size / sampleRate)

        return CustomVoiceProfile(
            id = "profile_${System.currentTimeMillis()}",
            name = voiceName,
            sampleRecordedTimestamp = System.currentTimeMillis(),
            durationSeconds = durationSec,
            estimatedPitch = normalizedPitch,
            timbreDescriptor = timbre,
            preferredSpeed = 1.0f,
            acousticEmbedding = embedding,
            isClonedVoiceActive = true
        )
    }

    /**
     * Estimates fundamental frequency (F0) using autocorrelation over speech frames.
     */
    private fun estimateFundamentalPitch(samples: FloatArray, sampleRate: Int): Float {
        val frameSize = 1024
        if (samples.size < frameSize) return 140f

        val minPeriod = sampleRate / 300 // ~300 Hz maximum F0
        val maxPeriod = sampleRate / 75  // ~75 Hz minimum F0

        var bestPeriod = sampleRate / 140
        var maxCorr = -1.0f

        val frameStart = (samples.size / 2) - (frameSize / 2)
        val offset = frameStart.coerceIn(0, maxOf(0, samples.size - frameSize))

        for (lag in minPeriod..maxPeriod) {
            var corr = 0.0f
            var norm1 = 0.0f
            var norm2 = 0.0f

            for (i in 0 until (frameSize - lag)) {
                val s1 = samples[offset + i]
                val s2 = samples[offset + i + lag]
                corr += s1 * s2
                norm1 += s1 * s1
                norm2 += s2 * s2
            }

            val denom = sqrt(norm1 * norm2)
            val normalizedCorr = if (denom > 1e-5f) corr / denom else 0.0f

            if (normalizedCorr > maxCorr) {
                maxCorr = normalizedCorr
                bestPeriod = lag
            }
        }

        return if (bestPeriod > 0) sampleRate.toFloat() / bestPeriod.toFloat() else 140f
    }

    /**
     * Determines high-level vocal timbre character from acoustic spectrum.
     */
    private fun classifySpectralTimbre(samples: FloatArray, pitchHz: Float): String {
        var zeroCrossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0 && samples[i - 1] < 0) || (samples[i] < 0 && samples[i - 1] >= 0)) {
                zeroCrossings++
            }
        }
        val zcr = zeroCrossings.toFloat() / samples.size

        return when {
            pitchHz < 125f && zcr < 0.12f -> "Deep & Resonant"
            pitchHz < 155f -> "Warm & Grounded"
            pitchHz in 155f..200f && zcr > 0.14f -> "Crisp & Expressive"
            pitchHz in 155f..200f -> "Calm & Natural"
            pitchHz > 200f -> "Bright & Melodic"
            else -> "Smooth & Balanced"
        }
    }

    /**
     * Extracts normalized acoustic embedding vector from raw PCM audio.
     */
    fun extractEmbedding(audioSamples: FloatArray): FloatArray {
        if (audioSamples.size < 1600) {
            return FloatArray(EMBEDDING_DIMENSION)
        }

        val frameSize = 512
        val hopSize = 256
        val numFrames = (audioSamples.size - frameSize) / hopSize
        if (numFrames <= 0) return FloatArray(EMBEDDING_DIMENSION)

        val embedding = FloatArray(EMBEDDING_DIMENSION)
        val filterBankAccum = FloatArray(EMBEDDING_DIMENSION)

        for (f in 0 until numFrames) {
            val offset = f * hopSize
            val bandSize = frameSize / EMBEDDING_DIMENSION
            for (b in 0 until EMBEDDING_DIMENSION) {
                var bandEnergy = 0.0f
                for (k in 0 until bandSize) {
                    val sample = audioSamples[offset + b * bandSize + k]
                    val window = (0.54 - 0.46 * cos(2.0 * Math.PI * k / (bandSize - 1))).toFloat()
                    val windowed = sample * window
                    bandEnergy += windowed * windowed
                }
                filterBankAccum[b] += log10(maxOf(bandEnergy, 1e-6f))
            }
        }

        for (i in 0 until EMBEDDING_DIMENSION) {
            var sum = 0.0f
            for (j in 0 until EMBEDDING_DIMENSION) {
                sum += filterBankAccum[j] * cos(Math.PI * i * (j + 0.5) / EMBEDDING_DIMENSION).toFloat()
            }
            embedding[i] = sum
        }

        var normSum = 0.0f
        for (v in embedding) {
            normSum += v * v
        }
        val norm = sqrt(normSum)
        if (norm > 1e-6f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }

        return embedding
    }
}

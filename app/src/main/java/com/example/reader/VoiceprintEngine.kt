package com.example.reader

import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sqrt

class VoiceprintEngine {

    companion object {
        const val EMBEDDING_DIMENSION = 64
        const val DEFAULT_SIMILARITY_THRESHOLD = 0.72f
    }

    /**
     * Extracts a 1D normalized speaker embedding vector from raw PCM float samples.
     * Uses a multi-band spectral energy & mel-frequency filterbank representation.
     * 100% offline, zero cloud calls.
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

        // Process short-time frames
        for (f in 0 until numFrames) {
            val offset = f * hopSize
            // Calculate band energies
            val bandSize = frameSize / EMBEDDING_DIMENSION
            for (b in 0 until EMBEDDING_DIMENSION) {
                var bandEnergy = 0.0f
                for (k in 0 until bandSize) {
                    val sample = audioSamples[offset + b * bandSize + k]
                    // Apply Hamming window
                    val window = (0.54 - 0.46 * cos(2.0 * Math.PI * k / (bandSize - 1))).toFloat()
                    val windowed = sample * window
                    bandEnergy += windowed * windowed
                }
                filterBankAccum[b] += log10(maxOf(bandEnergy, 1e-6f))
            }
        }

        // Average across frames and apply Discrete Cosine Transform (DCT)
        for (i in 0 until EMBEDDING_DIMENSION) {
            var sum = 0.0f
            for (j in 0 until EMBEDDING_DIMENSION) {
                sum += filterBankAccum[j] * cos(Math.PI * i * (j + 0.5) / EMBEDDING_DIMENSION).toFloat()
            }
            embedding[i] = sum
        }

        // L2 normalize the embedding vector
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

    /**
     * Computes the Cosine Similarity between two 1D FloatArray embedding vectors.
     * Cosine Similarity = (A · B) / (||A|| * ||B||)
     */
    fun calculateCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.isEmpty() || vectorB.isEmpty() || vectorA.size != vectorB.size) {
            return 0.0f
        }

        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }

        val denominator = (sqrt(normA) * sqrt(normB))
        if (denominator <= 1e-6f) {
            return 0.0f
        }

        return (dotProduct / denominator).coerceIn(-1.0f, 1.0f)
    }
}

package com.example.reader

data class RsvpToken(
    val id: String,
    val text: String,
    val isRtl: Boolean,
    val delayMultiplier: Float,
    val focalCharIndex: Int
)

data class RsvpPlaybackState(
    val currentTokenIndex: Int = 0,
    val totalTokens: Int = 0,
    val wpm: Int = 350,
    val isPlaying: Boolean = false,
    val elapsedSeconds: Int = 0,
    val estimatedRemainingSeconds: Int = 0,
    val completionPercent: Int = 0,
    val currentToken: RsvpToken? = null
)

object RsvpSpeedReaderEngine {

    private fun isArabicDiacritic(c: Char): Boolean {
        return (c in '\u064B'..'\u065F') || c == '\u0670' || (c in '\u06D6'..'\u06ED')
    }

    /**
     * Tokenizes text for RSVP speed reading.
     * Preserves Arabic script ligatures, Tashkeel/diacritics, and handles French contractions/punctuation.
     */
    fun tokenize(content: String): List<RsvpToken> {
        if (content.isBlank()) return emptyList()

        // Match words along with attached punctuation
        val rawTokens = content.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val tokens = mutableListOf<RsvpToken>()

        rawTokens.forEachIndexed { index, raw ->
            val isRtl = raw.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' }
            
            // Calculate delay multiplier based on punctuation
            val delayMultiplier = when {
                raw.endsWith(".") || raw.endsWith("!") || raw.endsWith("?") || raw.endsWith("؟") || raw.endsWith("»") -> 1.6f
                raw.endsWith(",") || raw.endsWith(";") || raw.endsWith(":") || raw.endsWith("؛") || raw.endsWith("،") -> 1.3f
                raw.length > 12 -> 1.2f
                else -> 1.0f
            }

            // ORP calculation (For LTR: index ~30%; For RTL: natural center of base graphemes)
            val focalIndex = if (isRtl) {
                // Count base characters excluding combining diacritics
                val baseIndices = raw.indices.filter { !isArabicDiacritic(raw[it]) }
                if (baseIndices.isNotEmpty()) {
                    baseIndices[baseIndices.size / 2]
                } else {
                    0
                }
            } else {
                when (raw.length) {
                    1 -> 0
                    2, 3 -> 1
                    4, 5 -> 1
                    6, 7 -> 2
                    8, 9 -> 3
                    else -> 4
                }.coerceIn(0, (raw.length - 1).coerceAtLeast(0))
            }

            tokens.add(
                RsvpToken(
                    id = "rsvp_tok_$index",
                    text = raw,
                    isRtl = isRtl,
                    delayMultiplier = delayMultiplier,
                    focalCharIndex = focalIndex
                )
            )
        }

        return tokens
    }

    /**
     * Calculates estimated remaining seconds considering punctuation delays.
     */
    fun calculateRemainingSeconds(tokens: List<RsvpToken>, currentIndex: Int, wpm: Int): Int {
        if (currentIndex >= tokens.size || wpm <= 0) return 0
        val baseMsPerWord = 60_000.0 / wpm
        var remainingMs = 0.0
        for (i in currentIndex until tokens.size) {
            remainingMs += baseMsPerWord * tokens[i].delayMultiplier
        }
        return (remainingMs / 1000.0).toInt().coerceAtLeast(0)
    }

    /**
     * Calculates completion percentage ensuring it reaches 100% after the final token.
     */
    fun calculateProgressPercent(currentIndex: Int, totalTokens: Int): Int {
        if (totalTokens <= 0) return 0
        if (currentIndex >= totalTokens - 1) return 100
        return ((currentIndex.toFloat() / (totalTokens - 1).coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
    }
}

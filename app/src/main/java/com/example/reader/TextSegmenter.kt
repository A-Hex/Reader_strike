package com.example.reader

data class TextSegment(
    val id: String,
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val text: String,
    val startCharOffset: Int,
    val endCharOffset: Int
)

object TextSegmenter {

    private const val MAX_TTS_CHUNK_LENGTH = 3500

    /**
     * Splits text into synchronized segments with paragraph and sentence indices.
     * Accurately supports Arabic, French, and English punctuation marks.
     */
    fun segment(rawText: String): List<TextSegment> {
        if (rawText.isBlank()) return emptyList()

        val segments = mutableListOf<TextSegment>()
        val paragraphs = rawText.split("\n\n", "\n")
        var globalCharOffset = 0
        var globalSentenceIndex = 0

        // Comprehensive punctuation matching English (.!?), Arabic (؟؛!), French («».!?)
        val sentenceSplitter = Regex("(?<=[.!?؟؛])\\s+|(?<=[»])\\s+")

        paragraphs.forEachIndexed { pIndex, paragraph ->
            val pTrimmed = paragraph.trim()
            if (pTrimmed.isNotBlank()) {
                val rawSentences = pTrimmed.split(sentenceSplitter).map { it.trim() }.filter { it.isNotBlank() }
                
                val finalSentences = if (rawSentences.isEmpty()) listOf(pTrimmed) else rawSentences

                for (s in finalSentences) {
                    // If sentence exceeds safe speech input length, chunk it by commas or spaces
                    val chunks = chunkIfTooLong(s, MAX_TTS_CHUNK_LENGTH)
                    for (chunk in chunks) {
                        val sStart = rawText.indexOf(chunk, globalCharOffset).takeIf { it >= 0 } ?: globalCharOffset
                        val sEnd = sStart + chunk.length
                        globalCharOffset = sEnd

                        segments.add(
                            TextSegment(
                                id = "seg_${pIndex}_$globalSentenceIndex",
                                paragraphIndex = pIndex,
                                sentenceIndex = globalSentenceIndex,
                                text = chunk,
                                startCharOffset = sStart,
                                endCharOffset = sEnd
                            )
                        )
                        globalSentenceIndex++
                    }
                }
            }
        }

        return segments
    }

    private fun chunkIfTooLong(text: String, maxLength: Int): List<String> {
        if (text.length <= maxLength) return listOf(text)

        val result = mutableListOf<String>()
        var current = StringBuilder()
        val words = text.split(" ")

        for (w in words) {
            if (current.length + w.length + 1 > maxLength) {
                if (current.isNotEmpty()) {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                if (w.length > maxLength) {
                    // Hard chunk long word
                    var i = 0
                    while (i < w.length) {
                        val end = minOf(i + maxLength, w.length)
                        result.add(w.substring(i, end))
                        i = end
                    }
                    continue
                }
            }
            if (current.isNotEmpty()) current.append(" ")
            current.append(w)
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }
}

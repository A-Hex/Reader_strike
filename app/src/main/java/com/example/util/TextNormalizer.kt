package com.example.util

import java.text.Normalizer
import java.util.Locale

object TextNormalizer {

    /**
     * Normalizes text for search indexing and queries:
     * - Trims and lowercases.
     * - Removes Arabic tashkeel (diacritics: fatha, damma, kasra, sukun, shadda, tanween).
     * - Normalizes Arabic letter variants:
     *     أ, إ, آ, ٱ -> ا
     *     ة -> ه
     *     ى -> ي
     *     ؤ -> و
     *     ئ -> ي
     *     ـ (tatweel/kashida) removed
     * - Strips combining diacritical marks from Latin characters (e.g. é -> e, ü -> u, ñ -> n).
     * - Removes redundant non-alphanumeric punctuation except single spaces.
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""

        var str = input.trim().lowercase(Locale.ROOT)

        // 1. Remove Arabic Tashkeel & Tatweel
        str = str.replace(Regex("[\u064B-\u065F\u0670\u0640]"), "")

        // 2. Normalize Arabic Alef variants
        str = str.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')

        // 3. Normalize Taa Marbuta and Alef Maksura
        str = str.replace('ة', 'ه')
            .replace('ى', 'ي')

        // 4. Normalize Hamza on carriers
        str = str.replace('ؤ', 'و')
            .replace('ئ', 'ي')

        // 5. Decompose Latin accents (e.g., é -> e, à -> a, ö -> o)
        val decomposed = Normalizer.normalize(str, Normalizer.Form.NFD)
        str = decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        // 6. Normalize punctuation and multiple spaces to single space
        str = str.replace(Regex("[\\p{Punct}&&[^_-]]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return str
    }

    /**
     * Checks whether the target text contains the query using normalized fuzzy matching.
     */
    fun matches(target: String?, query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        if (target.isNullOrBlank()) return false

        val normTarget = normalize(target)
        val normQuery = normalize(query)

        if (normQuery.isEmpty()) return true
        if (normTarget.contains(normQuery)) return true

        // Check if all tokens in query exist in target
        val tokens = normQuery.split(" ").filter { it.isNotBlank() }
        if (tokens.size > 1) {
            return tokens.all { normTarget.contains(it) }
        }

        return false
    }
}

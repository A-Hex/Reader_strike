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

        val tokens = normQuery.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true

        // 1. All tokens exist in target
        if (tokens.all { normTarget.contains(it) }) return true

        // 2. Multi-word query with >= 60% token overlap
        if (tokens.size >= 2) {
            val matchedCount = tokens.count { normTarget.contains(it) }
            if (matchedCount.toFloat() / tokens.size >= 0.6f) return true
        }

        // 3. For single words with length >= 4, check prefix / fuzzy edit distance <= 1
        if (tokens.size == 1) {
            val q = tokens.first()
            val targetWords = normTarget.split(" ").filter { it.isNotBlank() }
            for (w in targetWords) {
                if (w.startsWith(q) || q.startsWith(w)) return true
                if (q.length >= 4 && w.length >= 4 && kotlin.math.abs(q.length - w.length) <= 1) {
                    if (isOneEditDistance(q, w)) return true
                }
            }
        }

        return false
    }

    /**
     * Matches query against multiple fields combined into a single unified search document.
     */
    fun matchesAny(query: String?, vararg targets: String?): Boolean {
        if (query.isNullOrBlank()) return true
        val combined = targets.filterNotNull().joinToString(" ")
        return matches(combined, query)
    }

    private fun isOneEditDistance(s1: String, s2: String): Boolean {
        val len1 = s1.length
        val len2 = s2.length
        if (kotlin.math.abs(len1 - len2) > 1) return false

        var i = 0
        var j = 0
        var diffCount = 0

        while (i < len1 && j < len2) {
            if (s1[i] != s2[j]) {
                diffCount++
                if (diffCount > 1) return false
                if (len1 > len2) i++
                else if (len2 > len1) j++
                else { i++; j++ }
            } else {
                i++
                j++
            }
        }
        if (i < len1 || j < len2) diffCount++
        return diffCount <= 1
    }
}

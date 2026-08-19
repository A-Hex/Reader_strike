package com.example.recommendation

import com.example.model.Book
import com.example.model.BookRecommendation
import com.example.model.Highlight
import com.example.model.ReadingStatus

object RecommendationEngine {

    /**
     * Analyzes reading history, genre preferences, and highlighted content
     * to suggest relevant new e-books (PDF, EPUB) for download and reading.
     */
    fun generateRecommendations(
        userLibrary: List<Book>,
        userHighlights: List<Highlight>,
        catalog: List<Book>
    ): List<BookRecommendation> {
        val ownedBookIds = userLibrary.map { it.id }.toSet()
        val candidateBooks = catalog.filter { it.id !in ownedBookIds }

        if (candidateBooks.isEmpty()) return emptyList()

        // 1. Analyze Reading History & Genre Preferences
        val genreFrequency = mutableMapOf<String, Int>()
        val authorFrequency = mutableMapOf<String, Int>()
        val tagFrequency = mutableMapOf<String, Int>()

        userLibrary.forEach { book ->
            val weight = when {
                book.status == ReadingStatus.FINISHED -> 3
                book.readingProgress > 0.5f -> 2
                book.isFavorite -> 2
                else -> 1
            }
            genreFrequency[book.genre] = (genreFrequency[book.genre] ?: 0) + weight
            authorFrequency[book.author] = (authorFrequency[book.author] ?: 0) + weight
            book.tags.forEach { tag ->
                tagFrequency[tag] = (tagFrequency[tag] ?: 0) + weight
            }
        }

        // 2. Analyze Highlight Content & Keywords
        val highlightKeywords = mutableSetOf<String>()
        val highlightThematicKeywords = listOf(
            "stoic", "stoicism", "control", "mind", "nature", "virtue", "wisdom", "soul", "resilience",
            "war", "strategy", "enemy", "victory", "tactics", "leadership", "discipline",
            "mystery", "detective", "crime", "deduction", "clue", "logic",
            "gothic", "horror", "monster", "darkness", "fear", "creature",
            "justice", "truth", "morality", "ethics", "philosophy", "freedom", "society"
        )

        userHighlights.forEach { hl ->
            val content = (hl.text + " " + (hl.note ?: "")).lowercase()
            highlightThematicKeywords.forEach { kw ->
                if (content.contains(kw)) {
                    highlightKeywords.add(kw)
                }
            }
        }

        // 3. Score candidates
        val recommendations = candidateBooks.map { candidate ->
            var score = 65 // base compatibility score
            val reasons = mutableListOf<String>()
            val matchedThemes = mutableListOf<String>()

            // Match genre
            val genreMatches = genreFrequency.keys.filter {
                candidate.genre.contains(it, ignoreCase = true) || it.contains(candidate.genre, ignoreCase = true) ||
                (it.contains("Philosophy", true) && candidate.genre.contains("Philosophy", true)) ||
                (it.contains("Gothic", true) && candidate.genre.contains("Gothic", true))
            }
            if (genreMatches.isNotEmpty()) {
                score += 15
                reasons.add("Matches your interest in ${candidate.genre}")
            }

            // Match tags
            val tagOverlap = candidate.tags.filter { tag ->
                tagFrequency.containsKey(tag) || tagFrequency.keys.any { it.contains(tag, true) }
            }
            if (tagOverlap.isNotEmpty()) {
                score += (tagOverlap.size * 5).coerceAtMost(15)
            }

            // Match highlighted keywords
            val matchedHighlightKeywords = highlightKeywords.filter { kw ->
                candidate.description.lowercase().contains(kw) ||
                candidate.tags.any { it.lowercase().contains(kw) } ||
                candidate.title.lowercase().contains(kw) ||
                candidate.genre.lowercase().contains(kw)
            }
            if (matchedHighlightKeywords.isNotEmpty()) {
                score += 12
                matchedThemes.addAll(matchedHighlightKeywords)
                reasons.add("Aligns with themes in your notes: ${matchedHighlightKeywords.take(2).joinToString(", ")}")
            }

            // Specific book association logic
            if (candidate.id == "cat-letters-stoic" && userLibrary.any { it.id == "book-meditations" }) {
                score += 10
                reasons.add("Perfect companion to Marcus Aurelius's Meditations")
            } else if (candidate.id == "cat-republic" && userLibrary.any { it.genre.contains("Philosophy", true) }) {
                score += 8
                reasons.add("Foundational classical dialogue expanding on virtue and justice")
            } else if (candidate.id == "cat-dracula" && userLibrary.any { it.id == "book-frankenstein" || it.id == "book-sherlock-holmes" }) {
                score += 10
                reasons.add("Top classic gothic literature recommendation")
            } else if (candidate.id == "cat-dorian-gray" && userLibrary.any { it.id == "book-metamorphosis" }) {
                score += 9
                reasons.add("Psychological masterwork exploring transformation and identity")
            }

            val finalScore = score.coerceIn(78, 99)
            val primaryReason = if (reasons.isNotEmpty()) {
                reasons.first()
            } else {
                "Recommended based on your ${candidate.genre} reading habits"
            }

            BookRecommendation(
                book = candidate,
                matchScorePercent = finalScore,
                matchReason = primaryReason,
                matchedGenre = candidate.genre,
                relatedHighlights = matchedThemes
            )
        }.sortedByDescending { it.matchScorePercent }

        return recommendations
    }
}

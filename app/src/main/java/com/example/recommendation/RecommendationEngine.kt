package com.example.recommendation

import com.example.model.Book
import com.example.model.BookRecommendation
import com.example.model.Highlight
import com.example.model.ReadingStatus

/**
 * Suggests catalogue titles from what the reader has actually done in the app.
 *
 * Honesty rules this engine follows, because a "match percentage" the reader cannot influence is
 * worse than no percentage at all:
 *  - Only books the reader has genuinely engaged with count as a signal. A freshly installed shelf
 *    of unread bundled classics is a catalogue, not a reading history.
 *  - Every point is awarded for a concrete, checkable overlap (genre, author, tag, highlighted
 *    theme). There is no base score and no floor, so a weak suggestion really reads as weak.
 *  - [BookRecommendation.matchScorePercent] is 0 when no signal exists at all, and the UI shows
 *    "Suggested" instead of inventing a number.
 */
object RecommendationEngine {

    // Points are only ever awarded for a real overlap. The maximum is 100, so the score is a
    // genuine ratio of "how much of this the reader has evidence for".
    private const val POINTS_GENRE = 30
    private const val POINTS_AUTHOR = 20
    private const val POINTS_TAGS = 25
    private const val POINTS_HIGHLIGHT_THEME = 25

    private val HIGHLIGHT_THEMES = listOf(
        "stoic", "stoicism", "control", "mind", "nature", "virtue", "wisdom", "soul", "resilience",
        "war", "strategy", "enemy", "victory", "tactics", "leadership", "discipline",
        "mystery", "detective", "crime", "deduction", "clue", "logic",
        "gothic", "horror", "monster", "darkness", "fear", "creature",
        "justice", "truth", "morality", "ethics", "philosophy", "freedom", "society"
    )

    fun generateRecommendations(
        userLibrary: List<Book>,
        userHighlights: List<Highlight>,
        catalog: List<Book>
    ): List<BookRecommendation> {
        val ownedBookIds = userLibrary.map { it.id }.toSet()
        val candidateBooks = catalog.filter { it.id !in ownedBookIds }
        if (candidateBooks.isEmpty()) return emptyList()

        // 1. Signal: only books the reader has actually opened, finished or favourited.
        val engagedBooks = userLibrary.filter { book ->
            book.status == ReadingStatus.FINISHED ||
                book.readingProgress > 0f ||
                book.isFavorite
        }

        val genreFrequency = mutableMapOf<String, Int>()
        val authorFrequency = mutableMapOf<String, Int>()
        val tagFrequency = mutableMapOf<String, Int>()

        engagedBooks.forEach { book ->
            val weight = when {
                book.status == ReadingStatus.FINISHED -> 3
                book.readingProgress > 0.5f -> 2
                else -> 1
            }
            if (book.genre.isNotBlank()) genreFrequency[book.genre] = (genreFrequency[book.genre] ?: 0) + weight
            if (book.author.isNotBlank()) authorFrequency[book.author] = (authorFrequency[book.author] ?: 0) + weight
            book.tags.forEach { tag -> tagFrequency[tag] = (tagFrequency[tag] ?: 0) + weight }
        }

        // 2. Signal: themes the reader actually highlighted or annotated.
        val highlightThemes = mutableSetOf<String>()
        userHighlights.forEach { highlight ->
            val content = (highlight.text + " " + (highlight.note ?: "")).lowercase()
            HIGHLIGHT_THEMES.forEach { theme ->
                if (content.contains(theme)) highlightThemes.add(theme)
            }
        }

        val hasPersonalSignal = engagedBooks.isNotEmpty() || highlightThemes.isNotEmpty()

        // 3. Score each candidate strictly from those signals.
        val recommendations = candidateBooks.map { candidate ->
            var score = 0
            val reasons = mutableListOf<String>()
            val matchedThemes = mutableListOf<String>()

            val matchingGenre = genreFrequency.keys.firstOrNull { genre ->
                candidate.genre.contains(genre, ignoreCase = true) ||
                    genre.contains(candidate.genre, ignoreCase = true)
            }
            if (matchingGenre != null) {
                score += POINTS_GENRE
                reasons.add("You read $matchingGenre")
            }

            if (authorFrequency.containsKey(candidate.author)) {
                score += POINTS_AUTHOR
                reasons.add("More from ${candidate.author}")
            }

            val matchingTags = candidate.tags.filter { tag ->
                tagFrequency.containsKey(tag) || tagFrequency.keys.any { it.contains(tag, ignoreCase = true) }
            }
            if (matchingTags.isNotEmpty()) {
                score += (matchingTags.size * (POINTS_TAGS / 5)).coerceAtMost(POINTS_TAGS)
                reasons.add("Shares ${matchingTags.take(2).joinToString(", ")} with your books")
            }

            val matchingHighlightThemes = highlightThemes.filter { theme ->
                candidate.description.lowercase().contains(theme) ||
                    candidate.title.lowercase().contains(theme) ||
                    candidate.genre.lowercase().contains(theme) ||
                    candidate.tags.any { it.lowercase().contains(theme) }
            }
            if (matchingHighlightThemes.isNotEmpty()) {
                score += POINTS_HIGHLIGHT_THEME
                matchedThemes.addAll(matchingHighlightThemes)
                reasons.add("Matches themes you highlighted: ${matchingHighlightThemes.take(2).joinToString(", ")}")
            }

            BookRecommendation(
                book = candidate,
                // 0 = no evidence yet; the UI shows "Suggested" rather than a made-up number.
                matchScorePercent = score.coerceIn(0, 99),
                matchReason = when {
                    reasons.isNotEmpty() -> reasons.joinToString(" · ")
                    hasPersonalSignal -> "Related to your library, but no direct overlap found yet"
                    else -> "A public-domain classic from the SecureMind catalogue"
                },
                matchedGenre = candidate.genre,
                relatedHighlights = matchedThemes
            )
        }.sortedWith(
            compareByDescending<BookRecommendation> { it.matchScorePercent }
                .thenBy { it.book.title }
        )

        return recommendations
    }
}

package com.example.model

data class BookRecommendation(
    val book: Book,
    /**
     * Share of the signals this suggestion actually matches, 0-99. **0 means no signal exists
     * yet** — the UI shows "Suggested" instead of inventing a percentage.
     */
    val matchScorePercent: Int,
    val matchReason: String,
    val matchedGenre: String,
    val relatedHighlights: List<String> = emptyList()
)

package com.example.model

data class BookRecommendation(
    val book: Book,
    val matchScorePercent: Int, // e.g. 96
    val matchReason: String,
    val matchedGenre: String,
    val relatedHighlights: List<String> = emptyList()
)

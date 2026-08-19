package com.example.model

data class BookReview(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val userName: String,
    val userAvatarColor: Long = 0xFF5A8E72,
    val rating: Float, // 1.0 to 5.0
    val reviewTitle: String,
    val reviewText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isUserReview: Boolean = false,
    val helpfulCount: Int = 0
)

data class BookReviewSummary(
    val bookId: String,
    val averageRating: Float,
    val totalReviews: Int,
    val ratingBreakdown: Map<Int, Int> = emptyMap() // 5 -> count, 4 -> count, etc.
)

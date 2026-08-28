package com.example.model

data class InBookSearchMatch(
    val chapterIndex: Int,
    val chapterTitle: String,
    val paragraphIndex: Int,
    val snippet: String,
    val fullParagraph: String,
    val matchQuery: String
)

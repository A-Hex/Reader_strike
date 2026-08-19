package com.example.model

import androidx.compose.ui.graphics.Color

enum class HighlightColor(val hex: String, val displayName: String, val colorValue: Long) {
    AMBER("#FBBF24", "Warm Amber", 0xFFFBBF24),
    EMERALD("#34D399", "Mint Emerald", 0xFF34D399),
    CORAL("#F87171", "Sunset Coral", 0xFFF87171),
    SKY("#38BDF8", "Sky Cyan", 0xFF38BDF8),
    VIOLET("#C084FC", "Neon Violet", 0xFFC084FC);

    fun toComposeColor(): Color = Color(colorValue)

    companion object {
        fun fromHex(hex: String): HighlightColor {
            return entries.find { it.hex.equals(hex, ignoreCase = true) } ?: AMBER
        }
    }
}

data class Highlight(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val chapterIndex: Int = 0,
    val chapterTitle: String = "Chapter 1",
    val text: String,
    val note: String? = null,
    val color: HighlightColor = HighlightColor.AMBER,
    val pageOrLocation: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

data class Bookmark(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val chapterIndex: Int = 0,
    val chapterTitle: String = "",
    val page: Int = 1,
    val title: String,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

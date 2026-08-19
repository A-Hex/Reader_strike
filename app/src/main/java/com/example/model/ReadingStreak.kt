package com.example.model

enum class BadgeTier(val displayName: String, val colorLong: Long) {
    BRONZE("Bronze Hex", 0xFFCD7F32),
    SILVER("Silver Hex", 0xFFC0C0C0),
    GOLD("Gold Hex", 0xFFFFD700),
    DIAMOND("A-Hex Diamond", 0xFF00E5FF)
}

data class StreakBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val tier: BadgeTier,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Float = 0f // 0.0 to 1.0
)

data class DayReadingStat(
    val date: String, // YYYY-MM-DD
    val dayOfWeek: String, // "Mon", "Tue"
    val minutesRead: Int,
    val pagesRead: Int,
    val isGoalReached: Boolean
)

data class ReadingStreakData(
    val currentStreakDays: Int = 5,
    val longestStreakDays: Int = 14,
    val totalMinutesRead: Int = 1240,
    val totalPagesRead: Int = 412,
    val totalBooksRead: Int = 2,
    val totalBooksInLibrary: Int = 6,
    val avgSessionMinutes: Float = 24.5f,
    val totalSessionsCount: Int = 28,
    val dailyGoalMinutes: Int = 20,
    val dailyGoalPages: Int = 25,
    val todayMinutesRead: Int = 22,
    val todayPagesRead: Int = 28,
    val readingSpeedWpm: Int = 240,
    val lastReadDate: String = "",
    val weeklyStats: List<DayReadingStat> = emptyList(),
    val monthlyStats: List<DayReadingStat> = emptyList(),
    val badges: List<StreakBadge> = emptyList()
)

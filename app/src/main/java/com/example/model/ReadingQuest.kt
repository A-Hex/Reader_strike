package com.example.model

enum class ReaderRank(val title: String, val minXp: Int, val badgeIcon: String, val level: Int) {
    NOVICE("Novice Reader", 0, "🌱", 1),
    APPRENTICE("Page Apprentice", 250, "📜", 2),
    SCHOLAR("Curious Scholar", 750, "📖", 3),
    ADEPT("Literary Adept", 1800, "🔥", 4),
    SAGE("Page Sage", 3500, "🧠", 5),
    GRAND_BIBLIOPHILE("Grand Bibliophile", 6000, "👑", 6);

    companion object {
        fun getRankForXp(xp: Int): ReaderRank {
            return entries.lastOrNull { xp >= it.minXp } ?: NOVICE
        }

        fun getNextRank(currentRank: ReaderRank): ReaderRank? {
            val idx = entries.indexOf(currentRank)
            return if (idx < entries.size - 1) entries[idx + 1] else null
        }
    }
}

data class ReadingQuest(
    val id: String,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val xpReward: Int,
    val shieldReward: Int = 0,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val iconEmoji: String = "🎯"
)

data class GamificationState(
    val totalXp: Int = 340,
    val streakShields: Int = 1,
    val maxShields: Int = 3,
    val currentRank: ReaderRank = ReaderRank.APPRENTICE,
    val activeQuests: List<ReadingQuest> = emptyList()
)

package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GamificationState
import com.example.model.ReaderRank
import com.example.model.ReadingQuest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class QuestsAndShieldsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(GamificationState())
    val state: StateFlow<GamificationState> = _state.asStateFlow()

    init {
        loadState()
    }

    private fun loadState() {
        val totalXp = prefs.getInt("total_xp", 0)
        val shields = prefs.getInt("streak_shields", 0)
        val rank = ReaderRank.getRankForXp(totalXp)

        val questsJson = prefs.getString("quests_json", null)
        val quests = if (questsJson != null) {
            parseQuests(questsJson)
        } else {
            getDefaultQuests()
        }

        _state.value = GamificationState(
            totalXp = totalXp,
            streakShields = shields.coerceIn(0, 3),
            currentRank = rank,
            activeQuests = quests
        )
    }

    private fun getDefaultQuests(): List<ReadingQuest> {
        return listOf(
            ReadingQuest(
                id = "quest-focus-sprint",
                title = "20-Minute Focus Sprint",
                description = "Complete 20 uninterrupted minutes of reading today",
                currentProgress = 0,
                targetProgress = 20,
                xpReward = 80,
                shieldReward = 0,
                isCompleted = false,
                isClaimed = false,
                iconEmoji = "⏱️"
            ),
            ReadingQuest(
                id = "quest-vocab-vault",
                title = "Lexicon Builder",
                description = "Inspect and master 3 new vocabulary terms",
                currentProgress = 0,
                targetProgress = 3,
                xpReward = 60,
                shieldReward = 0,
                isCompleted = false,
                isClaimed = false,
                iconEmoji = "🔤"
            ),
            ReadingQuest(
                id = "quest-streak-guardian",
                title = "Streak Champion",
                description = "Maintain a 5-day continuous reading streak",
                currentProgress = 0,
                targetProgress = 5,
                xpReward = 150,
                shieldReward = 1,
                isCompleted = false,
                isClaimed = false,
                iconEmoji = "🛡️"
            ),
            ReadingQuest(
                id = "quest-speed-reader",
                title = "Velocity Master",
                description = "Read 500 words using RSVP Speed Reader",
                currentProgress = 0,
                targetProgress = 500,
                xpReward = 100,
                shieldReward = 0,
                isCompleted = false,
                isClaimed = false,
                iconEmoji = "⚡"
            )
        )
    }

    private fun parseQuests(json: String): List<ReadingQuest> {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<ReadingQuest>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ReadingQuest(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        currentProgress = obj.getInt("currentProgress"),
                        targetProgress = obj.getInt("targetProgress"),
                        xpReward = obj.getInt("xpReward"),
                        shieldReward = obj.optInt("shieldReward", 0),
                        isCompleted = obj.getBoolean("isCompleted"),
                        isClaimed = obj.getBoolean("isClaimed"),
                        iconEmoji = obj.optString("iconEmoji", "🎯")
                    )
                )
            }
            if (list.isEmpty()) getDefaultQuests() else list
        } catch (e: Exception) {
            getDefaultQuests()
        }
    }

    private fun persist() {
        try {
            val curr = _state.value
            prefs.edit()
                .putInt("total_xp", curr.totalXp)
                .putInt("streak_shields", curr.streakShields)
                .apply()

            val arr = JSONArray()
            for (q in curr.activeQuests) {
                val obj = JSONObject().apply {
                    put("id", q.id)
                    put("title", q.title)
                    put("description", q.description)
                    put("currentProgress", q.currentProgress)
                    put("targetProgress", q.targetProgress)
                    put("xpReward", q.xpReward)
                    put("shieldReward", q.shieldReward)
                    put("isCompleted", q.isCompleted)
                    put("isClaimed", q.isClaimed)
                    put("iconEmoji", q.iconEmoji)
                }
                arr.put(obj)
            }
            prefs.edit().putString("quests_json", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addXp(amount: Int) {
        val newXp = _state.value.totalXp + amount
        val newRank = ReaderRank.getRankForXp(newXp)
        _state.value = _state.value.copy(
            totalXp = newXp,
            currentRank = newRank
        )
        persist()
    }

    fun useStreakShield(): Boolean {
        if (_state.value.streakShields > 0) {
            _state.value = _state.value.copy(streakShields = _state.value.streakShields - 1)
            persist()
            return true
        }
        return false
    }

    fun addStreakShield(): Boolean {
        if (_state.value.streakShields < _state.value.maxShields) {
            _state.value = _state.value.copy(streakShields = _state.value.streakShields + 1)
            persist()
            return true
        }
        return false
    }

    fun incrementQuestProgress(questId: String, amount: Int = 1) {
        val updated = _state.value.activeQuests.map { q ->
            if (q.id == questId && !q.isCompleted) {
                val newProg = (q.currentProgress + amount).coerceAtMost(q.targetProgress)
                q.copy(
                    currentProgress = newProg,
                    isCompleted = newProg >= q.targetProgress
                )
            } else q
        }
        _state.value = _state.value.copy(activeQuests = updated)
        persist()
    }

    fun claimQuestReward(questId: String) {
        val quest = _state.value.activeQuests.find { it.id == questId && it.isCompleted && !it.isClaimed } ?: return
        val updatedQuests = _state.value.activeQuests.map {
            if (it.id == questId) it.copy(isClaimed = true) else it
        }

        val newXp = _state.value.totalXp + quest.xpReward
        val newShields = (_state.value.streakShields + quest.shieldReward).coerceAtMost(_state.value.maxShields)
        val newRank = ReaderRank.getRankForXp(newXp)

        _state.value = _state.value.copy(
            totalXp = newXp,
            streakShields = newShields,
            currentRank = newRank,
            activeQuests = updatedQuests
        )
        persist()
    }

    fun recordWordLookup() {
        incrementQuestProgress("quest-vocab-vault", 1)
        addXp(15)
    }

    fun recordMinutesRead(minutes: Int) {
        incrementQuestProgress("quest-focus-sprint", minutes)
        addXp(minutes * 5)
    }

    fun recordSpeedRead() {
        incrementQuestProgress("quest-speed-reader", 1)
        addXp(25)
    }

    fun updateStreak(streak: Int) {
        val updated = _state.value.activeQuests.map { q ->
            if (q.id == "quest-streak-guardian" && !q.isClaimed) {
                val newProg = streak.coerceAtMost(q.targetProgress)
                q.copy(
                    currentProgress = newProg,
                    isCompleted = newProg >= q.targetProgress
                )
            } else q
        }
        _state.value = _state.value.copy(activeQuests = updated)
        persist()
    }
}

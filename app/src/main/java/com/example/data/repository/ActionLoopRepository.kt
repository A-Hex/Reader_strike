package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ActionLoop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local storage for the "Read -> Build" habit loop.
 *
 * This is deliberately honest, small and offline-first: loops live in this app's private
 * preferences as JSON, exactly like the quest/shield state. Nothing is uploaded anywhere and
 * no AI writes the reader's answers for them.
 */
class ActionLoopRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("action_loop_prefs", Context.MODE_PRIVATE)

    private val _loops = MutableStateFlow<List<ActionLoop>>(emptyList())
    val loops: StateFlow<List<ActionLoop>> = _loops.asStateFlow()

    init {
        _loops.value = load()
    }

    /** Number of loops the reader has actually finished (marked built). */
    val builtCount: Int
        get() = _loops.value.count { it.isBuilt }

    fun find(id: String): ActionLoop? = _loops.value.firstOrNull { it.id == id }

    /** Inserts a new loop or replaces an existing one with the same id. */
    fun save(loop: ActionLoop): ActionLoop {
        val now = System.currentTimeMillis()
        val normalised = if (loop.id.isBlank() || find(loop.id) == null) {
            loop.copy(
                id = loop.id.ifBlank { "loop-${UUID.randomUUID().toString().take(8)}" },
                createdAt = if (loop.createdAt == 0L) now else loop.createdAt,
                updatedAt = now
            )
        } else {
            loop.copy(updatedAt = now)
        }

        _loops.value = _loops.value
            .filterNot { it.id == normalised.id }
            .plus(normalised)
            .sortedByDescending { it.updatedAt }

        persist()
        return normalised
    }

    fun delete(id: String) {
        _loops.value = _loops.value.filterNot { it.id == id }
        persist()
    }

    /** Marks a loop built. Returns true only the first time, so callers can award a reward once. */
    fun markBuilt(id: String): Boolean {
        val current = find(id) ?: return false
        if (current.isBuilt) return false
        save(current.copy(isBuilt = true))
        return true
    }

    fun clearAll() {
        _loops.value = emptyList()
        persist()
    }

    // ------------------------------------------------------------------------------------
    // Serialisation
    // ------------------------------------------------------------------------------------

    private fun load(): List<ActionLoop> {
        val raw = prefs.getString(KEY_LOOPS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    if (id.isBlank()) continue
                    add(
                        ActionLoop(
                            id = id,
                            bookTitle = obj.optString("bookTitle"),
                            readNote = obj.optString("readNote"),
                            problem = obj.optString("problem"),
                            bookIdea = obj.optString("bookIdea"),
                            action = obj.optString("action"),
                            isBuilt = obj.optBoolean("isBuilt", false),
                            createdAt = obj.optLong("createdAt", 0L),
                            updatedAt = obj.optLong("updatedAt", 0L)
                        )
                    )
                }
            }.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            // Corrupt payload: start clean rather than crashing the Settings screen.
            prefs.edit().remove(KEY_LOOPS).apply()
            emptyList()
        }
    }

    private fun persist() {
        try {
            val array = JSONArray()
            for (loop in _loops.value) {
                array.put(
                    JSONObject()
                        .put("id", loop.id)
                        .put("bookTitle", loop.bookTitle)
                        .put("readNote", loop.readNote)
                        .put("problem", loop.problem)
                        .put("bookIdea", loop.bookIdea)
                        .put("action", loop.action)
                        .put("isBuilt", loop.isBuilt)
                        .put("createdAt", loop.createdAt)
                        .put("updatedAt", loop.updatedAt)
                )
            }
            prefs.edit().putString(KEY_LOOPS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private companion object {
        const val KEY_LOOPS = "action_loops_json"
    }
}

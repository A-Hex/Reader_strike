package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.entity.BookEntity
import com.example.data.entity.HighlightEntity
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CloudSyncRepository(private val database: AppDatabase) {

    private val _syncInfo = MutableStateFlow(CloudSyncInfo())
    val syncInfo: StateFlow<CloudSyncInfo> = _syncInfo.asStateFlow()

    suspend fun performCloudSync(): Boolean = withContext(Dispatchers.IO) {
        _syncInfo.value = _syncInfo.value.copy(syncState = SyncState.SYNCING)
        try {
            delay(1600) // Simulate cloud server network handshake & encryption

            val allBooks = database.bookDao().getAllBooks().first()
            val allHighlights = database.highlightDao().getAllHighlights().first()

            val newLog = SyncLogItem(
                id = "log-" + UUID.randomUUID().toString().take(6),
                timestamp = System.currentTimeMillis(),
                message = "Synced ${allBooks.size} books, ${allHighlights.size} highlights with A-Hex Cloud",
                isSuccess = true,
                itemsSynced = allBooks.size + allHighlights.size
            )

            val updatedLogs = listOf(newLog) + _syncInfo.value.syncLogs.take(9)

            _syncInfo.value = _syncInfo.value.copy(
                syncState = SyncState.SUCCESS,
                lastSyncedAt = System.currentTimeMillis(),
                pendingChangesCount = 0,
                syncLogs = updatedLogs
            )

            delay(2000)
            _syncInfo.value = _syncInfo.value.copy(syncState = SyncState.IDLE)
            return@withContext true
        } catch (e: Exception) {
            _syncInfo.value = _syncInfo.value.copy(syncState = SyncState.ERROR)
            return@withContext false
        }
    }

    suspend fun exportFullLibraryJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val booksArray = JSONArray()
        val highlightsArray = JSONArray()

        val books = database.bookDao().getAllBooks().first()
        books.forEach { b ->
            val obj = JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("author", b.author)
                put("format", b.format)
                put("status", b.status)
                put("totalPages", b.totalPages)
                put("currentPage", b.currentPage)
                put("readingProgress", b.readingProgress.toDouble())
                put("isFavorite", b.isFavorite)
                put("genre", b.genre)
            }
            booksArray.put(obj)
        }

        val highlights = database.highlightDao().getAllHighlights().first()
        highlights.forEach { h ->
            val obj = JSONObject().apply {
                put("id", h.id)
                put("bookId", h.bookId)
                put("bookTitle", h.bookTitle)
                put("text", h.text)
                put("note", h.note ?: "")
                put("colorHex", h.colorHex)
                put("chapterTitle", h.chapterTitle)
            }
            highlightsArray.put(obj)
        }

        root.put("version", "1.0")
        root.put("appName", "A-Hex streak")
        root.put("creator", "@ahex0_01")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("books", booksArray)
        root.put("highlights", highlightsArray)

        return@withContext root.toString(2)
    }

    suspend fun restoreLibraryFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val booksArray = root.optJSONArray("books")
            val highlightsArray = root.optJSONArray("highlights")

            if (booksArray != null) {
                for (i in 0 until booksArray.length()) {
                    val obj = booksArray.getJSONObject(i)
                    val book = Book(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        author = obj.getString("author"),
                        description = "Restored from cloud backup",
                        format = try { BookFormat.valueOf(obj.getString("format")) } catch (_: Exception) { BookFormat.EPUB },
                        status = try { ReadingStatus.valueOf(obj.getString("status")) } catch (_: Exception) { ReadingStatus.READING },
                        totalPages = obj.optInt("totalPages", 100),
                        currentPage = obj.optInt("currentPage", 1),
                        readingProgress = obj.optDouble("readingProgress", 0.0).toFloat(),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        genre = obj.optString("genre", "Classic")
                    )
                    database.bookDao().insertBook(BookEntity.fromModel(book))
                }
            }

            if (highlightsArray != null) {
                for (i in 0 until highlightsArray.length()) {
                    val obj = highlightsArray.getJSONObject(i)
                    val hl = Highlight(
                        id = obj.getString("id"),
                        bookId = obj.getString("bookId"),
                        bookTitle = obj.getString("bookTitle"),
                        text = obj.getString("text"),
                        note = obj.optString("note").ifBlank { null },
                        color = HighlightColor.fromHex(obj.optString("colorHex", "#FBBF24")),
                        chapterTitle = obj.optString("chapterTitle", "Chapter")
                    )
                    database.highlightDao().insertHighlight(HighlightEntity.fromModel(hl))
                }
            }

            performCloudSync()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        _syncInfo.value = _syncInfo.value.copy(autoSyncEnabled = enabled)
    }

    fun toggleWifiOnly(enabled: Boolean) {
        _syncInfo.value = _syncInfo.value.copy(syncOnWifiOnly = enabled)
    }
}

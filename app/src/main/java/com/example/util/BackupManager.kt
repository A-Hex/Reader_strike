package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.data.entity.BookEntity
import com.example.data.entity.BookReviewEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.HighlightEntity
import com.example.data.entity.ReadingSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

sealed class BackupResult {
    data class Success(val booksCount: Int, val highlightsCount: Int, val bookmarksCount: Int, val sessionsCount: Int) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

object BackupManager {

    const val BACKUP_SCHEMA_VERSION = 1
    const val APP_IDENTIFIER = "A-Hex streak"

    suspend fun createBackupJson(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val books = database.bookDao().getAllBooksSnapshot()
        val highlights = database.highlightDao().getAllHighlightsSnapshot()
        val bookmarks = database.bookmarkDao().getAllBookmarksSnapshot()
        val sessions = database.readingSessionDao().getAllSessionsSnapshot()
        val reviews = database.bookReviewDao().getAllReviewsSnapshot()

        val root = JSONObject().apply {
            put("version", BACKUP_SCHEMA_VERSION)
            put("appName", APP_IDENTIFIER)
            put("exportedAt", System.currentTimeMillis())

            val booksArray = JSONArray()
            books.forEach { b ->
                booksArray.put(JSONObject().apply {
                    put("id", b.id)
                    put("title", b.title)
                    put("author", b.author)
                    put("description", b.description)
                    put("format", b.format)
                    put("status", b.status)
                    put("coverGradientStart", b.coverGradientStart)
                    put("coverGradientEnd", b.coverGradientEnd)
                    put("coverImageUrl", b.coverImageUrl ?: JSONObject.NULL)
                    put("totalPages", b.totalPages)
                    put("currentPage", b.currentPage)
                    put("readingProgress", b.readingProgress.toDouble())
                    put("isFavorite", b.isFavorite)
                    put("isDownloaded", b.isDownloaded)
                    put("localFilePath", b.localFilePath ?: JSONObject.NULL)
                    put("fileSize", b.fileSize)
                    put("genre", b.genre)
                    put("tagsRaw", b.tagsRaw)
                    put("rating", b.rating.toDouble())
                    put("lastReadTimestamp", b.lastReadTimestamp)
                    put("addedTimestamp", b.addedTimestamp)
                    put("totalMinutesSpent", b.totalMinutesSpent)
                    put("customShelvesRaw", b.customShelvesRaw)
                })
            }
            put("books", booksArray)

            val highlightsArray = JSONArray()
            highlights.forEach { h ->
                highlightsArray.put(JSONObject().apply {
                    put("id", h.id)
                    put("bookId", h.bookId)
                    put("bookTitle", h.bookTitle)
                    put("chapterIndex", h.chapterIndex)
                    put("chapterTitle", h.chapterTitle)
                    put("text", h.text)
                    put("note", h.note ?: JSONObject.NULL)
                    put("colorHex", h.colorHex)
                    put("pageOrLocation", h.pageOrLocation)
                    put("timestamp", h.timestamp)
                })
            }
            put("highlights", highlightsArray)

            val bookmarksArray = JSONArray()
            bookmarks.forEach { bm ->
                bookmarksArray.put(JSONObject().apply {
                    put("id", bm.id)
                    put("bookId", bm.bookId)
                    put("bookTitle", bm.bookTitle)
                    put("chapterIndex", bm.chapterIndex)
                    put("chapterTitle", bm.chapterTitle)
                    put("page", bm.page)
                    put("title", bm.title)
                    put("note", bm.note ?: JSONObject.NULL)
                    put("timestamp", bm.timestamp)
                })
            }
            put("bookmarks", bookmarksArray)

            val sessionsArray = JSONArray()
            sessions.forEach { s ->
                sessionsArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("bookId", s.bookId)
                    put("durationMinutes", s.durationMinutes)
                    put("pagesRead", s.pagesRead)
                    put("dateString", s.dateString)
                    put("timestamp", s.timestamp)
                })
            }
            put("readingSessions", sessionsArray)

            val reviewsArray = JSONArray()
            reviews.forEach { r ->
                reviewsArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("bookId", r.bookId)
                    put("bookTitle", r.bookTitle)
                    put("userName", r.userName)
                    put("userAvatarColor", r.userAvatarColor)
                    put("rating", r.rating.toDouble())
                    put("reviewTitle", r.reviewTitle)
                    put("reviewText", r.reviewText)
                    put("timestamp", r.timestamp)
                    put("isUserReview", r.isUserReview)
                    put("helpfulCount", r.helpfulCount)
                })
            }
            put("bookReviews", reviewsArray)
        }

        root.toString(2)
    }

    suspend fun exportBackupToUri(context: Context, uri: Uri, database: AppDatabase): BackupResult = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson(database)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            } ?: return@withContext BackupResult.Error("Unable to open destination file for writing.")

            val booksCount = database.bookDao().getAllBooksSnapshot().size
            val highlightsCount = database.highlightDao().getAllHighlightsSnapshot().size
            val bookmarksCount = database.bookmarkDao().getAllBookmarksSnapshot().size
            val sessionsCount = database.readingSessionDao().getAllSessionsSnapshot().size
            BackupResult.Success(booksCount, highlightsCount, bookmarksCount, sessionsCount)
        } catch (e: Exception) {
            BackupResult.Error("Export failed: ${e.localizedMessage ?: "Unknown I/O error"}")
        }
    }

    fun parseAndValidateBackup(jsonString: String): Result<ValidatedBackupPayload> {
        return try {
            if (jsonString.isBlank()) {
                return Result.failure(IllegalArgumentException("Backup file is empty."))
            }

            val root = JSONObject(jsonString)
            if (!root.has("version")) {
                return Result.failure(IllegalArgumentException("Invalid backup format: missing schema version."))
            }

            val version = root.optInt("version", -1)
            if (version < 1) {
                return Result.failure(IllegalArgumentException("Unsupported backup version: $version"))
            }

            val booksList = mutableListOf<BookEntity>()
            val highlightsList = mutableListOf<HighlightEntity>()
            val bookmarksList = mutableListOf<BookmarkEntity>()
            val sessionsList = mutableListOf<ReadingSessionEntity>()
            val reviewsList = mutableListOf<BookReviewEntity>()

            // Parse Books
            val booksArray = root.optJSONArray("books") ?: JSONArray()
            for (i in 0 until booksArray.length()) {
                val obj = booksArray.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val title = obj.optString("title")
                if (id.isBlank() || title.isBlank()) continue

                booksList.add(
                    BookEntity(
                        id = id,
                        title = title,
                        author = obj.optString("author", "Unknown Author"),
                        description = obj.optString("description", ""),
                        format = obj.optString("format", "EPUB"),
                        status = obj.optString("status", "WANT_TO_READ"),
                        coverGradientStart = obj.optLong("coverGradientStart", 0xFF8D6E63),
                        coverGradientEnd = obj.optLong("coverGradientEnd", 0xFF4E342E),
                        coverImageUrl = if (obj.isNull("coverImageUrl")) null else obj.optString("coverImageUrl"),
                        totalPages = obj.optInt("totalPages", 100),
                        currentPage = obj.optInt("currentPage", 1),
                        readingProgress = obj.optDouble("readingProgress", 0.0).toFloat().coerceIn(0f, 1f),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isDownloaded = obj.optBoolean("isDownloaded", true),
                        localFilePath = if (obj.isNull("localFilePath")) null else obj.optString("localFilePath"),
                        fileSize = obj.optString("fileSize", "1.2 MB"),
                        genre = obj.optString("genre", "General"),
                        tagsRaw = obj.optString("tagsRaw", ""),
                        rating = obj.optDouble("rating", 4.5).toFloat(),
                        lastReadTimestamp = obj.optLong("lastReadTimestamp", System.currentTimeMillis()),
                        addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis()),
                        totalMinutesSpent = obj.optInt("totalMinutesSpent", 0),
                        customShelvesRaw = obj.optString("customShelvesRaw", "")
                    )
                )
            }

            // Parse Highlights
            val highlightsArray = root.optJSONArray("highlights") ?: JSONArray()
            for (i in 0 until highlightsArray.length()) {
                val obj = highlightsArray.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val bookId = obj.optString("bookId")
                val text = obj.optString("text")
                if (id.isBlank() || bookId.isBlank() || text.isBlank()) continue

                highlightsList.add(
                    HighlightEntity(
                        id = id,
                        bookId = bookId,
                        bookTitle = obj.optString("bookTitle", "Book"),
                        chapterIndex = obj.optInt("chapterIndex", 0),
                        chapterTitle = obj.optString("chapterTitle", "Chapter 1"),
                        text = text,
                        note = if (obj.isNull("note")) null else obj.optString("note"),
                        colorHex = obj.optString("colorHex", "#FFE082"),
                        pageOrLocation = obj.optInt("pageOrLocation", 1),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            // Parse Bookmarks
            val bookmarksArray = root.optJSONArray("bookmarks") ?: JSONArray()
            for (i in 0 until bookmarksArray.length()) {
                val obj = bookmarksArray.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val bookId = obj.optString("bookId")
                if (id.isBlank() || bookId.isBlank()) continue

                bookmarksList.add(
                    BookmarkEntity(
                        id = id,
                        bookId = bookId,
                        bookTitle = obj.optString("bookTitle", "Book"),
                        chapterIndex = obj.optInt("chapterIndex", 0),
                        chapterTitle = obj.optString("chapterTitle", "Chapter 1"),
                        page = obj.optInt("page", 1),
                        title = obj.optString("title", "Bookmark"),
                        note = if (obj.isNull("note")) null else obj.optString("note"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            // Parse Reading Sessions
            val sessionsArray = root.optJSONArray("readingSessions") ?: JSONArray()
            for (i in 0 until sessionsArray.length()) {
                val obj = sessionsArray.optJSONObject(i) ?: continue
                val bookId = obj.optString("bookId")
                if (bookId.isBlank()) continue

                sessionsList.add(
                    ReadingSessionEntity(
                        id = obj.optLong("id", 0L),
                        bookId = bookId,
                        durationMinutes = obj.optInt("durationMinutes", 1).coerceAtLeast(1),
                        pagesRead = obj.optInt("pagesRead", 0),
                        dateString = obj.optString("dateString", "2026-08-18"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            // Parse Book Reviews
            val reviewsArray = root.optJSONArray("bookReviews") ?: JSONArray()
            for (i in 0 until reviewsArray.length()) {
                val obj = reviewsArray.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val bookId = obj.optString("bookId")
                if (id.isBlank() || bookId.isBlank()) continue

                reviewsList.add(
                    BookReviewEntity(
                        id = id,
                        bookId = bookId,
                        bookTitle = obj.optString("bookTitle", "Book"),
                        userName = obj.optString("userName", "Reader"),
                        userAvatarColor = obj.optLong("userAvatarColor", 0xFF6D4C41),
                        rating = obj.optDouble("rating", 5.0).toFloat(),
                        reviewTitle = obj.optString("reviewTitle", "Review"),
                        reviewText = obj.optString("reviewText", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isUserReview = obj.optBoolean("isUserReview", true),
                        helpfulCount = obj.optInt("helpfulCount", 0)
                    )
                )
            }

            Result.success(
                ValidatedBackupPayload(
                    version = version,
                    books = booksList,
                    highlights = highlightsList,
                    bookmarks = bookmarksList,
                    sessions = sessionsList,
                    reviews = reviewsList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromUri(context: Context, uri: Uri, database: AppDatabase): BackupResult = withContext(Dispatchers.IO) {
        try {
            val jsonContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
            } ?: return@withContext BackupResult.Error("Unable to open backup file for reading.")

            val validationResult = parseAndValidateBackup(jsonContent)
            if (validationResult.isFailure) {
                return@withContext BackupResult.Error(
                    "Invalid or corrupt backup: ${validationResult.exceptionOrNull()?.localizedMessage ?: "Parsing error"}"
                )
            }

            val payload = validationResult.getOrThrow()

            // Non-destructively merge/upsert restored data into Room database
            if (payload.books.isNotEmpty()) {
                database.bookDao().insertBooks(payload.books)
            }
            if (payload.highlights.isNotEmpty()) {
                database.highlightDao().insertHighlights(payload.highlights)
            }
            if (payload.bookmarks.isNotEmpty()) {
                database.bookmarkDao().insertBookmarks(payload.bookmarks)
            }
            if (payload.sessions.isNotEmpty()) {
                database.readingSessionDao().insertSessions(payload.sessions)
            }
            if (payload.reviews.isNotEmpty()) {
                database.bookReviewDao().insertReviews(payload.reviews)
            }

            BackupResult.Success(
                booksCount = payload.books.size,
                highlightsCount = payload.highlights.size,
                bookmarksCount = payload.bookmarks.size,
                sessionsCount = payload.sessions.size
            )
        } catch (e: Exception) {
            BackupResult.Error("Restore failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}

data class ValidatedBackupPayload(
    val version: Int,
    val books: List<BookEntity>,
    val highlights: List<HighlightEntity>,
    val bookmarks: List<BookmarkEntity>,
    val sessions: List<ReadingSessionEntity>,
    val reviews: List<BookReviewEntity>
)

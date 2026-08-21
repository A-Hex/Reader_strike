package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.data.SampleBooksData
import com.example.data.entity.BookEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.BookReviewEntity
import com.example.data.entity.HighlightEntity
import com.example.data.entity.ReadingSessionEntity
import com.example.model.*
import com.example.reader.EpubParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class BookRepository(private val context: Context, private val database: AppDatabase) {

    private val bookDao = database.bookDao()
    private val highlightDao = database.highlightDao()
    private val bookmarkDao = database.bookmarkDao()
    private val readingSessionDao = database.readingSessionDao()
    private val bookReviewDao = database.bookReviewDao()

    val allBooks: Flow<List<Book>> = bookDao.getAllBooks().map { entities ->
        entities.map { it.toModel() }
    }

    val allHighlights: Flow<List<Highlight>> = highlightDao.getAllHighlights().map { entities ->
        entities.map { it.toModel() }
    }

    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks().map { entities ->
        entities.map { it.toModel() }
    }

    val allReviews: Flow<List<BookReview>> = bookReviewDao.getAllReviews().map { entities ->
        entities.map { it.toModel() }
    }

    fun getReviewsForBook(bookId: String): Flow<List<BookReview>> {
        return bookReviewDao.getReviewsForBook(bookId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val streakPrefs = context.getSharedPreferences("reading_streak_prefs", Context.MODE_PRIVATE)
        if (!streakPrefs.getBoolean("has_cleared_legacy_mock_sessions_v2", false)) {
            readingSessionDao.clearAllSessions()
            streakPrefs.edit()
                .putBoolean("has_cleared_legacy_mock_sessions_v2", true)
                .putInt("current_streak_days", 0)
                .putInt("longest_streak_days", 0)
                .putInt("minutes_read_today", 0)
                .apply()
        }

        val existing = bookDao.getAllBooks().first()
        if (existing.isEmpty()) {
            val entities = SampleBooksData.INITIAL_BOOKS.map { BookEntity.fromModel(it) }
            bookDao.insertBooks(entities)

            SampleBooksData.INITIAL_HIGHLIGHTS.forEach { hl ->
                highlightDao.insertHighlight(HighlightEntity.fromModel(hl))
            }

            // Seed initial community reviews for classic literature
            SampleBooksData.INITIAL_REVIEWS.forEach { rev ->
                bookReviewDao.insertReview(BookReviewEntity.fromModel(rev))
            }
        }
    }

    suspend fun addReview(
        bookId: String,
        bookTitle: String,
        rating: Float,
        reviewTitle: String,
        reviewText: String,
        userName: String
    ) = withContext(Dispatchers.IO) {
        val review = BookReview(
            id = "rev-" + UUID.randomUUID().toString().take(8),
            bookId = bookId,
            bookTitle = bookTitle,
            userName = userName.ifBlank { "A-Hex Reader" },
            userAvatarColor = 0xFF5A8E72,
            rating = rating,
            reviewTitle = reviewTitle,
            reviewText = reviewText,
            timestamp = System.currentTimeMillis(),
            isUserReview = true,
            helpfulCount = 1
        )
        bookReviewDao.insertReview(BookReviewEntity.fromModel(review))
    }

    suspend fun deleteReview(id: String) = withContext(Dispatchers.IO) {
        bookReviewDao.deleteReview(id)
    }

    suspend fun incrementHelpful(id: String) = withContext(Dispatchers.IO) {
        bookReviewDao.incrementHelpful(id)
    }

    suspend fun getBookById(id: String): Book? = withContext(Dispatchers.IO) {
        bookDao.getBookById(id)?.toModel()
    }

    fun getHighlightsForBook(bookId: String): Flow<List<Highlight>> {
        return highlightDao.getHighlightsForBook(bookId).map { list -> list.map { it.toModel() } }
    }

    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksForBook(bookId).map { list -> list.map { it.toModel() } }
    }

    suspend fun toggleFavorite(bookId: String, currentVal: Boolean) = withContext(Dispatchers.IO) {
        bookDao.updateFavorite(bookId, !currentVal)
    }

    suspend fun updateReadingProgress(bookId: String, page: Int, progress: Float, sessionMinutes: Int = 1) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        bookDao.updateReadingProgress(bookId, page, progress, now, sessionMinutes)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(Date(now))
        readingSessionDao.insertSession(
            ReadingSessionEntity(
                bookId = bookId,
                durationMinutes = sessionMinutes,
                pagesRead = 1,
                dateString = dateStr,
                timestamp = now
            )
        )
    }

    suspend fun addHighlight(
        bookId: String,
        bookTitle: String,
        chapterIndex: Int,
        chapterTitle: String,
        text: String,
        note: String?,
        color: HighlightColor,
        page: Int
    ) = withContext(Dispatchers.IO) {
        val highlight = Highlight(
            id = "hl-" + UUID.randomUUID().toString().take(8),
            bookId = bookId,
            bookTitle = bookTitle,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            text = text,
            note = note,
            color = color,
            pageOrLocation = page,
            timestamp = System.currentTimeMillis()
        )
        highlightDao.insertHighlight(HighlightEntity.fromModel(highlight))
    }

    suspend fun deleteHighlight(id: String) = withContext(Dispatchers.IO) {
        highlightDao.deleteHighlight(id)
    }

    suspend fun addBookmark(
        bookId: String,
        bookTitle: String,
        chapterIndex: Int,
        chapterTitle: String,
        page: Int,
        title: String,
        note: String?
    ) = withContext(Dispatchers.IO) {
        val bookmark = Bookmark(
            id = "bm-" + UUID.randomUUID().toString().take(8),
            bookId = bookId,
            bookTitle = bookTitle,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            page = page,
            title = title.ifBlank { "Page $page Bookmark" },
            note = note,
            timestamp = System.currentTimeMillis()
        )
        bookmarkDao.insertBookmark(BookmarkEntity.fromModel(bookmark))
    }

    suspend fun deleteBookmark(id: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(id)
    }

    suspend fun deleteBook(id: String) = withContext(Dispatchers.IO) {
        bookDao.deleteBookById(id)
    }

    suspend fun importBookFromUri(uri: Uri, displayName: String): Book? = withContext(Dispatchers.IO) {
        try {
            val extension = displayName.substringAfterLast(".", "").lowercase()
            val format = when (extension) {
                "pdf" -> BookFormat.PDF
                "epub" -> BookFormat.EPUB
                else -> BookFormat.TXT
            }

            val savedFile = File(context.filesDir, "imported_${System.currentTimeMillis()}_$displayName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(savedFile).use { output ->
                    input.copyTo(output)
                }
            }

            var title = displayName.substringBeforeLast(".")
            var author = "Imported Document"
            var totalPages = 50
            var desc = "Imported local e-book document."

            if (format == BookFormat.EPUB) {
                savedFile.inputStream().use { stream ->
                    val parsed = EpubParser.parseEpubStream(stream, title)
                    title = parsed.title
                    author = parsed.author
                    totalPages = parsed.chapters.size * 5
                }
            } else if (format == BookFormat.TXT) {
                savedFile.inputStream().use { stream ->
                    val parsed = EpubParser.parsePlainTextStream(stream, displayName)
                    totalPages = parsed.chapters.size * 3
                }
            }

            val newBook = Book(
                id = "imported-" + UUID.randomUUID().toString().take(8),
                title = title,
                author = author,
                description = desc,
                format = format,
                status = ReadingStatus.WANT_TO_READ,
                coverGradientStart = 0xFF1E293B,
                coverGradientEnd = 0xFF475569,
                totalPages = totalPages,
                currentPage = 1,
                readingProgress = 0f,
                isFavorite = false,
                isDownloaded = true,
                localFilePath = savedFile.absolutePath,
                fileSize = "${(savedFile.length() / 1024 / 1024.0 * 10).toInt() / 10.0} MB",
                genre = "Imported Document",
                tags = listOf("Local Import", format.displayName),
                rating = 5.0f,
                addedTimestamp = System.currentTimeMillis()
            )

            bookDao.insertBook(BookEntity.fromModel(newBook))
            return@withContext newBook
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun addConvertedEpubBook(file: File, title: String, author: String): Book? = withContext(Dispatchers.IO) {
        try {
            var totalPages = 50
            file.inputStream().use { stream ->
                val parsed = EpubParser.parseEpubStream(stream, title)
                totalPages = (parsed.chapters.size * 6).coerceAtLeast(10)
            }

            val newBook = Book(
                id = "epub-" + UUID.randomUUID().toString().take(8),
                title = title,
                author = author,
                description = "Converted EPUB 3 digital publication with formatted typography and structured index.",
                format = BookFormat.EPUB,
                status = ReadingStatus.WANT_TO_READ,
                coverGradientStart = 0xFF0D9488,
                coverGradientEnd = 0xFF059669,
                totalPages = totalPages,
                currentPage = 1,
                readingProgress = 0f,
                isFavorite = false,
                isDownloaded = true,
                localFilePath = file.absolutePath,
                fileSize = "${(file.length() / 1024 / 1024.0 * 10).toInt() / 10.0} MB".ifBlank { "${(file.length() / 1024)} KB" },
                genre = "Converted EPUB",
                tags = listOf("EPUB 3", "Converted", "Digital Edition"),
                rating = 5.0f,
                addedTimestamp = System.currentTimeMillis()
            )

            bookDao.insertBook(BookEntity.fromModel(newBook))
            return@withContext newBook
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun downloadCatalogBook(book: Book) = withContext(Dispatchers.IO) {
        val downloadedBook = book.copy(
            isDownloaded = true,
            status = ReadingStatus.WANT_TO_READ,
            addedTimestamp = System.currentTimeMillis()
        )
        bookDao.insertBook(BookEntity.fromModel(downloadedBook))
    }

    suspend fun calculateStreakData(dailyGoalMinutes: Int = 20): ReadingStreakData = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val today = System.currentTimeMillis()

        val allSessions = readingSessionDao.getAllSessions().first()
        val sessionsByDate = allSessions.groupBy { it.dateString }
        val books = bookDao.getAllBooks().first().map { it.toModel() }
        val allHighlights = highlightDao.getAllHighlights().first()

        // Get past 7 days stats (Weekly)
        val weeklyStats = mutableListOf<DayReadingStat>()
        var todayMinutes = 0
        var todayPages = 0

        for (i in 6 downTo 0) {
            val dayTime = today - (i * 86400000L)
            val dateStr = dateFormat.format(Date(dayTime))
            val dayName = dayNameFormat.format(Date(dayTime))
            val sessionsForDay = sessionsByDate[dateStr] ?: emptyList()
            val minutes = sessionsForDay.sumOf { it.durationMinutes }
            val pages = sessionsForDay.sumOf { it.pagesRead }

            if (i == 0) {
                todayMinutes = minutes
                todayPages = pages
            }

            val goalReached = minutes >= dailyGoalMinutes && dailyGoalMinutes > 0 && minutes > 0
            weeklyStats.add(
                DayReadingStat(
                    date = dateStr,
                    dayOfWeek = dayName,
                    minutesRead = minutes,
                    pagesRead = pages,
                    isGoalReached = goalReached
                )
            )
        }

        // Get past 30 days stats (Monthly)
        val monthlyStats = mutableListOf<DayReadingStat>()
        for (i in 29 downTo 0) {
            val dayTime = today - (i * 86400000L)
            val dateStr = dateFormat.format(Date(dayTime))
            val dayName = dayNameFormat.format(Date(dayTime))
            val sessionsForDay = sessionsByDate[dateStr] ?: emptyList()
            val minutes = sessionsForDay.sumOf { it.durationMinutes }
            val pages = sessionsForDay.sumOf { it.pagesRead }

            monthlyStats.add(
                DayReadingStat(
                    date = dateStr,
                    dayOfWeek = dayName,
                    minutesRead = minutes,
                    pagesRead = pages,
                    isGoalReached = minutes >= dailyGoalMinutes && dailyGoalMinutes > 0 && minutes > 0
                )
            )
        }

        // Logical streak calculation:
        // A day counts if reading occurred (minutes > 0 or sessions exist).
        // 1. If user read today: streak starts at 1, then check yesterday, 2 days ago, 3 days ago...
        // 2. If user hasn't read today: check yesterday. If read yesterday, streak is alive from yesterday! Check 2 days ago, 3 days ago...
        // 3. If user didn't read today AND didn't read yesterday: streak is 0.
        var streak = 0
        val cal = Calendar.getInstance()
        cal.timeInMillis = today
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val todayDateStr = dateFormat.format(cal.time)
        val todayHasRead = (sessionsByDate[todayDateStr]?.sumOf { it.durationMinutes } ?: 0) > 0

        var checkOffset = if (todayHasRead) 0 else 1
        var isStreakActive = true

        if (!todayHasRead) {
            val yesterdayCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis - 86400000L
            }
            val yesterdayDateStr = dateFormat.format(yesterdayCal.time)
            val yesterdayHasRead = (sessionsByDate[yesterdayDateStr]?.sumOf { it.durationMinutes } ?: 0) > 0
            if (!yesterdayHasRead) {
                isStreakActive = false
                streak = 0
            }
        }

        if (isStreakActive) {
            while (true) {
                val dayCal = Calendar.getInstance().apply {
                    timeInMillis = cal.timeInMillis - (checkOffset * 86400000L)
                }
                val checkDateStr = dateFormat.format(dayCal.time)
                val dayMinutes = sessionsByDate[checkDateStr]?.sumOf { it.durationMinutes } ?: 0
                if (dayMinutes > 0) {
                    streak++
                    checkOffset++
                } else {
                    break
                }
            }
        }

        // Calculate all-time longest streak from historical dates
        val readingDates = sessionsByDate.filter { (_, sessions) ->
            sessions.sumOf { it.durationMinutes } > 0
        }.keys.sorted()

        var calculatedLongest = streak
        if (readingDates.isNotEmpty()) {
            var tempStreak = 1
            for (j in 1 until readingDates.size) {
                try {
                    val prevDate = dateFormat.parse(readingDates[j - 1])
                    val currDate = dateFormat.parse(readingDates[j])
                    if (prevDate != null && currDate != null) {
                        val diffDays = ((currDate.time - prevDate.time) / (1000 * 60 * 60 * 24)).toInt()
                        if (diffDays == 1) {
                            tempStreak++
                            if (tempStreak > calculatedLongest) {
                                calculatedLongest = tempStreak
                            }
                        } else if (diffDays > 1) {
                            tempStreak = 1
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        val prefs = context.getSharedPreferences("reading_streak_prefs", Context.MODE_PRIVATE)
        val savedLongest = prefs.getInt("longest_streak_days", 0)
        val finalLongestStreak = maxOf(savedLongest, calculatedLongest, streak)

        prefs.edit()
            .putInt("current_streak_days", streak)
            .putInt("longest_streak_days", finalLongestStreak)
            .putInt("minutes_read_today", todayMinutes)
            .putInt("daily_goal_minutes", dailyGoalMinutes)
            .apply()

        val totalMinutes = allSessions.sumOf { it.durationMinutes }
        val totalPages = allSessions.sumOf { it.pagesRead }
        val totalBooksFinished = books.count { it.status == ReadingStatus.FINISHED || it.readingProgress >= 0.99f }
        val totalSessionsCount = allSessions.size
        val avgSessionMinutes = if (allSessions.isNotEmpty()) {
            allSessions.map { it.durationMinutes }.average().toFloat()
        } else {
            0f
        }

        // Dynamic badges based on real achievements
        val badges = SampleBooksData.INITIAL_BADGES.map { b ->
            when (b.id) {
                "badge-first-step" -> {
                    val unlocked = allSessions.isNotEmpty() || totalMinutes > 0
                    b.copy(
                        isUnlocked = unlocked,
                        progress = if (unlocked) 1.0f else (totalMinutes / 1f).coerceIn(0f, 1f),
                        unlockedAt = if (unlocked) (allSessions.firstOrNull()?.timestamp ?: today) else null
                    )
                }
                "badge-3-day" -> {
                    val unlocked = finalLongestStreak >= 3 || streak >= 3
                    b.copy(
                        isUnlocked = unlocked,
                        progress = (streak / 3.0f).coerceIn(0f, 1f),
                        unlockedAt = if (unlocked) today else null
                    )
                }
                "badge-7-day" -> {
                    val unlocked = finalLongestStreak >= 7 || streak >= 7
                    b.copy(
                        isUnlocked = unlocked,
                        progress = (streak / 7.0f).coerceIn(0f, 1f),
                        unlockedAt = if (unlocked) today else null
                    )
                }
                "badge-30-day" -> {
                    val unlocked = finalLongestStreak >= 30 || streak >= 30
                    b.copy(
                        isUnlocked = unlocked,
                        progress = (streak / 30.0f).coerceIn(0f, 1f),
                        unlockedAt = if (unlocked) today else null
                    )
                }
                "badge-page-master" -> {
                    val unlocked = totalPages >= 100
                    b.copy(
                        isUnlocked = unlocked,
                        progress = (totalPages / 100.0f).coerceIn(0f, 1f),
                        unlockedAt = if (unlocked) today else null
                    )
                }
                "badge-highlighter" -> {
                    val count = allHighlights.size
                    val unlocked = count >= 10
                    b.copy(
                        isUnlocked = unlocked,
                        progress = (count / 10.0f).coerceIn(0f, 1f),
                        unlockedAt = if (unlocked) today else null
                    )
                }
                else -> b
            }
        }

        ReadingStreakData(
            currentStreakDays = streak,
            longestStreakDays = finalLongestStreak,
            totalMinutesRead = totalMinutes,
            totalPagesRead = totalPages,
            totalBooksRead = totalBooksFinished,
            totalBooksInLibrary = books.size,
            avgSessionMinutes = avgSessionMinutes,
            totalSessionsCount = totalSessionsCount,
            dailyGoalMinutes = dailyGoalMinutes,
            dailyGoalPages = (dailyGoalMinutes * 1.25f).toInt(),
            todayMinutesRead = todayMinutes,
            todayPagesRead = todayPages,
            readingSpeedWpm = if (totalMinutes > 0 && totalPages > 0) ((totalPages * 250) / totalMinutes).coerceIn(150, 400) else 240,
            lastReadDate = todayDateStr,
            weeklyStats = weeklyStats,
            monthlyStats = monthlyStats,
            badges = badges
        )
    }

    suspend fun exportHighlightsToMarkdown(): String = withContext(Dispatchers.IO) {
        val highlights = highlightDao.getAllHighlights().first().map { it.toModel() }
        val sb = StringBuilder()
        sb.append("# A-Hex streak - Reading Highlights & Notes\n\n")
        sb.append("Exported on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
        sb.append("Creator / Contact: @ahex0_01\n\n")
        sb.append("---\n\n")

        val grouped = highlights.groupBy { it.bookTitle }
        grouped.forEach { (bookTitle, bookHls) ->
            sb.append("## $bookTitle\n\n")
            bookHls.forEach { h ->
                sb.append("> \"${h.text}\"\n\n")
                if (!h.note.isNullOrBlank()) {
                    sb.append("**Note:** ${h.note}\n\n")
                }
                sb.append("*Chapter: ${h.chapterTitle} | Color: ${h.color.displayName}*\n\n")
            }
            sb.append("\n")
        }
        sb.toString()
    }
}

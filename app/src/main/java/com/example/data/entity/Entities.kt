package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.Book
import com.example.model.BookFormat
import com.example.model.Highlight
import com.example.model.HighlightColor
import com.example.model.Bookmark
import com.example.model.ReadingStatus

@Entity(
    tableName = "books",
    indices = [
        Index("status"),
        Index("genre"),
        Index("lastReadTimestamp")
    ]
)
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val format: String,
    val status: String,
    val coverGradientStart: Long,
    val coverGradientEnd: Long,
    val coverImageUrl: String?,
    val totalPages: Int,
    val currentPage: Int,
    val readingProgress: Float,
    val isFavorite: Boolean,
    val isDownloaded: Boolean,
    val localFilePath: String?,
    val fileSize: String,
    val genre: String,
    val tagsRaw: String, // Comma separated
    val rating: Float,
    val lastReadTimestamp: Long,
    val addedTimestamp: Long,
    val totalMinutesSpent: Int,
    val customShelvesRaw: String
) {
    fun toModel(): Book {
        return Book(
            id = id,
            title = title,
            author = author,
            description = description,
            format = try { BookFormat.valueOf(format) } catch (_: Exception) { BookFormat.EPUB },
            status = try { ReadingStatus.valueOf(status) } catch (_: Exception) { ReadingStatus.WANT_TO_READ },
            coverGradientStart = coverGradientStart,
            coverGradientEnd = coverGradientEnd,
            coverImageUrl = coverImageUrl,
            totalPages = totalPages,
            currentPage = currentPage,
            readingProgress = readingProgress,
            isFavorite = isFavorite,
            isDownloaded = isDownloaded,
            localFilePath = localFilePath,
            fileSize = fileSize,
            genre = genre,
            tags = if (tagsRaw.isBlank()) emptyList() else tagsRaw.split(",").map { it.trim() },
            rating = rating,
            lastReadTimestamp = lastReadTimestamp,
            addedTimestamp = addedTimestamp,
            totalMinutesSpent = totalMinutesSpent,
            customShelves = if (customShelvesRaw.isBlank()) emptyList() else customShelvesRaw.split(",").map { it.trim() }
        )
    }

    companion object {
        fun fromModel(book: Book): BookEntity {
            return BookEntity(
                id = book.id,
                title = book.title,
                author = book.author,
                description = book.description,
                format = book.format.name,
                status = book.status.name,
                coverGradientStart = book.coverGradientStart,
                coverGradientEnd = book.coverGradientEnd,
                coverImageUrl = book.coverImageUrl,
                totalPages = book.totalPages,
                currentPage = book.currentPage,
                readingProgress = book.readingProgress,
                isFavorite = book.isFavorite,
                isDownloaded = book.isDownloaded,
                localFilePath = book.localFilePath,
                fileSize = book.fileSize,
                genre = book.genre,
                tagsRaw = book.tags.joinToString(","),
                rating = book.rating,
                lastReadTimestamp = book.lastReadTimestamp,
                addedTimestamp = book.addedTimestamp,
                totalMinutesSpent = book.totalMinutesSpent,
                customShelvesRaw = book.customShelves.joinToString(",")
            )
        }
    }
}

@Entity(
    tableName = "highlights",
    indices = [
        Index("bookId"),
        Index("timestamp")
    ]
)
data class HighlightEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val bookTitle: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val text: String,
    val note: String?,
    val colorHex: String,
    val pageOrLocation: Int,
    val timestamp: Long
) {
    fun toModel(): Highlight {
        return Highlight(
            id = id,
            bookId = bookId,
            bookTitle = bookTitle,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            text = text,
            note = note,
            color = HighlightColor.fromHex(colorHex),
            pageOrLocation = pageOrLocation,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromModel(h: Highlight): HighlightEntity {
            return HighlightEntity(
                id = h.id,
                bookId = h.bookId,
                bookTitle = h.bookTitle,
                chapterIndex = h.chapterIndex,
                chapterTitle = h.chapterTitle,
                text = h.text,
                note = h.note,
                colorHex = h.color.hex,
                pageOrLocation = h.pageOrLocation,
                timestamp = h.timestamp
            )
        }
    }
}

@Entity(
    tableName = "bookmarks",
    indices = [
        Index("bookId"),
        Index("timestamp")
    ]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val bookTitle: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val page: Int,
    val title: String,
    val note: String?,
    val timestamp: Long
) {
    fun toModel(): Bookmark {
        return Bookmark(
            id = id,
            bookId = bookId,
            bookTitle = bookTitle,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            page = page,
            title = title,
            note = note,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromModel(b: Bookmark): BookmarkEntity {
            return BookmarkEntity(
                id = b.id,
                bookId = b.bookId,
                bookTitle = b.bookTitle,
                chapterIndex = b.chapterIndex,
                chapterTitle = b.chapterTitle,
                page = b.page,
                title = b.title,
                note = b.note,
                timestamp = b.timestamp
            )
        }
    }
}

@Entity(
    tableName = "reading_sessions",
    indices = [
        Index("bookId"),
        Index("dateString"),
        Index("timestamp")
    ]
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val durationMinutes: Int,
    val pagesRead: Int,
    val dateString: String, // YYYY-MM-DD
    val timestamp: Long
)

@Entity(
    tableName = "book_reviews",
    indices = [
        Index("bookId"),
        Index("timestamp")
    ]
)
data class BookReviewEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val bookTitle: String,
    val userName: String,
    val userAvatarColor: Long,
    val rating: Float,
    val reviewTitle: String,
    val reviewText: String,
    val timestamp: Long,
    val isUserReview: Boolean,
    val helpfulCount: Int
) {
    fun toModel(): com.example.model.BookReview {
        return com.example.model.BookReview(
            id = id,
            bookId = bookId,
            bookTitle = bookTitle,
            userName = userName,
            userAvatarColor = userAvatarColor,
            rating = rating,
            reviewTitle = reviewTitle,
            reviewText = reviewText,
            timestamp = timestamp,
            isUserReview = isUserReview,
            helpfulCount = helpfulCount
        )
    }

    companion object {
        fun fromModel(r: com.example.model.BookReview): BookReviewEntity {
            return BookReviewEntity(
                id = r.id,
                bookId = r.bookId,
                bookTitle = r.bookTitle,
                userName = r.userName,
                userAvatarColor = r.userAvatarColor,
                rating = r.rating,
                reviewTitle = r.reviewTitle,
                reviewText = r.reviewText,
                timestamp = r.timestamp,
                isUserReview = r.isUserReview,
                helpfulCount = r.helpfulCount
            )
        }
    }
}


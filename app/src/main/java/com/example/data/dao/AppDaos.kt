package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BookEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.HighlightEntity
import com.example.data.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC")
    suspend fun getAllBooksSnapshot(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE isFavorite = 1")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET currentPage = :page, readingProgress = :progress, lastReadTimestamp = :time, totalMinutesSpent = totalMinutesSpent + :minutes WHERE id = :id")
    suspend fun updateReadingProgress(id: String, page: Int, progress: Float, time: Long, minutes: Int)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: String)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights ORDER BY timestamp DESC")
    fun getAllHighlights(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY timestamp DESC")
    suspend fun getAllHighlightsSnapshot(): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY pageOrLocation ASC, timestamp ASC")
    fun getHighlightsForBook(bookId: String): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlights(highlights: List<HighlightEntity>)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    suspend fun getAllBookmarksSnapshot(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY page ASC")
    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)
}

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessionsSnapshot(): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions WHERE dateString >= :startDate ORDER BY timestamp ASC")
    suspend fun getSessionsSince(startDate: String): List<ReadingSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ReadingSessionEntity>)
}

@Dao
interface BookReviewDao {
    @Query("SELECT * FROM book_reviews ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<com.example.data.entity.BookReviewEntity>>

    @Query("SELECT * FROM book_reviews ORDER BY timestamp DESC")
    suspend fun getAllReviewsSnapshot(): List<com.example.data.entity.BookReviewEntity>

    @Query("SELECT * FROM book_reviews WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getReviewsForBook(bookId: String): Flow<List<com.example.data.entity.BookReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: com.example.data.entity.BookReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<com.example.data.entity.BookReviewEntity>)

    @Query("DELETE FROM book_reviews WHERE id = :id")
    suspend fun deleteReview(id: String)

    @Query("UPDATE book_reviews SET helpfulCount = helpfulCount + 1 WHERE id = :id")
    suspend fun incrementHelpful(id: String)
}


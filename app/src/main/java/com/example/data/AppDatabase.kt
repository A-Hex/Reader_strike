package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BookDao
import com.example.data.dao.BookReviewDao
import com.example.data.dao.BookmarkDao
import com.example.data.dao.HighlightDao
import com.example.data.dao.ReadingSessionDao
import com.example.data.entity.BookEntity
import com.example.data.entity.BookReviewEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.HighlightEntity
import com.example.data.entity.ReadingSessionEntity

@Database(
    entities = [
        BookEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
        ReadingSessionEntity::class,
        BookReviewEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun bookReviewDao(): BookReviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS book_reviews (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        bookTitle TEXT NOT NULL,
                        userName TEXT NOT NULL,
                        userAvatarColor INTEGER NOT NULL,
                        rating REAL NOT NULL,
                        reviewTitle TEXT NOT NULL,
                        reviewText TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isUserReview INTEGER NOT NULL,
                        helpfulCount INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add non-destructive performance indices for fast queries on foreign keys and filters
                db.execSQL("CREATE INDEX IF NOT EXISTS index_books_status ON books(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_books_genre ON books(genre)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_books_lastReadTimestamp ON books(lastReadTimestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_highlights_bookId ON highlights(bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_highlights_timestamp ON highlights(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_bookId ON bookmarks(bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_timestamp ON bookmarks(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_sessions_bookId ON reading_sessions(bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_sessions_dateString ON reading_sessions(dateString)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_sessions_timestamp ON reading_sessions(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_reviews_bookId ON book_reviews(bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_reviews_timestamp ON book_reviews(timestamp)")
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ahex_streak_library.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

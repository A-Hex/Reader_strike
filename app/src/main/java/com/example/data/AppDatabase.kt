package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 2,
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ahex_streak_library.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

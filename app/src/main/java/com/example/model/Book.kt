package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.util.AppLanguage
import com.example.util.AppStrings

enum class BookFormat(val extension: String, val displayName: String) {
    EPUB("epub", "EPUB"),
    PDF("pdf", "PDF"),
    TXT("txt", "TXT")
}

enum class ReadingStatus(val displayName: String, val stringKey: String) {
    ALL("All Books", "status_all"),
    READING("Currently Reading", "status_reading"),
    WANT_TO_READ("Want to Read", "status_want_to_read"),
    FINISHED("Finished", "status_finished"),
    FAVORITES("Favorites", "status_favorites"),
    DOWNLOADED("Offline Ready", "status_downloaded");

    fun getLocalizedTitle(language: AppLanguage): String = AppStrings.get(stringKey, language)
}

enum class SortOption(val displayName: String, val stringKey: String) {
    RECENTLY_READ("Recently Read", "sort_recent"),
    TITLE("Title (A-Z)", "sort_title"),
    AUTHOR("Author", "sort_author"),
    PROGRESS("Reading Progress", "sort_progress"),
    DATE_ADDED("Recently Added", "sort_date_added");

    fun getLocalizedTitle(language: AppLanguage): String = AppStrings.get(stringKey, language)
}

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val format: BookFormat,
    val status: ReadingStatus = ReadingStatus.WANT_TO_READ,
    val coverGradientStart: Long = 0xFF1E3A8A,
    val coverGradientEnd: Long = 0xFF3B82F6,
    val coverImageUrl: String? = null,
    val coverDrawableRes: Int? = null,
    val totalPages: Int = 100,
    val currentPage: Int = 1,
    val readingProgress: Float = 0.0f,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = true,
    val localFilePath: String? = null,
    val fileSize: String = "1.2 MB",
    val genre: String = "Classic Literature",
    val tags: List<String> = emptyList(),
    val rating: Float = 4.5f,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val addedTimestamp: Long = System.currentTimeMillis(),
    val totalMinutesSpent: Int = 0,
    val customShelves: List<String> = emptyList(),
    val languageCode: String = "en"
) {
    fun getEffectiveCoverRes(): Int {
        if (coverDrawableRes != null && coverDrawableRes != 0) {
            return coverDrawableRes
        }
        return when (id) {
            "book-metamorphosis" -> R.drawable.img_cover_metamorphosis
            "book-art-of-war" -> R.drawable.img_cover_art_of_war
            "book-alice-wonderland" -> R.drawable.img_cover_alice
            "book-sherlock-holmes" -> R.drawable.img_cover_sherlock
            "book-meditations" -> R.drawable.img_cover_meditations
            "book-frankenstein" -> R.drawable.img_cover_frankenstein
            "cat-great-gatsby" -> R.drawable.img_cover_gatsby
            "cat-dracula" -> R.drawable.img_cover_dracula
            "cat-pride-prejudice" -> R.drawable.img_cover_pride_prejudice
            "cat-beyond-good-evil" -> R.drawable.img_cover_nietzsche
            "cat-republic" -> R.drawable.img_cover_republic
            "cat-letters-stoic" -> R.drawable.img_cover_stoic
            "cat-dorian-gray" -> R.drawable.img_cover_dorian
            else -> {
                when {
                    genre.contains("Philosophy", ignoreCase = true) || title.contains("Stoic", ignoreCase = true) -> R.drawable.img_cover_meditations
                    genre.contains("Horror", ignoreCase = true) || genre.contains("Gothic", ignoreCase = true) -> R.drawable.img_cover_dracula
                    genre.contains("Mystery", ignoreCase = true) || genre.contains("Detective", ignoreCase = true) -> R.drawable.img_cover_sherlock
                    genre.contains("Strategy", ignoreCase = true) || genre.contains("War", ignoreCase = true) -> R.drawable.img_cover_art_of_war
                    genre.contains("Romance", ignoreCase = true) -> R.drawable.img_cover_pride_prejudice
                    genre.contains("Fantasy", ignoreCase = true) || genre.contains("Adventure", ignoreCase = true) -> R.drawable.img_cover_alice
                    else -> R.drawable.img_cover_metamorphosis
                }
            }
        }
    }
}

data class BookChapter(
    val index: Int,
    val title: String,
    val content: String,
    val wordCount: Int = content.split("\\s+".toRegex()).size
)

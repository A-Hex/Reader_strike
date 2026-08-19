package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.model.Book
import com.example.model.Highlight
import com.example.model.ReadingStreakData

object SocialShareHelper {

    const val CREATOR_HANDLE = "@ahex0_01"
    const val APP_NAME = "A-Hex streak"
    const val INSTAGRAM_URL = "https://instagram.com/ahex0_01"

    fun formatStatsShareText(streakData: ReadingStreakData): String {
        return """
🔥 A-Hex Reading Streak Update! 🔥

⚡ Current Streak: ${streakData.currentStreakDays} Days
📖 Total Pages Read: ${streakData.totalPagesRead} Pages
📚 Books Finished: ${streakData.totalBooksRead} Books
⏱️ Avg Session: ${(streakData.avgSessionMinutes).toInt()} mins / session
🚀 Reading Speed: ${streakData.readingSpeedWpm} WPM

Building unbreakable reading habits with A-Hex streak!
Tagging creator $CREATOR_HANDLE

#AHexStreak #ReadingStreak #BookLover #ReadingHabits #DailyReading #Ahex
        """.trimIndent()
    }

    fun formatHighlightShareText(highlight: Highlight): String {
        val noteSection = if (!highlight.note.isNullOrBlank()) "\n💭 Note: ${highlight.note}\n" else ""
        return """
✨ Favorite Highlight from '${highlight.bookTitle}' ✨

"${highlight.text}"
$noteSection
🔖 ${highlight.chapterTitle} • Saved on $APP_NAME
Tagging $CREATOR_HANDLE

#AHexStreak #BookQuotes #ReadingInspiration #Literature #BookNotes
        """.trimIndent()
    }

    fun formatBookProgressShareText(book: Book, streakData: ReadingStreakData): String {
        val percent = (book.readingProgress * 100).toInt()
        return """
📖 Reading Progress Update: '${book.title}' by ${book.author}

📊 Progress: $percent% (${book.currentPage}/${book.totalPages} pages)
🔥 Active Streak: ${streakData.currentStreakDays} days on $APP_NAME
🎯 Today's Reading: ${streakData.todayMinutesRead} min

Stay consistent with your reading goals! Tagging $CREATOR_HANDLE

#AHexStreak #CurrentlyReading #BookCommunity #ReadingGoals
        """.trimIndent()
    }

    fun shareToSocialPlatform(
        context: Context,
        content: String,
        targetInstagram: Boolean = false
    ) {
        if (targetInstagram) {
            val instagramIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                `package` = "com.instagram.android"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(instagramIntent)
                return
            } catch (_: ActivityNotFoundException) {
                // Fallback to regular chooser if Instagram is not installed
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "$APP_NAME - Reading Share")
            putExtra(Intent.EXTRA_TEXT, content)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(shareIntent, "Share with $CREATOR_HANDLE on...")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share content", Toast.LENGTH_SHORT).show()
        }
    }

    fun openInstagramProfile(context: Context) {
        val uri = Uri.parse(INSTAGRAM_URL)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Visiting $CREATOR_HANDLE...", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.model.Book
import com.example.model.Highlight
import com.example.model.ReadingStreakData

object SocialShareHelper {

    const val APP_NAME = "A-Hex streak"

    fun formatStatsShareText(streakData: ReadingStreakData): String {
        return """
🔥 My Reading Streak & Stats on $APP_NAME!

⚡ Current Streak: ${streakData.currentStreakDays} Consecutive Days
🎯 Daily Goal: ${streakData.dailyGoalMinutes} min/day
📖 Total Pages Read: ${streakData.totalPagesRead} Pages
📚 Books Finished: ${streakData.totalBooksRead} Books
⏱️ Avg Session: ${(streakData.avgSessionMinutes).toInt()} mins
🚀 Reading Speed: ${streakData.readingSpeedWpm} WPM

Building consistent daily reading habits with $APP_NAME! 📚✨
#ReadingStreak #BookLover #ReadingHabits #DailyReading #Bookworm
        """.trimIndent()
    }

    fun formatDailyGoalShareText(goalMinutes: Int, todayMinutes: Int, streakDays: Int): String {
        val statusText = if (todayMinutes >= goalMinutes) {
            "🎉 Goal Completed! I crushed my daily reading goal of $goalMinutes mins by reading $todayMinutes mins today!"
        } else {
            "🎯 In Progress: Read $todayMinutes of $goalMinutes mins today!"
        }
        return """
$statusText

🔥 Active Reading Streak: $streakDays Days
📱 Tracked with $APP_NAME

Stay consistent and make reading a daily ritual! 📖✨
#DailyReadingGoal #ReadingStreak #BookCommunity #ReadingHabits #Consistency
        """.trimIndent()
    }

    fun formatHighlightShareText(highlight: Highlight): String {
        val noteSection = if (!highlight.note.isNullOrBlank()) "\n💭 Note: ${highlight.note}\n" else ""
        return """
✨ Favorite Highlight from '${highlight.bookTitle}' ✨

"${highlight.text}"
$noteSection
🔖 ${highlight.chapterTitle} • Saved on $APP_NAME

#BookQuotes #ReadingInspiration #Literature #BookNotes #Bookworm
        """.trimIndent()
    }

    fun formatBookProgressShareText(book: Book, streakData: ReadingStreakData): String {
        val percent = (book.readingProgress * 100).toInt()
        return """
📖 Reading Progress: '${book.title}' by ${book.author}

📊 Progress: $percent% (${book.currentPage}/${book.totalPages} pages)
🔥 Active Streak: ${streakData.currentStreakDays} days on $APP_NAME
🎯 Today's Reading: ${streakData.todayMinutesRead} min

#CurrentlyReading #BookCommunity #ReadingGoals #ReadingStreak
        """.trimIndent()
    }

    fun formatAchievementShareText(title: String, description: String, streakDays: Int): String {
        return """
🏆 New Reading Achievement Unlocked! 🏆

⭐ $title
📝 $description
🔥 Active Streak: $streakDays Days on $APP_NAME

Keep reading every day! 📚🚀
#ReadingAchievement #BookLover #Milestone #ReadingHabits
        """.trimIndent()
    }

    /**
     * Universal Share: Opens the system share sheet (chooser) letting the user pick
     * any app installed on their device (WhatsApp, Twitter/X, Messages, Notes, Gmail, Telegram, etc.)
     */
    fun shareContent(
        context: Context,
        content: String,
        subject: String = "$APP_NAME - Reading Progress"
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, content)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(shareIntent, "Share your achievement via...")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch share chooser", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Helper to copy text directly to clipboard
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Reading Progress") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to copy text", Toast.LENGTH_SHORT).show()
        }
    }
}

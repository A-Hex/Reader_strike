package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.notification.ReadingNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReadingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            ACTION_DAILY_REMINDER -> {
                handleDailyReminder(context)
            }
            ACTION_SNOOZE -> {
                ReadingNotificationManager.scheduleSnoozeReminder(context, 60)
            }
            ACTION_TEST_NOTIFICATION -> {
                ReadingNotificationManager.sendTestNotification(context)
            }
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                handleBootOrReplaced(context)
            }
        }
    }

    private fun handleDailyReminder(context: Context) {
        val prefs = context.getSharedPreferences(ReadingNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(ReadingNotificationManager.KEY_NOTIFICATIONS_ENABLED, true)
        val hour = prefs.getInt(ReadingNotificationManager.KEY_REMINDER_HOUR, 20)
        val minute = prefs.getInt(ReadingNotificationManager.KEY_REMINDER_MINUTE, 0)

        if (!isEnabled) return

        // Check if user already read enough today
        val streakPrefs = context.getSharedPreferences("reading_streak_prefs", Context.MODE_PRIVATE)
        val minutesToday = streakPrefs.getInt("minutes_read_today", 0)
        val dailyGoal = streakPrefs.getInt("daily_goal_minutes", 20)
        val currentStreak = streakPrefs.getInt("current_streak_days", 0)

        // If goal not yet reached, trigger notification
        if (minutesToday < dailyGoal) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val recentBooks = db.bookDao().getAllBooksSnapshot()
                val topBook = recentBooks.firstOrNull { it.readingProgress < 1f } ?: recentBooks.firstOrNull()
                
                ReadingNotificationManager.sendReadingReminderNotification(
                    context = context,
                    lastBookTitle = topBook?.title,
                    currentStreak = currentStreak
                )
            }
        }

        // Reschedule for next day
        ReadingNotificationManager.scheduleDailyReminder(context, hour, minute)
    }

    private fun handleBootOrReplaced(context: Context) {
        val prefs = context.getSharedPreferences(ReadingNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(ReadingNotificationManager.KEY_NOTIFICATIONS_ENABLED, true)
        val hour = prefs.getInt(ReadingNotificationManager.KEY_REMINDER_HOUR, 20)
        val minute = prefs.getInt(ReadingNotificationManager.KEY_REMINDER_MINUTE, 0)

        ReadingNotificationManager.initChannels(context)
        if (isEnabled) {
            ReadingNotificationManager.scheduleDailyReminder(context, hour, minute)
        }
    }

    companion object {
        const val ACTION_DAILY_REMINDER = "com.example.ACTION_DAILY_REMINDER"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
        const val ACTION_TEST_NOTIFICATION = "com.example.ACTION_TEST_NOTIFICATION"
    }
}

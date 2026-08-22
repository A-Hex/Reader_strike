package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.ReadingReminderReceiver
import java.util.Calendar

object ReadingNotificationManager {

    const val CHANNEL_REMINDERS_ID = "reading_reminders_channel"
    const val CHANNEL_GOALS_ID = "reading_goals_channel"
    const val CHANNEL_STREAKS_ID = "reading_streaks_channel"

    const val NOTIFICATION_ID_DAILY_REMINDER = 1001
    const val NOTIFICATION_ID_GOAL_ACHIEVED = 1002
    const val NOTIFICATION_ID_STREAK_ALERT = 1003
    const val NOTIFICATION_ID_TEST = 1004

    const val PREFS_NAME = "ahex_notification_prefs"
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val KEY_REMINDER_HOUR = "reminder_hour"
    const val KEY_REMINDER_MINUTE = "reminder_minute"
    const val KEY_GOAL_ALERTS_ENABLED = "goal_alerts_enabled"
    const val KEY_STREAK_ALERTS_ENABLED = "streak_alerts_enabled"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Daily Reading Reminders Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Daily Reading Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gentle daily notifications to help you build and maintain a consistent reading habit."
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Goal Milestones & Celebrations Channel
            val goalsChannel = NotificationChannel(
                CHANNEL_GOALS_ID,
                "Goal Milestones & Achievements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Celebrations when you reach your daily reading target or achieve streak records."
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Streak Preservation Alerts Channel
            val streakChannel = NotificationChannel(
                CHANNEL_STREAKS_ID,
                "Streak Preservation Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when your active reading streak is about to reset."
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(reminderChannel, goalsChannel, streakChannel))
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendDailyGoalAchievedNotification(context: Context, minutesRead: Int, streakDays: Int) {
        if (!hasNotificationPermission(context)) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_GOAL_ALERTS_ENABLED, true)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", "STREAK")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_GOAL_ACHIEVED,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_hero_reading)
        } catch (e: Exception) {
            null
        }

        val title = "🎉 Daily Reading Goal Completed!"
        val text = "You've read $minutesRead minutes today and powered your streak to $streakDays days! Keep the momentum going."

        val builder = NotificationCompat.Builder(context, CHANNEL_GOALS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF5A8E72.toInt())

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_GOAL_ACHIEVED, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun sendReadingReminderNotification(context: Context, lastBookTitle: String? = null, currentStreak: Int = 0) {
        if (!hasNotificationPermission(context)) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", "LIBRARY")
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_DAILY_REMINDER,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 1 Hour
        val snoozeIntent = Intent(context, ReadingReminderReceiver::class.java).apply {
            action = ReadingReminderReceiver.ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (currentStreak > 0) {
            "🔥 Time to Read — Keep Your $currentStreak-Day Streak!"
        } else {
            "📖 Evening Reading Time"
        }

        val text = if (!lastBookTitle.isNullOrBlank()) {
            "Pick up where you left off in '$lastBookTitle'. Dive into a few pages to hit your daily goal."
        } else {
            "Relax with a quiet chapter today. Every minute counts towards your daily reading goal."
        }

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_onboarding_welcome)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .setColor(0xFF5A8E72.toInt())
            .addAction(R.drawable.ic_launcher_foreground, "Read Now", openAppPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Snooze 1h", snoozePendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY_REMINDER, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun sendStreakWarningNotification(context: Context, streakDays: Int) {
        if (!hasNotificationPermission(context)) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_STREAK_ALERTS_ENABLED, true)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", "STREAK")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_STREAK_ALERT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚡ Don't lose your $streakDays-day reading streak!"
        val text = "Midnight is approaching! Read for a few minutes before the day ends to keep your streak unbroken."

        val builder = NotificationCompat.Builder(context, CHANNEL_STREAKS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFE8985E.toInt())

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_STREAK_ALERT, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun sendTestNotification(context: Context) {
        initChannels(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", "LIBRARY")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "📚 A-Hex streak Notifications Active!"
        val text = "Your daily reading reminders and streak alerts are configured and working smoothly."

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF5A8E72.toInt())

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReadingReminderReceiver::class.java).apply {
            action = ReadingReminderReceiver.ACTION_DAILY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // In case exact alarm permission is restricted on Android 12+, fallback to inexact
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun scheduleSnoozeReminder(context: Context, minutesLater: Int = 60) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReadingReminderReceiver::class.java).apply {
            action = ReadingReminderReceiver.ACTION_DAILY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (minutesLater * 60 * 1000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReadingReminderReceiver::class.java).apply {
            action = ReadingReminderReceiver.ACTION_DAILY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}

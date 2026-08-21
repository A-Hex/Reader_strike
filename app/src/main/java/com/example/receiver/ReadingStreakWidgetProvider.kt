package com.example.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class ReadingStreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_reading_streak)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            // Read preferences if available
            val prefs = context.getSharedPreferences("reading_streak_prefs", Context.MODE_PRIVATE)
            val streak = prefs.getInt("current_streak_days", 0)
            val minutesToday = prefs.getInt("minutes_read_today", 0)
            val dailyGoal = prefs.getInt("daily_goal_minutes", 20)

            views.setTextViewText(R.id.widget_streak_count, "🔥 $streak Days")
            views.setTextViewText(R.id.widget_daily_progress, "$minutesToday / $dailyGoal min")
            val progressPercent = ((minutesToday.toFloat() / dailyGoal.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
            views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

package com.example.blurwidgetdemo.widgets.calendar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetAppearance
import java.text.DateFormat
import java.util.Date

class CalendarWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { updateWidget(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) = updateWidget(context, manager, id)

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val item = nextEvent(context)
            val compact = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
            val views = RemoteViews(context.packageName, if (compact) R.layout.widget_calendar_compact else R.layout.widget_calendar_large).apply {
                setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(prefs, id))
                setTextViewText(R.id.widget_primary, item.first)
                setTextViewText(R.id.widget_secondary, item.second)
                setViewVisibility(R.id.widget_secondary, if (compact) View.GONE else View.VISIBLE)
                setTextColor(R.id.widget_primary, WidgetAppearance.textColor(prefs, id))
                setTextColor(R.id.widget_secondary, WidgetAppearance.textColor(prefs, id))
                setInt(R.id.widget_primary, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                setInt(R.id.widget_secondary, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                setFloat(R.id.widget_primary, "setTextSize", 23f * WidgetAppearance.textScale(prefs, id) / 100f)
                setOnClickPendingIntent(android.R.id.background, calendarIntent(context))
            }
            manager.updateAppWidget(id, views)
        }

        private fun nextEvent(context: Context): Pair<String, String> = try {
            val now = System.currentTimeMillis()
            context.contentResolver.query(
                CalendarContract.Instances.CONTENT_URI.buildUpon().appendPath(now.toString()).appendPath((now + 7 * 86_400_000L).toString()).build(),
                arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN),
                null, null, "begin ASC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val title = cursor.getString(0).takeUnless { it.isNullOrBlank() } ?: "Untitled event"
                    val start = cursor.getLong(1)
                    val time = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(start))
                    title to "Starts $time"
                } else "No upcoming events" to "Tap to open Calendar"
            } ?: ("Calendar unavailable" to "Tap to open Calendar")
        } catch (_: SecurityException) {
            "Calendar permission needed" to "Tap to open Calendar"
        }

        private fun calendarIntent(context: Context): PendingIntent = PendingIntent.getActivity(context, 100,
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

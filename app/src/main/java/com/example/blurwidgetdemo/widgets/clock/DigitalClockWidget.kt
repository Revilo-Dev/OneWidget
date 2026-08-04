package com.example.blurwidgetdemo.widgets.clock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.R

class DigitalClockWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) =
        ids.forEach { updateWidget(context, manager, it) }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        updateWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, DigitalClockWidget::class.java))
                onUpdate(context, manager, ids)
            }
        }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val preferences = ClockWidgetPreferences.preferences(context)
        ids.forEach { ClockWidgetPreferences.delete(preferences, it) }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val options = manager.getAppWidgetOptions(id)
            val category = ClockWidgetLayout.category(
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40)
            )
            val settings = ClockWidgetPreferences.load(ClockWidgetPreferences.preferences(context), id)
            val views = RemoteViews(context.packageName, layoutFor(context, category, settings.font)).apply {
                // Preserve this exact root surface: One UI Home uses it for native wallpaper blur.
                setInt(android.R.id.background, "setBackgroundColor", if (settings.backgroundEnabled) settings.tintColor() else Color.TRANSPARENT)
                setViewVisibility(R.id.clock_date, if (settings.showDate) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.clock_date_above, if (settings.showDate && settings.dateAboveTime) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.clock_date, if (settings.showDate && !settings.dateAboveTime) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.clock_day, if (settings.showDate && settings.showDayOfWeek && category == ClockWidgetLayout.Category.LARGE) View.VISIBLE else View.GONE)
                applyTimeFormat(settings.timeFormat)
                val gravity = settings.alignment.gravity()
                setInt(R.id.clock_content, "setGravity", gravity)
                setInt(R.id.clock_time, "setGravity", gravity)
                setInt(R.id.clock_date, "setGravity", gravity)
                setInt(R.id.clock_day, "setGravity", gravity)
                val tapIntent = if (settings.tapAction == ClockTapAction.CLOCK) clockPendingIntent(context) else calendarPendingIntent(context)
                setOnClickPendingIntent(android.R.id.background, tapIntent)
                setOnClickPendingIntent(R.id.clock_content, tapIntent)
                setOnClickPendingIntent(R.id.clock_time, tapIntent)
                setOnClickPendingIntent(R.id.clock_date, tapIntent)
            }
            manager.updateAppWidget(id, views)
        }

        private fun RemoteViews.applyTimeFormat(format: ClockTimeFormat) {
            val format12: CharSequence
            val format24: CharSequence
            when (format) {
                ClockTimeFormat.SYSTEM -> { format12 = "h:mm a"; format24 = "HH:mm" }
                ClockTimeFormat.TWELVE_HOUR -> { format12 = "h:mm a"; format24 = "h:mm a" }
                ClockTimeFormat.TWENTY_FOUR_HOUR -> { format12 = "HH:mm"; format24 = "HH:mm" }
            }
            setCharSequence(R.id.clock_time, "setFormat12Hour", format12)
            setCharSequence(R.id.clock_time, "setFormat24Hour", format24)
        }

        private fun ClockTextAlignment.gravity(): Int = when (this) {
            ClockTextAlignment.START -> Gravity.START
            ClockTextAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
            ClockTextAlignment.END -> Gravity.END
        }

        private fun clockPendingIntent(context: Context): PendingIntent = pendingActivity(
            context, Intent(AlarmClock.ACTION_SHOW_ALARMS), Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CLOCK")
        )

        private fun calendarPendingIntent(context: Context): PendingIntent = pendingActivity(
            context,
            Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time")), Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        )

        private fun pendingActivity(context: Context, preferred: Intent, fallback: Intent): PendingIntent {
            val resolved = if (preferred.resolveActivity(context.packageManager) != null) preferred else fallback
            return PendingIntent.getActivity(context, 0, resolved.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun layoutFor(context: Context, category: ClockWidgetLayout.Category, font: ClockFont): Int {
            val size = when (category) { ClockWidgetLayout.Category.COMPACT -> "compact"; ClockWidgetLayout.Category.MEDIUM -> "medium"; ClockWidgetLayout.Category.LARGE -> "large" }
            val suffix = when (font) {
                ClockFont.SYSTEM -> return when (category) { ClockWidgetLayout.Category.COMPACT -> R.layout.widget_digital_clock_compact; ClockWidgetLayout.Category.MEDIUM -> R.layout.widget_digital_clock_medium; ClockWidgetLayout.Category.LARGE -> R.layout.widget_digital_clock_large }
                ClockFont.SAMSUNG_DEFAULT -> "samsung_default"; ClockFont.SAMSUNG_DEFAULT_BOLD -> "samsung_default_bold"; ClockFont.SAMSUNG_DEFAULT_THIN -> "samsung_default_thin"; ClockFont.SAMSUNG_MONO -> "samsung_mono"
                ClockFont.CLOCK_BOLD_SERIF -> "clock_bold_serif"; ClockFont.CLOCK_STRIPE -> "clock_stripe"; ClockFont.CLOCK_STAMP -> "clock_stamp"; ClockFont.ALATSI -> "alatsi"; ClockFont.CAPRIOLA -> "capriola"; ClockFont.FREDERICKA -> "fredericka"; ClockFont.LATO -> "lato"; ClockFont.STARDOS_STENCIL -> "stardos_stencil"; ClockFont.MODAK -> "modak"
            }
            return context.resources.getIdentifier("widget_digital_clock_${size}_$suffix", "layout", context.packageName)
        }
    }
}

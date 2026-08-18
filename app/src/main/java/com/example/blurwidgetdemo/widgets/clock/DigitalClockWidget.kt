package com.example.blurwidgetdemo.widgets.clock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.BatteryManager
import android.util.Log
import android.provider.AlarmClock
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.R

class DigitalClockWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) =
        ids.forEach { updateWidget(context, manager, it, "PROVIDER_UPDATE") }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        updateWidget(context, manager, id, "RESIZE")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, DigitalClockWidget::class.java))
                ids.forEach { updateWidget(context, manager, it, intent.action ?: "BROADCAST") }
            }
        }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val preferences = ClockWidgetPreferences.preferences(context)
        ids.forEach { ClockWidgetPreferences.delete(preferences, it) }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int, reason: String = "UPDATE") {
            val options = manager.getAppWidgetOptions(id)
            val category = ClockWidgetLayout.category(
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40)
            )
            val settings = ClockWidgetPreferences.load(ClockWidgetPreferences.preferences(context), id)
            val layout = ClockLayoutResolver.resolve(category, settings.font)
            val debug = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
            val layoutName = runCatching { context.resources.getResourceEntryName(layout) }.getOrDefault("unknown")
            if (debug) Log.d("ClockFontDebug", "provider id=$id reason=$reason font=${settings.font.preferenceValue} size=$category layout=$layout/$layoutName path=TextClock")
            val views = RemoteViews(context.packageName, layout).apply {
                // Preserve this exact root surface: One UI Home uses it for native wallpaper blur.
                setInt(android.R.id.background, "setBackgroundColor", if (settings.backgroundEnabled) settings.tintColor() else Color.TRANSPARENT)
                setViewVisibility(R.id.clock_date, if (settings.showDate) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.clock_date_above, if (settings.showDate && settings.dateAboveTime) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.clock_date, if (settings.showDate && !settings.dateAboveTime) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.clock_day, if (settings.showDate && settings.showDayOfWeek && category == ClockWidgetLayout.Category.LARGE) View.VISIBLE else View.GONE)
                applyTimeFormat(settings.timeFormat, settings.showAmPm, settings.showSeconds)
                setTextColor(R.id.clock_time, settings.textColor)
                setTextColor(R.id.clock_date_above, settings.textColor)
                setTextColor(R.id.clock_date, settings.textColor)
                setTextColor(R.id.clock_day, settings.textColor)
                setTextColor(R.id.clock_battery_percent, settings.textColor)
                setViewVisibility(R.id.clock_battery, if (settings.showPhoneBattery) View.VISIBLE else View.GONE)
                if (settings.showPhoneBattery) {
                    val percentage = context.getSystemService(BatteryManager::class.java)
                        ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        ?.takeIf { it in 0..100 } ?: 0
                    setTextViewText(R.id.clock_battery_percent, "$percentage%")
                }
                val scale = settings.textScalePercent / 100f
                setTextViewTextSize(R.id.clock_time, android.util.TypedValue.COMPLEX_UNIT_SP, baseTimeTextSize(category) * scale)
                setTextViewTextSize(R.id.clock_date_above, android.util.TypedValue.COMPLEX_UNIT_SP, baseDateTextSize(category) * scale)
                setTextViewTextSize(R.id.clock_date, android.util.TypedValue.COMPLEX_UNIT_SP, baseDateTextSize(category) * scale)
                setTextViewTextSize(R.id.clock_day, android.util.TypedValue.COMPLEX_UNIT_SP, baseDateTextSize(category) * scale)
                setTextViewTextSize(R.id.clock_battery_percent, android.util.TypedValue.COMPLEX_UNIT_SP, 14f * scale)
                setViewVisibility(R.id.clock_debug_marker, View.GONE)
                // Alignment is horizontal only; content stays vertically centred at every size.
                val horizontalGravity = if (!settings.showDate || category == ClockWidgetLayout.Category.COMPACT) Gravity.CENTER_HORIZONTAL else settings.alignment.gravity()
                setInt(R.id.clock_content, "setGravity", Gravity.CENTER_VERTICAL or horizontalGravity)
                setInt(R.id.clock_time, "setGravity", horizontalGravity)
                setInt(R.id.clock_date_above, "setGravity", horizontalGravity)
                setInt(R.id.clock_date, "setGravity", horizontalGravity)
                setInt(R.id.clock_day, "setGravity", horizontalGravity)
                val tapIntent = if (settings.tapAction == ClockTapAction.CLOCK) clockPendingIntent(context) else calendarPendingIntent(context)
                setOnClickPendingIntent(android.R.id.background, tapIntent)
                setOnClickPendingIntent(R.id.clock_content, tapIntent)
                setOnClickPendingIntent(R.id.clock_time, tapIntent)
                setOnClickPendingIntent(R.id.clock_date, tapIntent)
            }
            manager.updateAppWidget(id, views)
            if (debug) Log.d("ClockFontDebug", "updateAppWidget dispatched id=$id layout=$layoutName visibleTime=${R.id.clock_time}")
        }

        private fun RemoteViews.applyTimeFormat(format: ClockTimeFormat, showAmPm: Boolean, showSeconds: Boolean) {
            val format12: CharSequence
            val format24: CharSequence
            val seconds = if (showSeconds) ":ss" else ""
            when (format) {
                ClockTimeFormat.SYSTEM -> { format12 = "h:mm${seconds} a"; format24 = "HH:mm$seconds" }
                ClockTimeFormat.TWELVE_HOUR -> {
                    val pattern = if (showAmPm) "h:mm${seconds} a" else "h:mm$seconds"
                    format12 = pattern; format24 = pattern
                }
                ClockTimeFormat.TWENTY_FOUR_HOUR -> { format12 = "HH:mm$seconds"; format24 = "HH:mm$seconds" }
            }
            setCharSequence(R.id.clock_time, "setFormat12Hour", format12)
            setCharSequence(R.id.clock_time, "setFormat24Hour", format24)
        }

        private fun ClockTextAlignment.gravity(): Int = when (this) {
            ClockTextAlignment.START -> Gravity.START
            ClockTextAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
            ClockTextAlignment.END -> Gravity.END
        }

        private fun baseTimeTextSize(category: ClockWidgetLayout.Category) = when (category) {
            ClockWidgetLayout.Category.COMPACT -> 28f
            ClockWidgetLayout.Category.MEDIUM -> 42f
            ClockWidgetLayout.Category.LARGE -> 52f
        }

        private fun baseDateTextSize(category: ClockWidgetLayout.Category) = when (category) {
            ClockWidgetLayout.Category.COMPACT -> 14f
            ClockWidgetLayout.Category.MEDIUM -> 15f
            ClockWidgetLayout.Category.LARGE -> 16f
        }

        private fun clockPendingIntent(context: Context): PendingIntent {
            val samsungClock = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(SAMSUNG_CLOCK_PACKAGE, SAMSUNG_CLOCK_ACTIVITY))
            return pendingActivity(context, samsungClock, Intent(AlarmClock.ACTION_SHOW_ALARMS))
        }

        private fun calendarPendingIntent(context: Context): PendingIntent = pendingActivity(
            context,
            Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time")), Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        )

        private fun pendingActivity(context: Context, preferred: Intent, fallback: Intent): PendingIntent {
            val resolved = if (preferred.resolveActivity(context.packageManager) != null) preferred else fallback
            return PendingIntent.getActivity(context, 0, resolved.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private const val SAMSUNG_CLOCK_PACKAGE = "com.sec.android.app.clockpackage"
        private const val SAMSUNG_CLOCK_ACTIVITY = "com.sec.android.app.clockpackage.ClockPackage"

    }
}

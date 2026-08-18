package com.example.blurwidgetdemo.widgets.calendar

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.provider.CalendarContract
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetAppearance
import java.text.DateFormatSymbols
import java.util.Calendar

/** A One UI-inspired week view, designed to sit beside the compact next-event widget. */
class CalendarDisplayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { updateWidget(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) = updateWidget(context, manager, id)
    companion object {
        private val labels = intArrayOf(R.id.day_0, R.id.day_1, R.id.day_2, R.id.day_3, R.id.day_4, R.id.day_5, R.id.day_6, R.id.day_7, R.id.day_8, R.id.day_9, R.id.day_10, R.id.day_11, R.id.day_12, R.id.day_13, R.id.day_14, R.id.day_15, R.id.day_16, R.id.day_17, R.id.day_18, R.id.day_19, R.id.day_20)
        fun accentColor(context: Context, id: Int) = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getInt("calendar_accent_$id", 0xFF08B0CE.toInt())
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val today = Calendar.getInstance()
            val week = today.clone() as Calendar
            week.firstDayOfWeek = Calendar.MONDAY
            week.add(Calendar.DAY_OF_YEAR, -(week.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7)
            val color = WidgetAppearance.textColor(prefs, id)
            val accent = accentColor(context, id)
            RemoteViews(context.packageName, R.layout.widget_calendar_display).apply {
                setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(prefs, id))
                setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, id, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                setTextViewText(R.id.month_title, "${DateFormatSymbols().months[today.get(Calendar.MONTH)]} ${today.get(Calendar.YEAR)}")
                setTextColor(R.id.month_title, color); setInt(R.id.month_title, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                labels.forEach { viewId ->
                    val active = week.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                    setTextViewText(viewId, "${DateFormatSymbols().shortWeekdays[week.get(Calendar.DAY_OF_WEEK)].take(1)}\n${week.get(Calendar.DAY_OF_MONTH)}")
                    val weekend = week.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || week.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    setTextColor(viewId, if (active) Color.WHITE else if (weekend) 0xFFFD4B47.toInt() else color)
                    if (active) {
                        setInt(viewId, "setBackgroundResource", R.drawable.calendar_today)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(accent))
                        }
                    } else setInt(viewId, "setBackgroundResource", android.R.color.transparent)
                    week.add(Calendar.DAY_OF_YEAR, 1)
                }
                setFloat(R.id.month_title, "setTextSize", 18f * WidgetAppearance.textScale(prefs, id) / 100f)
            }.also { manager.updateAppWidget(id, it) }
        }
    }
}

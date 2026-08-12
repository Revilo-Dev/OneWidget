package com.example.blurwidgetdemo.widgets.hydration

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetAppearance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HydrationWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { updateWidget(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) = updateWidget(context, manager, id)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD_WATER) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
                prefs.edit().putInt("hydration_$id", (currentCount(prefs, id) + 1) % 9).putString("hydration_day_$id", today()).apply()
                updateWidget(context, AppWidgetManager.getInstance(context), id)
            }
        }
    }
    companion object {
        private const val ACTION_ADD_WATER = "com.example.blurwidgetdemo.action.ADD_WATER"
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val count = currentCount(prefs, id)
            val compact = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
            val add = PendingIntent.getBroadcast(context, id, Intent(context, HydrationWidget::class.java).setAction(ACTION_ADD_WATER).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            RemoteViews(context.packageName, if (compact) R.layout.widget_hydration_compact else R.layout.widget_hydration_large).apply {
                setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(prefs, id))
                setTextViewText(R.id.widget_primary, "$count / 8 glasses")
                setTextViewText(R.id.widget_secondary, "Tap to add a glass")
                setViewVisibility(R.id.widget_secondary, if (compact) View.GONE else View.VISIBLE)
                setTextColor(R.id.widget_primary, WidgetAppearance.textColor(prefs, id))
                setTextColor(R.id.widget_secondary, WidgetAppearance.textColor(prefs, id))
                setInt(R.id.widget_primary, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                setInt(R.id.widget_secondary, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                setFloat(R.id.widget_primary, "setTextSize", 23f * WidgetAppearance.textScale(prefs, id) / 100f)
                setOnClickPendingIntent(android.R.id.background, add)
            }.also { manager.updateAppWidget(id, it) }
        }
        private fun currentCount(prefs: android.content.SharedPreferences, id: Int) =
            if (prefs.getString("hydration_day_$id", null) == today()) prefs.getInt("hydration_$id", 0) else 0
        private fun today() = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }
}

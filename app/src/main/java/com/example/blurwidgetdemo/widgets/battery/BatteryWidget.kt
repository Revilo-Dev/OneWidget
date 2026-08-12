package com.example.blurwidgetdemo.widgets.battery

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetAppearance

class BatteryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) =
        ids.forEach { updateWidget(context, manager, it) }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        updateWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_POWER_CONNECTED || intent.action == Intent.ACTION_POWER_DISCONNECTED) {
            val manager = AppWidgetManager.getInstance(context)
            onUpdate(context, manager, manager.getAppWidgetIds(ComponentName(context, BatteryWidget::class.java)))
        }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val options = manager.getAppWidgetOptions(id)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80)
            val compact = height <= 100 || width < 250
            val level = context.getSystemService(BatteryManager::class.java)
                ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 0..100 }
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            RemoteViews(context.packageName, if (compact) R.layout.widget_battery_compact else R.layout.widget_battery_large).apply {
                setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(prefs, id))
                setTextViewText(R.id.battery_percentage, level?.let { "$it%" } ?: "—")
                setTextViewText(R.id.battery_status, batteryStatus(context, level))
                setTextColor(R.id.battery_percentage, WidgetAppearance.textColor(prefs, id))
                setTextColor(R.id.battery_status, WidgetAppearance.textColor(prefs, id))
                setInt(R.id.battery_percentage, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                setInt(R.id.battery_status, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                setFloat(R.id.battery_percentage, "setTextSize", (if (compact) 30f else 46f) * WidgetAppearance.textScale(prefs, id) / 100f)
                setViewVisibility(R.id.battery_status, if (compact) View.GONE else View.VISIBLE)
            }.also { manager.updateAppWidget(id, it) }
        }

        private fun batteryStatus(context: Context, level: Int?): String {
            if (level == null) return "Battery unavailable"
            val status = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            return when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> "Charging • $level%"
                else -> "Battery • $level%"
            }
        }
    }
}

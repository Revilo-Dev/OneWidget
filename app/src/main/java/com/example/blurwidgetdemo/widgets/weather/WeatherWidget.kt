package com.example.blurwidgetdemo.widgets.weather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetAppearance
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date

/** Current conditions from Open-Meteo, using the phone's approximate last-known location. */
class WeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
        val result = goAsync()
        Thread {
            ids.forEach { refresh(context, manager, it) }
            result.finish()
        }.start()
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = render(context, manager, id)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val result = goAsync()
                Thread { refresh(context, AppWidgetManager.getInstance(context), id); result.finish() }.start()
            }
        }
    }
    companion object {
        private const val ACTION_REFRESH = "com.example.blurwidgetdemo.action.REFRESH_WEATHER"
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) = render(context, manager, id)
        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val compact = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
            val hasLocation = lastLocation(context) != null
            val temp = prefs.getString("weather_temp_$id", null)
            val condition = prefs.getString("weather_condition_$id", null)
            val updated = prefs.getLong("weather_updated_$id", 0)
            val refresh = PendingIntent.getBroadcast(context, id, Intent(context, WeatherWidget::class.java).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            RemoteViews(context.packageName, if (compact) R.layout.widget_weather_compact else R.layout.widget_weather_large).apply {
                setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(prefs, id))
                setOnClickPendingIntent(android.R.id.background, refresh)
                setTextViewText(R.id.weather_title, temp ?: if (hasLocation) "Weather" else "Location needed")
                setTextViewText(R.id.weather_source, condition ?: if (hasLocation) "Updating forecast…" else "Allow location to show weather")
                setTextViewText(R.id.weather_hint, if (updated > 0) "Updated ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(updated))} • Tap to refresh" else "Tap to refresh")
                setViewVisibility(R.id.weather_hint, if (compact) View.GONE else View.VISIBLE)
                val color = WidgetAppearance.textColor(prefs, id)
                setTextColor(R.id.weather_title, color); setTextColor(R.id.weather_source, color); setTextColor(R.id.weather_hint, color)
                setInt(R.id.weather_title, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                setInt(R.id.weather_source, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                setInt(R.id.weather_hint, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                setFloat(R.id.weather_title, "setTextSize", 24f * WidgetAppearance.textScale(prefs, id) / 100f)
            }.also { manager.updateAppWidget(id, it) }
        }
        private fun refresh(context: Context, manager: AppWidgetManager, id: Int) {
            val location = lastLocation(context) ?: return
            runCatching {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current_weather=true&temperature_unit=celsius")
                val connection = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 8_000; readTimeout = 8_000 }
                val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }.getJSONObject("current_weather")
                val temp = "${json.getDouble("temperature").toInt()}°C"
                val condition = condition(json.getInt("weathercode"))
                context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit()
                    .putString("weather_temp_$id", temp).putString("weather_condition_$id", condition).putLong("weather_updated_$id", System.currentTimeMillis()).apply()
            }
            render(context, manager, id)
        }
        @Suppress("MissingPermission") private fun lastLocation(context: Context): Location? = runCatching {
            val manager = context.getSystemService(LocationManager::class.java)
            listOf(LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                .mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        }.getOrNull()
        private fun condition(code: Int) = when (code) { 0 -> "Clear sky"; 1, 2 -> "Partly cloudy"; 3 -> "Overcast"; 45, 48 -> "Foggy"; 51, 53, 55, 56, 57 -> "Drizzle"; 61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"; 71, 73, 75, 77, 85, 86 -> "Snow"; 95, 96, 99 -> "Thunderstorm"; else -> "Current conditions" }
    }
}

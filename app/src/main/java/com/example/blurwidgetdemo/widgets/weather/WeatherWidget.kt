package com.example.blurwidgetdemo.widgets.weather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetAppearance
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Current local conditions from Open-Meteo. Tapping the widget opens Samsung Weather. */
class WeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
        val result = goAsync()
        Thread { ids.forEach { refresh(context, manager, it) }; result.finish() }.start()
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = render(context, manager, id)

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            render(context, manager, id)
            Thread { refresh(context, manager, id) }.start()
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val compact = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
            val hasLocation = lastLocation(context) != null
            val tempC = prefs.getFloat("weather_temp_c_$id", Float.NaN)
            val unit = prefs.getString("weather_unit_$id", "C") ?: "C"
            val showLocation = prefs.getBoolean("weather_show_location_$id", true)
            val condition = prefs.getString("weather_condition_$id", null)
            val location = prefs.getString("weather_location_$id", null)
            val weatherCode = prefs.getInt("weather_code_$id", -1)
            val temp = if (tempC.isNaN()) if (hasLocation) "Weather" else "Location needed" else formatTemperature(tempC, unit)
            val source = condition ?: if (hasLocation) "Updating forecast…" else "Allow location to show weather"
            RemoteViews(context.packageName, if (compact) R.layout.widget_weather_compact else R.layout.widget_weather_large).apply {
                BlurWidget.applySurfaceTint(this, prefs, id)
                setOnClickPendingIntent(android.R.id.background, samsungWeatherIntent(context, id))
                setTextViewText(R.id.weather_temperature, temp)
                setTextViewText(R.id.weather_condition, source)
                setTextViewText(R.id.weather_location, location ?: if (hasLocation) "Current location" else "Location access required")
                setViewVisibility(R.id.weather_location, if (showLocation) View.VISIBLE else View.GONE)
                setImageViewResource(R.id.weather_icon, iconFor(weatherCode))
                val color = WidgetAppearance.textColor(prefs, id)
                intArrayOf(R.id.weather_temperature, R.id.weather_condition, R.id.weather_location).forEach { setTextColor(it, color) }
                val centred = WidgetAppearance.alignment(prefs, id) == WidgetAppearance.Alignment.CENTRE
                val textScale = WidgetAppearance.textScale(prefs, id) / 100f
                setFloat(R.id.weather_temperature, "setTextSize", (if (centred) 32f else 26f) * textScale)
                val gravity = if (centred) Gravity.CENTER else Gravity.START or Gravity.CENTER_VERTICAL
                intArrayOf(R.id.weather_temperature, R.id.weather_condition, R.id.weather_location).forEach { setInt(it, "setGravity", gravity) }
            }.also { manager.updateAppWidget(id, it) }
        }

        private fun refresh(context: Context, manager: AppWidgetManager, id: Int) {
            val location = lastLocation(context)
            if (location != null) fetchForecast(context, manager, id, location)
            else requestCurrentLocation(context) { current ->
                if (current != null) Thread { fetchForecast(context, manager, id, current) }.start()
                else render(context, manager, id)
            }
        }

        private fun fetchForecast(context: Context, manager: AppWidgetManager, id: Int, location: Location) {
            runCatching {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current=temperature_2m,weather_code&temperature_unit=celsius")
                val connection = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 8_000; readTimeout = 8_000 }
                val weather = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }.getJSONObject("current")
                val code = weather.getInt("weather_code")
                context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit()
                    .putFloat("weather_temp_c_$id", weather.getDouble("temperature_2m").toFloat())
                    .putString("weather_condition_$id", condition(code))
                    .putInt("weather_code_$id", code)
                    .putString("weather_location_$id", locality(context, location))
                    .apply()
            }
            render(context, manager, id)
        }

        private fun samsungWeatherIntent(context: Context, id: Int): PendingIntent {
            val samsungWeather = Intent(Intent.ACTION_MAIN)
                .addCategory("android.intent.category.APP_WEATHER")
                .setPackage("com.sec.android.daemonapp")
            val target = if (samsungWeather.resolveActivity(context.packageManager) != null) samsungWeather
            else Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://weather.com"))
            return PendingIntent.getActivity(context, id, target, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        @Suppress("DEPRECATION") private fun locality(context: Context, location: Location): String = runCatching {
            Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()?.locality ?: "Current location"
        }.getOrDefault("Current location")
        @Suppress("MissingPermission") private fun lastLocation(context: Context): Location? = runCatching {
            if (!hasLocationPermission(context)) return null
            val manager = context.getSystemService(LocationManager::class.java)
            listOf(LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        }.getOrNull()
        @Suppress("MissingPermission") private fun requestCurrentLocation(context: Context, callback: (Location?) -> Unit) {
            if (!hasLocationPermission(context)) {
                callback(null)
                return
            }
            val manager = context.getSystemService(LocationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, context.mainExecutor) { location ->
                    if (location != null) callback(location)
                    else manager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, context.mainExecutor, callback)
                }
            } else callback(lastLocation(context))
        }
        private fun hasLocationPermission(context: Context) = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        private fun formatTemperature(celsius: Float, unit: String): String = "${(if (unit == "F") celsius * 9 / 5 + 32 else celsius).toInt()}°$unit"
        private fun condition(code: Int) = when (code) { 0 -> "Clear sky"; 1, 2 -> "Partly cloudy"; 3 -> "Overcast"; 45, 48 -> "Foggy"; 51, 53, 55, 56, 57 -> "Drizzle"; 61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"; 71, 73, 75, 77, 85, 86 -> "Snow"; 95, 96, 99 -> "Thunderstorm"; else -> "Current conditions" }
        private fun iconFor(code: Int) = when (code) { 0 -> R.drawable.weather_sunny; 1, 2 -> R.drawable.weather_partly_sunny; 3 -> R.drawable.weather_cloudy; 45, 48 -> R.drawable.weather_fog; 51, 53, 55, 56, 57 -> R.drawable.weather_shower; 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.weather_rain; 71, 73, 75, 77, 85, 86 -> R.drawable.weather_snow; 95, 96, 99 -> R.drawable.weather_thunder_storm; else -> R.drawable.weather_loading }
    }
}

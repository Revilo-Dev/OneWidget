package dev.revilodev.onewidget.widgets.weather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.widget.RemoteViews
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.widgets.WidgetAppearance
import dev.revilodev.onewidget.studio.universal.UniversalElementContent
import dev.revilodev.onewidget.studio.universal.UniversalProductionWidgetRenderer
import dev.revilodev.onewidget.studio.universal.UniversalWidgetActions
import dev.revilodev.onewidget.studio.universal.UniversalWidgetContent
import dev.revilodev.onewidget.studio.universal.UniversalWidgetIds
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Current local conditions from Open-Meteo. Tapping the widget opens Samsung Weather. */
open class WeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
        val result = goAsync()
        Thread {
            try {
                ids.forEach { refresh(context, manager, it) }
            } finally {
                result.finish()
            }
        }.start()
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = render(context, manager, id)
    override fun onDeleted(context: Context, ids: IntArray) {
        dev.revilodev.onewidget.widgets.WidgetAppearance.delete(context, ids)

        UniversalProductionWidgetRenderer.deleteModes(context, UniversalWidgetIds.WEATHER, ids)
    }

    companion object {
        private val fetching = java.util.concurrent.atomic.AtomicBoolean(false)

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            render(context, manager, id)
            Thread { refresh(context, manager, id) }.start()
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val compact = dev.revilodev.onewidget.widgets.WidgetInitialSizeSpec.renderOptions(manager, id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
            val hasLocation = lastLocation(context) != null
            val tempC = prefs.getFloat("weather_temp_c", prefs.getFloat("weather_temp_c_$id", Float.NaN))
            val unit = prefs.getString("weather_unit_$id", "C") ?: "C"
            val showLocation = prefs.getBoolean("weather_show_location_$id", true)
            val condition = prefs.getString("weather_condition", prefs.getString("weather_condition_$id", null))
            val location = prefs.getString("weather_location", prefs.getString("weather_location_$id", null))
            val weatherCode = prefs.getInt("weather_code", prefs.getInt("weather_code_$id", -1))
            val temp = if (tempC.isNaN()) if (hasLocation) "Weather" else "Location needed" else formatTemperature(tempC, unit)
            val outdated = System.currentTimeMillis() - prefs.getLong("weather_updated_at", 0) > 2 * 60 * 60_000L
            val source = if (condition != null) condition + if (outdated) " · Outdated" else ""
                else if (hasLocation) "Forecast unavailable" else "Allow location to show weather"
            val locationText = location ?: if (hasLocation) "Current location" else "Location access required"
            val rootAction = samsungWeatherIntent(context, id)
            val promoted = UniversalProductionWidgetRenderer.buildIfPromoted(
                context,
                manager,
                id,
                UniversalWidgetIds.WEATHER,
                UniversalWidgetContent(
                    mapOf(
                        "universal.icon" to UniversalElementContent(imageResId = iconFor(weatherCode)),
                        "universal.temperature" to UniversalElementContent(text = temp),
                        "universal.location" to UniversalElementContent(text = locationText, visible = showLocation),
                        "universal.condition" to UniversalElementContent(text = source)
                    )
                ),
                UniversalWidgetActions(root = rootAction)
            )
            val views = promoted ?: RemoteViews(
                context.packageName,
                if (compact) R.layout.widget_weather_compact else R.layout.widget_weather_large
            ).apply {
                BlurWidget.applySurfaceTint(this, prefs, id)
                setOnClickPendingIntent(android.R.id.background, rootAction)
                setTextViewText(R.id.weather_temperature, temp)
                setTextViewText(R.id.weather_condition, source)
                setTextViewText(R.id.weather_location, locationText)
                setViewVisibility(R.id.weather_location, if (showLocation) View.VISIBLE else View.GONE)
                setImageViewResource(R.id.weather_icon, iconFor(weatherCode))
                val color = WidgetAppearance.textColor(prefs, id)
                intArrayOf(R.id.weather_temperature, R.id.weather_condition, R.id.weather_location).forEach { setTextColor(it, color) }
                val centred = WidgetAppearance.alignment(prefs, id) == WidgetAppearance.Alignment.CENTRE
                val textScale = WidgetAppearance.textScale(prefs, id) / 100f
                setFloat(R.id.weather_temperature, "setTextSize", (if (centred) 32f else 26f) * textScale)
                val gravity = if (centred) Gravity.CENTER else Gravity.START or Gravity.CENTER_VERTICAL
                intArrayOf(R.id.weather_temperature, R.id.weather_condition, R.id.weather_location).forEach { setInt(it, "setGravity", gravity) }
            }
            dev.revilodev.onewidget.widgets.WidgetSurface.update(context, manager, id, views)
        }

        private fun refresh(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val age = System.currentTimeMillis() - prefs.getLong("weather_updated_at", 0)
            if (age in 0 until 30 * 60_000L) return
            val attempted = System.currentTimeMillis() - prefs.getLong("weather_attempted_at", 0)
            if (attempted in 0 until 5 * 60_000L || !fetching.compareAndSet(false, true)) return
            prefs.edit().putLong("weather_attempted_at", System.currentTimeMillis()).apply()
            try {
                val location = currentLocation(context) ?: lastLocation(context)
                if (location != null) fetchForecast(context, manager, id, location)
            } finally {
                fetching.set(false)
            }
        }

        private fun fetchForecast(context: Context, manager: AppWidgetManager, id: Int, location: Location) {
            runCatching {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current=temperature_2m,weather_code&temperature_unit=celsius")
                val connection = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 3_000; readTimeout = 3_000 }
                try {
                    val weather = connection.inputStream.bufferedReader().use {
                        JSONObject(it.readText())
                    }.getJSONObject("current")
                    val code = weather.getInt("weather_code")
                    context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit()
                        .putLong("weather_updated_at", System.currentTimeMillis())
                        .putFloat("weather_temp_c", weather.getDouble("temperature_2m").toFloat())
                        .putString("weather_condition", condition(code))
                        .putInt("weather_code", code)
                        .putString("weather_location", prefsLocality(context))
                        .apply()
                } finally {
                    connection.disconnect()
                }
            }
            manager.installedProviders.filter { it.provider.packageName == context.packageName && it.provider.className.contains("Weather") }.forEach { provider ->
                manager.getAppWidgetIds(provider.provider).forEach { render(context, manager, it) }
            }
        }

        private fun samsungWeatherIntent(context: Context, id: Int): PendingIntent {
            val samsungWeather = Intent(Intent.ACTION_MAIN)
                .addCategory("android.intent.category.APP_WEATHER")
                .setPackage("com.sec.android.daemonapp")
            val target = if (samsungWeather.resolveActivity(context.packageManager) != null) samsungWeather
            else Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://weather.com"))
            return PendingIntent.getActivity(context, id, target, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun prefsLocality(context: Context): String = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            .getString("weather_location", "Current location") ?: "Current location"

        @Suppress("MissingPermission") private fun lastLocation(context: Context): Location? = runCatching {
            if (!hasLocationPermission(context)) return null
            val manager = context.getSystemService(LocationManager::class.java)
            listOf(LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        }.getOrNull()
        @Suppress("MissingPermission")
        private fun currentLocation(context: Context): Location? {
            if (Build.VERSION.SDK_INT < 30 || !hasLocationPermission(context)) return null
            val manager = context.getSystemService(LocationManager::class.java)
            val cached = lastLocation(context)
            if (cached != null && System.currentTimeMillis() - cached.time < 60 * 60_000L) return cached
            val signal = android.os.CancellationSignal()
            val latch = java.util.concurrent.CountDownLatch(1)
            var location: Location? = null
            return try {
                val provider = if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { current -> location = current; latch.countDown() }
                latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                location
            } catch (_: Exception) {
                cached
            } finally {
                signal.cancel()
            }
        }
        private fun hasLocationPermission(context: Context) = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        private fun formatTemperature(celsius: Float, unit: String): String = "${(if (unit == "F") celsius * 9 / 5 + 32 else celsius).toInt()}°$unit"
        private fun condition(code: Int) = when (code) { 0 -> "Clear sky"; 1, 2 -> "Partly cloudy"; 3 -> "Overcast"; 45, 48 -> "Foggy"; 51, 53, 55, 56, 57 -> "Drizzle"; 61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"; 71, 73, 75, 77, 85, 86 -> "Snow"; 95, 96, 99 -> "Thunderstorm"; else -> "Current conditions" }
        private fun iconFor(code: Int) = when (code) { 0 -> R.drawable.weather_sunny; 1, 2 -> R.drawable.weather_partly_sunny; 3 -> R.drawable.weather_cloudy; 45, 48 -> R.drawable.weather_fog; 51, 53, 55, 56, 57 -> R.drawable.weather_shower; 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.weather_rain; 71, 73, 75, 77, 85, 86 -> R.drawable.weather_snow; 95, 96, 99 -> R.drawable.weather_thunder_storm; else -> R.drawable.weather_loading }
    }
}

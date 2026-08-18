package com.example.blurwidgetdemo

import android.appwidget.AppWidgetManager
import android.Manifest
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.graphics.Typeface
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.app.AppCompatActivity
import com.example.blurwidgetdemo.widgets.clock.DigitalClockWidget
import com.example.blurwidgetdemo.widgets.battery.BatteryWidget
import com.example.blurwidgetdemo.widgets.weather.WeatherWidget
import com.example.blurwidgetdemo.widgets.calendar.CalendarWidget
import com.example.blurwidgetdemo.widgets.calendar.CalendarDisplayWidget
import com.example.blurwidgetdemo.widgets.storage.StorageWidget
import com.example.blurwidgetdemo.widgets.hydration.HydrationWidget
import com.example.blurwidgetdemo.widgets.extra.DataToggleWidget
import com.example.blurwidgetdemo.widgets.extra.DarkModeToggleWidget
import com.example.blurwidgetdemo.widgets.extra.WifiToggleWidget
import com.example.blurwidgetdemo.widgets.extra.DoNotDisturbToggleWidget
import com.example.blurwidgetdemo.widgets.extra.BluetoothToggleWidget
import com.example.blurwidgetdemo.widgets.extra.LocationToggleWidget
import com.example.blurwidgetdemo.widgets.extra.HotspotToggleWidget
import com.example.blurwidgetdemo.widgets.extra.CameraToggleWidget
import com.example.blurwidgetdemo.widgets.extra.TorchToggleWidget
import com.example.blurwidgetdemo.widgets.extra.GoogleSearchWidget
import com.example.blurwidgetdemo.widgets.extra.OpenAppWidget
import com.example.blurwidgetdemo.widgets.extra.TextWidget
import com.example.blurwidgetdemo.widgets.extra.StepCounterWidget
import com.example.blurwidgetdemo.widgets.extra.AlarmWidget
import com.example.blurwidgetdemo.widgets.extra.MediaPlayerWidget
import dev.oneuiproject.oneui.widget.OnboardingTipsItemView

class MainActivity : AppCompatActivity() {
    private var pendingWidget: Class<out android.appwidget.AppWidgetProvider>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        findViewById<TextView>(R.id.add_widget_button).setOnClickListener {
            requestWidgetPin(BlurWidget::class.java)
        }
        findViewById<TextView>(R.id.add_clock_widget_button).setOnClickListener {
            requestWidgetPin(DigitalClockWidget::class.java)
        }
        findViewById<TextView>(R.id.add_battery_widget_button).setOnClickListener {
            requestWidgetPin(BatteryWidget::class.java)
        }
        findViewById<TextView>(R.id.add_weather_widget_button).setOnClickListener { requestWeatherWidgetPin() }
        findViewById<TextView>(R.id.add_calendar_widget_button).setOnClickListener { requestCalendarWidgetPin() }
        findViewById<TextView>(R.id.add_calendar_display_widget_button).setOnClickListener { requestWidgetPin(CalendarDisplayWidget::class.java) }
        findViewById<TextView>(R.id.add_storage_widget_button).setOnClickListener { requestWidgetPin(StorageWidget::class.java) }
        findViewById<TextView>(R.id.add_hydration_widget_button).setOnClickListener { requestWidgetPin(HydrationWidget::class.java) }
        findViewById<android.view.View>(R.id.onboarding_footer).visibility = android.view.View.GONE
        addWidgetCatalogue()
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        findViewById<TextView>(R.id.home_build_version).text =
            "${getString(R.string.home_build_version)}: ${packageInfo.versionName ?: "unknown"} (${packageInfo.versionCode})"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_about) {
            startActivity(Intent(this, AboutActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    private fun requestWidgetPin(provider: Class<out android.appwidget.AppWidgetProvider>) {
        val appWidgetManager = getSystemService(AppWidgetManager::class.java)
        val status = findViewById<TextView>(R.id.add_widget_status)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            status.text = getString(R.string.add_widget_not_supported)
            return
        }

        appWidgetManager.requestPinAppWidget(
            ComponentName(this, provider),
            null,
            null
        )
        status.text = getString(R.string.add_widget_requested)
    }

    private data class WidgetEntry(val title: String, val sample: String, val icon: Int, val provider: Class<out android.appwidget.AppWidgetProvider>)

    private fun addWidgetCatalogue() {
        val container = findViewById<LinearLayout>(R.id.onboarding_tips_container)
        container.removeAllViews()
        addCatalogueSection(container, "Widgets", listOf(
            WidgetEntry("Glass", "Blur background", R.drawable.ic_home_24, BlurWidget::class.java), WidgetEntry("Digital clock", "12:45", R.drawable.time, DigitalClockWidget::class.java),
            WidgetEntry("Battery", "Phone 85%", R.drawable.battery, BatteryWidget::class.java), WidgetEntry("Weather", "24° • Sunny", R.drawable.weather_sunny, WeatherWidget::class.java),
            WidgetEntry("Next event", "Team catch-up", R.drawable.calendar_today, CalendarWidget::class.java), WidgetEntry("Calendar", "August 2026", R.drawable.calendar_today, CalendarDisplayWidget::class.java),
            WidgetEntry("Storage", "Storage 62%", R.drawable.manage_storage, StorageWidget::class.java), WidgetEntry("Hydration", "4 / 8 glasses", R.drawable.water_drop, HydrationWidget::class.java),
            WidgetEntry("Steps", "6,524 steps", R.drawable.running, StepCounterWidget::class.java), WidgetEntry("Text", "Your text", R.drawable.text_check_on, TextWidget::class.java),
            WidgetEntry("Alarm", "Next alarm", R.drawable.time, AlarmWidget::class.java), WidgetEntry("Media player", "Now playing", R.drawable.music_alt, MediaPlayerWidget::class.java),
            WidgetEntry("Google Search", "Search", R.drawable.google, GoogleSearchWidget::class.java), WidgetEntry("Open app", "Choose an app", R.drawable.phone, OpenAppWidget::class.java)
        ))
        addCatalogueSection(container, "Toggle widgets", listOf(
            WidgetEntry("Mobile data", "", R.drawable.network_storage, DataToggleWidget::class.java), WidgetEntry("Dark mode", "", R.drawable.dark, DarkModeToggleWidget::class.java),
            WidgetEntry("Wi-Fi", "", R.drawable.wifi_2, WifiToggleWidget::class.java), WidgetEntry("Do not disturb", "", R.drawable.do_not_disturb, DoNotDisturbToggleWidget::class.java),
            WidgetEntry("Bluetooth", "", R.drawable.devices, BluetoothToggleWidget::class.java), WidgetEntry("Location", "", R.drawable.location, LocationToggleWidget::class.java),
            WidgetEntry("Hotspot", "", R.drawable.wifi_2, HotspotToggleWidget::class.java), WidgetEntry("Camera", "", R.drawable.image, CameraToggleWidget::class.java), WidgetEntry("Torch", "", R.drawable.flashlight, TorchToggleWidget::class.java)
        ))
    }

    private fun addCatalogueSection(container: LinearLayout, title: String, entries: List<WidgetEntry>) {
        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = true; setPadding(0, 0, 0, dp(12)) }
        container.addView(TextView(this).apply { text = "$title  ▾"; textSize = 20f; setPadding(0, dp(12), 0, dp(6)); setOnClickListener { grid.visibility = if (grid.visibility == android.view.View.VISIBLE) android.view.View.GONE else android.view.View.VISIBLE } })
        entries.forEach { entry -> grid.addView(widgetTile(entry), GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply { width = 0; height = dp(148); setMargins(dp(3), dp(3), dp(3), dp(3)) }) }
        container.addView(grid, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private fun widgetTile(entry: WidgetEntry): android.view.View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = getDrawable(R.drawable.widget_clock_background); setPadding(dp(10), dp(8), dp(10), dp(8)); isClickable = true; isFocusable = true
        addView(ImageView(this@MainActivity).apply { setImageResource(entry.icon); contentDescription = null }, LinearLayout.LayoutParams(dp(40), dp(40)))
        addView(TextView(this@MainActivity).apply { text = entry.sample; gravity = Gravity.CENTER; textSize = 13f; maxLines = 2 }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        addView(TextView(this@MainActivity).apply { text = entry.title; gravity = Gravity.CENTER; textSize = 14f; setTypeface(typeface, Typeface.BOLD) }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setOnClickListener { when (entry.provider) { WeatherWidget::class.java -> requestWeatherWidgetPin(); CalendarWidget::class.java -> requestCalendarWidgetPin(); else -> requestWidgetPin(entry.provider) } }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun requestCalendarWidgetPin() {
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            requestWidgetPin(CalendarWidget::class.java)
        } else {
            pendingWidget = CalendarWidget::class.java
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), CALENDAR_PERMISSION_REQUEST)
        }
    }

    private fun requestWeatherWidgetPin() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requestWidgetPin(WeatherWidget::class.java)
        } else {
            pendingWidget = WeatherWidget::class.java
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), WEATHER_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == CALENDAR_PERMISSION_REQUEST || requestCode == WEATHER_PERMISSION_REQUEST) && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            pendingWidget?.let(::requestWidgetPin)
        }
        pendingWidget = null
    }

    private companion object {
        const val CALENDAR_PERMISSION_REQUEST = 41
        const val WEATHER_PERMISSION_REQUEST = 42
    }
}

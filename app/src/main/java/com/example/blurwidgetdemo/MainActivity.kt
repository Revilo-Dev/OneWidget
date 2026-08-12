package com.example.blurwidgetdemo

import android.appwidget.AppWidgetManager
import android.Manifest
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.blurwidgetdemo.widgets.clock.DigitalClockWidget
import com.example.blurwidgetdemo.widgets.battery.BatteryWidget
import com.example.blurwidgetdemo.widgets.weather.WeatherWidget
import com.example.blurwidgetdemo.widgets.calendar.CalendarWidget
import com.example.blurwidgetdemo.widgets.calendar.CalendarDisplayWidget
import com.example.blurwidgetdemo.widgets.storage.StorageWidget
import com.example.blurwidgetdemo.widgets.hydration.HydrationWidget
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

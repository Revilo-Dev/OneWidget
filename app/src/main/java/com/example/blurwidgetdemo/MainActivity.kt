package com.example.blurwidgetdemo

import android.appwidget.AppWidgetManager
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
import dev.oneuiproject.oneui.widget.OnboardingTipsItemView

class MainActivity : AppCompatActivity() {
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
        findViewById<TextView>(R.id.home_build_version).text = getString(
            R.string.home_build_version, packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown", packageManager.getPackageInfo(packageName, 0).versionCode
        )
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
}

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
import dev.oneuiproject.oneui.widget.OnboardingTipsItemView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initTips()
        findViewById<TextView>(R.id.add_widget_button).setOnClickListener {
            requestWidgetPin(BlurWidget::class.java)
        }
        findViewById<TextView>(R.id.add_clock_widget_button).setOnClickListener {
            requestWidgetPin(DigitalClockWidget::class.java)
        }
        findViewById<TextView>(R.id.home_build_version).text = getString(
            R.string.home_build_version, packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown", packageManager.getPackageInfo(packageName, 0).longVersionCode
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

    private fun initTips() {
        val tips = listOf(
            Triple(R.drawable.ic_tint_24, R.string.onboarding_tint_title, R.string.onboarding_tint_summary),
            Triple(R.drawable.ic_opacity_24, R.string.onboarding_opacity_title, R.string.onboarding_opacity_summary),
            Triple(R.drawable.ic_code_24, R.string.onboarding_implement_title, R.string.onboarding_implement_summary)
        )
        val container = findViewById<LinearLayout>(R.id.onboarding_tips_container)
        tips.forEach { (icon, title, summary) ->
            OnboardingTipsItemView(this).apply {
                setIcon(icon)
                this.title = getString(title)
                this.summary = getString(summary)
                container.addView(this, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
        }
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

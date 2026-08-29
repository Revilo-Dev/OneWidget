package com.example.blurwidgetdemo

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SeslSeekBar
import androidx.picker3.app.SeslColorPickerDialog
import com.example.blurwidgetdemo.widgets.battery.BatteryWidget
import com.example.blurwidgetdemo.widgets.calendar.CalendarDisplayWidget
import com.example.blurwidgetdemo.widgets.calendar.CalendarWidget
import com.example.blurwidgetdemo.widgets.clock.DigitalClockWidget
import com.example.blurwidgetdemo.widgets.extra.AlarmWidget
import com.example.blurwidgetdemo.widgets.extra.CounterWidget
import com.example.blurwidgetdemo.widgets.extra.GoogleSearchWidget
import com.example.blurwidgetdemo.widgets.extra.MediaPlayerWidget
import com.example.blurwidgetdemo.widgets.extra.OpenAppWidget
import com.example.blurwidgetdemo.widgets.extra.SamsungBrowserSearchWidget
import com.example.blurwidgetdemo.widgets.extra.StepCounterWidget
import com.example.blurwidgetdemo.widgets.extra.TextWidget
import com.example.blurwidgetdemo.widgets.extra.ToggleWidget
import com.example.blurwidgetdemo.widgets.extra.YouTubeSearchWidget
import com.example.blurwidgetdemo.widgets.hydration.HydrationWidget
import com.example.blurwidgetdemo.widgets.storage.StorageWidget
import com.example.blurwidgetdemo.widgets.weather.WeatherWidget
import dev.oneuiproject.oneui.widget.CardItemView
import dev.oneuiproject.oneui.widget.SwitchItemView

class AppearanceSettingsActivity : AppCompatActivity() {
    private lateinit var prefs: android.content.SharedPreferences
    private var defaultTint = BlurWidget.Companion.TintDefaults(
        BlurWidget.DEFAULT_TINT_HUE,
        BlurWidget.DEFAULT_TINT_SATURATION,
        BlurWidget.DEFAULT_TINT_VALUE,
        BlurWidget.DEFAULT_TINT_ALPHA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appearance_settings)
        prefs = getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE)
        setupDefaultAppearance()
        findViewById<SwitchItemView>(R.id.global_borders).apply {
            isChecked = prefs.getBoolean("global_widget_borders", true)
            onCheckedChangedListener = { _, checked -> prefs.edit().putBoolean("global_widget_borders", checked).apply(); refreshWidgets() }
        }
        findViewById<SwitchItemView>(R.id.global_gradients).apply {
            isChecked = prefs.getBoolean("global_widget_gradients", true)
            onCheckedChangedListener = { _, checked -> prefs.edit().putBoolean("global_widget_gradients", checked).apply(); refreshWidgets() }
        }
    }

    private fun setupDefaultAppearance() {
        defaultTint = BlurWidget.defaultTint(prefs)
        val tintItem = findViewById<CardItemView>(R.id.default_widget_tint)
        val blurItem = findViewById<CardItemView>(R.id.default_widget_blur)
        val blurSlider = findViewById<SeslSeekBar>(R.id.default_widget_blur_slider)
        fun refreshSummaries() {
            tintItem.summary = "#${BlurWidget.tintColor(defaultTint.hue, defaultTint.saturation, defaultTint.value, defaultTint.alpha).toUInt().toString(16).uppercase().padStart(8, '0')}"
            blurItem.summary = "${(defaultTint.alpha * 100 / 254)}% opacity"
            blurSlider.progress = defaultTint.alpha - 1
        }
        tintItem.setOnClickListener {
            SeslColorPickerDialog(this, { color ->
                val hsv = FloatArray(3)
                Color.colorToHSV(color, hsv)
                defaultTint = defaultTint.copy(hue = hsv[0], saturation = hsv[1], value = hsv[2])
                BlurWidget.saveDefaultTint(prefs, defaultTint)
                refreshSummaries()
                refreshWidgets()
            }, BlurWidget.tintColor(defaultTint.hue, defaultTint.saturation, defaultTint.value, defaultTint.alpha), intArrayOf(), true).show()
        }
        blurSlider.max = 253
        blurSlider.setOnSeekBarChangeListener(object : SeslSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeslSeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                defaultTint = defaultTint.copy(alpha = progress + 1)
                BlurWidget.saveDefaultTint(prefs, defaultTint)
                blurItem.summary = "${(defaultTint.alpha * 100 / 254)}% opacity"
                refreshWidgets()
            }
            override fun onStartTrackingTouch(seekBar: SeslSeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeslSeekBar) = Unit
        })
        refreshSummaries()
    }

    private fun refreshWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        widgetProviders.forEach { provider ->
            val ids = manager.getAppWidgetIds(ComponentName(this, provider))
            if (ids.isNotEmpty()) {
                sendBroadcast(android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(ComponentName(this, provider))
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
            }
        }
    }

    private companion object {
        val widgetProviders: List<Class<out AppWidgetProvider>> = listOf(
            BlurWidget::class.java, DigitalClockWidget::class.java, BatteryWidget::class.java,
            WeatherWidget::class.java, CalendarWidget::class.java, CalendarDisplayWidget::class.java,
            StorageWidget::class.java, HydrationWidget::class.java, GoogleSearchWidget::class.java,
            SamsungBrowserSearchWidget::class.java, YouTubeSearchWidget::class.java, ToggleWidget::class.java,
            StepCounterWidget::class.java, TextWidget::class.java, OpenAppWidget::class.java,
            AlarmWidget::class.java, CounterWidget::class.java, MediaPlayerWidget::class.java
        )
    }
}

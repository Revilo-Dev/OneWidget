package com.example.blurwidgetdemo

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.blurwidgetdemo.widgets.battery.BatteryWidget
import com.example.blurwidgetdemo.widgets.storage.StorageWidget
import dev.oneuiproject.oneui.widget.SwitchItemView

class AppearanceSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appearance_settings)
        val prefs = getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE)
        findViewById<SwitchItemView>(R.id.global_borders).apply {
            isChecked = prefs.getBoolean("global_widget_borders", true)
            onCheckedChangedListener = { _, checked -> prefs.edit().putBoolean("global_widget_borders", checked).apply(); refreshWidgets() }
        }
        findViewById<SwitchItemView>(R.id.global_gradients).apply {
            isChecked = prefs.getBoolean("global_widget_gradients", true)
            onCheckedChangedListener = { _, checked -> prefs.edit().putBoolean("global_widget_gradients", checked).apply(); refreshWidgets() }
        }
    }

    private fun refreshWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        manager.getAppWidgetIds(ComponentName(this, BatteryWidget::class.java)).forEach { BatteryWidget.updateWidget(this, manager, it) }
        manager.getAppWidgetIds(ComponentName(this, StorageWidget::class.java)).forEach { StorageWidget.updateWidget(this, manager, it) }
    }
}

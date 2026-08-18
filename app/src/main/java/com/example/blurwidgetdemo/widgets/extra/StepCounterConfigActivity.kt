package com.example.blurwidgetdemo.widgets.extra

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StepCounterConfigActivity : AppCompatActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) finishConfiguration() else requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), 1)
    }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) { super.onRequestPermissionsResult(code, permissions, results); if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) finishConfiguration() else finish() }
    private fun finishConfiguration() { StepCounterWidget().onUpdate(this, AppWidgetManager.getInstance(this), intArrayOf(widgetId)); setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)); finish() }
}

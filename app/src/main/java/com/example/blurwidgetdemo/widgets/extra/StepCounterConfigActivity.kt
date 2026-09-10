package dev.revilodev.onewidget.widgets.extra

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.revilodev.onewidget.R

class StepCounterConfigActivity : AppCompatActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        ) {
            finishConfiguration()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), PERMISSION_REQUEST)
        }
    }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (results.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.steps_permission_denied, Toast.LENGTH_LONG).show()
        }
        finishConfiguration()
    }
    private fun finishConfiguration() {
        dev.revilodev.onewidget.studio.universal.UniversalProductionWidgetRenderer
            .initializeConfiguredWidget(this, dev.revilodev.onewidget.studio.universal.UniversalWidgetIds.STEP_COUNTER, widgetId)
        StepCounterWidget().onUpdate(this, AppWidgetManager.getInstance(this), intArrayOf(widgetId))
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }

    private companion object {
        const val PERMISSION_REQUEST = 1
    }
}

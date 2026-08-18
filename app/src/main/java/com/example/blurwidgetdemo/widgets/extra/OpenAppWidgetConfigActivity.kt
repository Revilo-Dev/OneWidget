package com.example.blurwidgetdemo.widgets.extra

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** Lets the user choose the icon and label shown by an Open app widget. */
class OpenAppWidgetConfigActivity : AppCompatActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = packageManager.queryIntentActivities(launcher, 0).sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        AlertDialog.Builder(this).setTitle("Choose an app").setItems(activities.map { it.loadLabel(packageManager) }.toTypedArray()) { _, which ->
            val selected = activities[which]
            getSharedPreferences("widget_prefs", MODE_PRIVATE).edit()
                .putString("open_app_package_$widgetId", selected.activityInfo.packageName)
                .putString("open_app_class_$widgetId", selected.activityInfo.name)
                .putString("open_app_label_$widgetId", selected.loadLabel(packageManager).toString())
                .apply()
            OpenAppWidget().onUpdate(this, AppWidgetManager.getInstance(this), intArrayOf(widgetId))
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }.setOnCancelListener { finish() }.show()
    }
}

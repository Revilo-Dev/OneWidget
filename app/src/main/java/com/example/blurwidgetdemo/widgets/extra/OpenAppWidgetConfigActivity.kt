package com.example.blurwidgetdemo.widgets.extra

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** Lets the user choose the icon and label shown by an Open app widget. */
class OpenAppWidgetConfigActivity : AppCompatActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var imagePreferenceKey: String? = null
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
            showAppearanceMenu()
        }.setOnCancelListener { finish() }.show()
    }

    private fun showAppearanceMenu() {
        AlertDialog.Builder(this).setTitle("Widget appearance")
            .setItems(arrayOf("Set centred text", "Choose background image", "Choose icon image", "Finish")) { _, choice ->
                when (choice) {
                    0 -> editLabel()
                    1 -> selectImage("open_app_background_uri_$widgetId")
                    2 -> selectImage("open_app_icon_uri_$widgetId")
                    else -> finishConfiguration()
                }
            }.show()
    }

    private fun editLabel() {
        val input = EditText(this).apply {
            setText(getSharedPreferences("widget_prefs", MODE_PRIVATE).getString("open_app_custom_label_$widgetId", ""))
            hint = "Widget text"
            setSingleLine()
        }
        AlertDialog.Builder(this).setTitle("Centred text").setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                getSharedPreferences("widget_prefs", MODE_PRIVATE).edit()
                    .putString("open_app_custom_label_$widgetId", input.text.toString().trim()).apply()
                showAppearanceMenu()
            }.show()
    }

    private fun selectImage(preferenceKey: String) {
        imagePreferenceKey = preferenceKey
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, REQUEST_IMAGE)
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_IMAGE) return
        data?.data?.let { uri ->
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            imagePreferenceKey?.let { key -> getSharedPreferences("widget_prefs", MODE_PRIVATE).edit().putString(key, uri.toString()).apply() }
        }
        showAppearanceMenu()
    }

    private fun finishConfiguration() {
        com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
            .initializeConfiguredWidget(this, com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds.OPEN_APP, widgetId)
        OpenAppWidget().onUpdate(this, AppWidgetManager.getInstance(this), intArrayOf(widgetId))
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }

    private companion object { const val REQUEST_IMAGE = 71 }
}

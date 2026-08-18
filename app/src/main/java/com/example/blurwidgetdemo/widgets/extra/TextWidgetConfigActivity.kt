package com.example.blurwidgetdemo.widgets.extra

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class TextWidgetConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val input = EditText(this).apply { hint = "Enter widget text"; setText(getSharedPreferences("widget_prefs", MODE_PRIVATE).getString("text_widget_$id", "")) }
        AlertDialog.Builder(this).setTitle("Text widget").setView(input).setNegativeButton("Cancel") { _, _ -> finish() }.setPositiveButton("Save") { _, _ ->
            getSharedPreferences("widget_prefs", MODE_PRIVATE).edit().putString("text_widget_$id", input.text.toString().ifBlank { "Text" }).apply()
            TextWidget().onUpdate(this, AppWidgetManager.getInstance(this), intArrayOf(id))
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)); finish()
        }.show()
    }
}

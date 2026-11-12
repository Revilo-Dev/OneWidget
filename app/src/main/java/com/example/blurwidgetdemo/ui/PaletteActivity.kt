package dev.revilodev.onewidget.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.revilodev.onewidget.R

// follows the toolkit switch between native sesl colors and app defaults
internal object SystemPalette {
    private const val KEY = "global_use_color_palette"

    private fun preferences(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    fun isEnabled(context: Context) = preferences(context).getBoolean(KEY, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY, enabled).apply()
    }
}

abstract class PaletteActivity : AppCompatActivity() {
    private var useSystemPalette = true

    override fun onCreate(savedInstanceState: Bundle?) {
        useSystemPalette = SystemPalette.isEnabled(this)
        if (!useSystemPalette) theme.applyStyle(R.style.OneWidgetFixedPalette, true)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (useSystemPalette != SystemPalette.isEnabled(this)) recreate()
    }
}

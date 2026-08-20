package com.example.blurwidgetdemo.widgets

import android.content.SharedPreferences
import android.graphics.Color
import android.content.res.ColorStateList
import android.os.Build
import android.view.Gravity
import android.widget.RemoteViews

/** Appearance settings shared by every information widget. */
object WidgetAppearance {
    private const val GLOBAL_BORDERS = "global_widget_borders"
    private const val GLOBAL_GRADIENTS = "global_widget_gradients"
    enum class Alignment(val label: String, val gravity: Int) {
        START("Start", Gravity.START or Gravity.CENTER_VERTICAL),
        CENTRE("Centre", Gravity.CENTER),
        END("End", Gravity.END or Gravity.CENTER_VERTICAL);
        companion object { fun fromPreference(value: String?) = entries.firstOrNull { it.name == value } ?: CENTRE }
    }
    fun textScale(prefs: SharedPreferences, id: Int) = prefs.getInt("text_scale_$id", 100).coerceIn(50, 150)
    fun textColor(prefs: SharedPreferences, id: Int) = prefs.getInt("text_color_$id", Color.WHITE)
    fun alignment(prefs: SharedPreferences, id: Int) = Alignment.fromPreference(prefs.getString("text_alignment_$id", null))
    fun bordersEnabled(prefs: SharedPreferences) = prefs.getBoolean(GLOBAL_BORDERS, true)
    fun gradientsEnabled(prefs: SharedPreferences) = prefs.getBoolean(GLOBAL_GRADIENTS, true)
    fun applyBorder(views: RemoteViews, prefs: SharedPreferences) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(android.R.id.background, "setForegroundTintList", ColorStateList.valueOf(if (bordersEnabled(prefs)) Color.WHITE else Color.TRANSPARENT))
        }
    }
}

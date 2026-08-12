package com.example.blurwidgetdemo.widgets

import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity

/** Appearance settings shared by every information widget. */
object WidgetAppearance {
    enum class Alignment(val label: String, val gravity: Int) {
        START("Start", Gravity.START or Gravity.CENTER_VERTICAL),
        CENTRE("Centre", Gravity.CENTER),
        END("End", Gravity.END or Gravity.CENTER_VERTICAL);
        companion object { fun fromPreference(value: String?) = entries.firstOrNull { it.name == value } ?: CENTRE }
    }
    fun textScale(prefs: SharedPreferences, id: Int) = prefs.getInt("text_scale_$id", 100).coerceIn(50, 150)
    fun textColor(prefs: SharedPreferences, id: Int) = prefs.getInt("text_color_$id", Color.WHITE)
    fun alignment(prefs: SharedPreferences, id: Int) = Alignment.fromPreference(prefs.getString("text_alignment_$id", null))
}

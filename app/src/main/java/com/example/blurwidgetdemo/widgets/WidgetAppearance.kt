package dev.revilodev.onewidget.widgets

import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity

object WidgetAppearance {
    private const val GLOBAL_BORDERS = "global_widget_borders"
    private const val GLOBAL_GRADIENTS = "global_widget_gradients"

    enum class Alignment(val label: String, val gravity: Int) {
        START("Start", Gravity.START or Gravity.CENTER_VERTICAL),
        CENTRE("Centre", Gravity.CENTER),
        END("End", Gravity.END or Gravity.CENTER_VERTICAL);
        companion object { fun fromPreference(value: String?) = entries.firstOrNull { it.name == value } ?: CENTRE }
    }

    fun textScale(prefs: SharedPreferences, id: Int) = prefs.getInt("text_scale_" + id, 100).coerceIn(50, 150)
    fun textColor(prefs: SharedPreferences, id: Int) = prefs.getInt("text_color_" + id, Color.WHITE)
    fun alignment(prefs: SharedPreferences, id: Int) = Alignment.fromPreference(prefs.getString("text_alignment_" + id, null))
    fun bordersEnabled(prefs: SharedPreferences) = prefs.getBoolean(GLOBAL_BORDERS, true)
    fun gradientsEnabled(prefs: SharedPreferences) = prefs.getBoolean(GLOBAL_GRADIENTS, true)

    fun defaultBorder(prefs: SharedPreferences) = WidgetBorderStyle(
        enabled = bordersEnabled(prefs),
        thicknessPx = prefs.getInt("global_border_thickness_px", 4),
        opacityPercent = prefs.getInt("global_border_opacity", 20)
    ).normalized()

    fun border(prefs: SharedPreferences, id: Int): WidgetBorderStyle {
        WidgetPresets.selected(prefs, id)?.let { return it.border }
        if (!prefs.contains("border_enabled_" + id)) return defaultBorder(prefs)
        return WidgetBorderStyle(prefs.getBoolean("border_enabled_" + id, true),
            prefs.getInt("border_thickness_px_" + id, 4), prefs.getInt("border_opacity_" + id, 20)).normalized()
    }

    fun saveBorder(prefs: SharedPreferences, id: Int, style: WidgetBorderStyle?) {
        prefs.edit().apply {
            if (style == null) {
                remove("border_enabled_" + id)
                remove("border_thickness_px_" + id)
                remove("border_opacity_" + id)
            } else {
                val border = style.normalized()
                putBoolean("border_enabled_" + id, border.enabled)
                putInt("border_thickness_px_" + id, border.thicknessPx)
                putInt("border_opacity_" + id, border.opacityPercent)
            }
        }.apply()
    }

    fun delete(context: android.content.Context, ids: IntArray) {
        val prefs = context.getSharedPreferences(dev.revilodev.onewidget.BlurWidget.WIDGET_PREFS, android.content.Context.MODE_PRIVATE)
        val images = ids.map { backgroundUri(prefs, it) }
        val suffixes = ids.map { "_" + it }
        prefs.edit().apply { prefs.all.keys.filter { key -> suffixes.any(key::endsWith) }.forEach(::remove) }.apply()
        images.forEach { WidgetBackgroundStore.removeIfUnused(context, it) }
        WidgetUpdates.ensureScheduled(context, force = true)
    }

    fun backgroundUri(prefs: SharedPreferences, id: Int): String? =
        prefs.getString("background_image_" + id, null)
            ?: prefs.getString("open_app_background_uri_" + id, null)
            ?: prefs.getString("text_background_uri_" + id, null)
}

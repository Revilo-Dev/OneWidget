package com.example.blurwidgetdemo

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.example.blurwidgetdemo.studio.universal.UniversalElementContent
import com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetContent
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds
import com.example.blurwidgetdemo.widgets.WidgetAppearance

class BlurWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (id in appWidgetIds) {
            editor.remove("level_$id")
            editor.remove("theme_$id")
            editor.remove("tint_hue_$id")
            editor.remove("tint_saturation_$id")
            editor.remove("tint_value_$id")
            editor.remove("tint_alpha_$id")
        }
        editor.apply()
        UniversalProductionWidgetRenderer.deleteModes(context, UniversalWidgetIds.GLASS, appWidgetIds)
    }

    companion object {
        const val WIDGET_PREFS = "widget_prefs"
        private const val GLOBAL_TINT_HUE = "global_default_tint_hue"
        private const val GLOBAL_TINT_SATURATION = "global_default_tint_saturation"
        private const val GLOBAL_TINT_VALUE = "global_default_tint_value"
        private const val GLOBAL_TINT_ALPHA = "global_default_tint_alpha"
        const val LAYOUT_MODE_LARGE = 0
        const val LAYOUT_MODE_COMPACT_STRIP = 1
        const val LAYOUT_MODE_COMPACT_SQUARE = 2
        const val DEFAULT_TINT_HUE = 0f
        const val DEFAULT_TINT_SATURATION = 0f
        const val DEFAULT_TINT_VALUE = 1f
        const val DEFAULT_TINT_ALPHA = 102
        val PRESET_ALPHAS = intArrayOf(38, 102, 178, 240)

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 360)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 260)
            val layoutResource = when (layoutMode(minWidth, minHeight)) {
                LAYOUT_MODE_COMPACT_STRIP -> R.layout.widget_compact_strip
                LAYOUT_MODE_COMPACT_SQUARE -> R.layout.widget_compact_square
                else -> R.layout.widget_blur
            }

            val promoted = UniversalProductionWidgetRenderer.buildIfPromoted(
                context,
                appWidgetManager,
                appWidgetId,
                UniversalWidgetIds.GLASS,
                UniversalWidgetContent(
                    mapOf("universal.surface" to UniversalElementContent())
                )
            )
            val views = promoted ?: RemoteViews(context.packageName, layoutResource).apply {
                // One UI Home looks for this exact view ID and composites its native
                // wallpaper blur beneath the translucent colour on compatible devices.
                // Keep the layout root tagged @android:id/background and its alpha 1..254.
                applySurfaceTint(this, prefs, appWidgetId)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        data class TintDefaults(val hue: Float, val saturation: Float, val value: Float, val alpha: Int)

        fun defaultTint(prefs: SharedPreferences): TintDefaults = TintDefaults(
            hue = prefs.getFloat(GLOBAL_TINT_HUE, DEFAULT_TINT_HUE),
            saturation = prefs.getFloat(GLOBAL_TINT_SATURATION, DEFAULT_TINT_SATURATION),
            value = prefs.getFloat(GLOBAL_TINT_VALUE, DEFAULT_TINT_VALUE),
            alpha = prefs.getInt(GLOBAL_TINT_ALPHA, DEFAULT_TINT_ALPHA).coerceIn(1, 254)
        )

        fun saveDefaultTint(prefs: SharedPreferences, defaults: TintDefaults) {
            prefs.edit()
                .putFloat(GLOBAL_TINT_HUE, defaults.hue.coerceIn(0f, 360f))
                .putFloat(GLOBAL_TINT_SATURATION, defaults.saturation.coerceIn(0f, 1f))
                .putFloat(GLOBAL_TINT_VALUE, defaults.value.coerceIn(0f, 1f))
                .putInt(GLOBAL_TINT_ALPHA, defaults.alpha.coerceIn(1, 254))
                .apply()
        }

        fun tintColor(prefs: SharedPreferences, appWidgetId: Int): Int {
            val defaults = defaultTint(prefs)
            val alpha = if (prefs.contains("tint_alpha_$appWidgetId")) {
                prefs.getInt("tint_alpha_$appWidgetId", DEFAULT_TINT_ALPHA)
            } else if (prefs.contains("level_$appWidgetId")) {
                val oldLevel = prefs.getInt("level_$appWidgetId", 1).coerceIn(0, PRESET_ALPHAS.lastIndex)
                PRESET_ALPHAS[oldLevel]
            } else {
                defaults.alpha
            }
            return tintColor(
                prefs.getFloat("tint_hue_$appWidgetId", defaults.hue),
                prefs.getFloat("tint_saturation_$appWidgetId", defaults.saturation),
                prefs.getFloat("tint_value_$appWidgetId", defaults.value),
                alpha
            )
        }

        fun tintColor(hue: Float, saturation: Float, value: Float, alpha: Int): Int =
            Color.HSVToColor(
                alpha.coerceIn(1, 254),
                floatArrayOf(
                    hue.coerceIn(0f, 360f),
                    saturation.coerceIn(0f, 1f),
                    value.coerceIn(0f, 1f)
                )
            )

        /** Keeps the rounded XML drawable in place while the glass colour changes. */
        fun applySurfaceTint(views: RemoteViews, prefs: SharedPreferences, appWidgetId: Int) {
            views.setInt(
                android.R.id.background,
                "setBackgroundResource",
                if (WidgetAppearance.bordersEnabled(prefs)) {
                    R.drawable.widget_clock_background
                } else {
                    R.drawable.widget_clock_background_no_border
                }
            )
            applySurfaceColor(views, tintColor(prefs, appWidgetId))
        }

        /** Media artwork is clipped by its own matching outline, so it must not add a second edge stroke. */
        fun applyMediaSurfaceTint(views: RemoteViews, prefs: SharedPreferences, appWidgetId: Int) {
            views.setInt(android.R.id.background, "setBackgroundResource", R.drawable.widget_clock_background_no_border)
            applySurfaceColor(views, tintColor(prefs, appWidgetId))
        }

        fun applySurfaceColor(views: RemoteViews, color: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(android.R.id.background, "setBackgroundTintList", ColorStateList.valueOf(color))
            }
        }

        fun layoutMode(minWidth: Int, minHeight: Int): Int = when {
            minHeight <= 100 -> LAYOUT_MODE_COMPACT_STRIP
            minWidth <= 220 -> LAYOUT_MODE_COMPACT_SQUARE
            else -> LAYOUT_MODE_LARGE
        }
    }
}

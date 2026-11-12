package dev.revilodev.onewidget

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.util.Size
import dev.revilodev.onewidget.widgets.WidgetBackgroundArtwork
import dev.revilodev.onewidget.widgets.WidgetAppearance
import dev.revilodev.onewidget.ui.HighlightedSurfaceDrawable

object WidgetPreviewEnvironment {
    fun apply(view: View) = apply(view, fullyRounded = false)
    fun apply(view: View, fullyRounded: Boolean) = apply(view, fullyRounded, columns = 0, rows = 0)

    fun apply(view: View, fullyRounded: Boolean, columns: Int, rows: Int) {
        val prefs = view.context.getSharedPreferences(BlurWidget.WIDGET_PREFS, android.content.Context.MODE_PRIVATE)
        val tint = BlurWidget.defaultTint(prefs)
        val border = WidgetAppearance.defaultBorder(prefs)
        val span = if (columns > 0 && rows > 0) Size(columns, rows) else null
        val width = 84f * columns.coerceAtLeast(2)
        val height = 84f * rows.coerceAtLeast(2)
        val artwork = span?.let { WidgetBackgroundArtwork.drawable(it, width, height) }
        val radius = if (fullyRounded) 1000f else WidgetBackgroundArtwork.radius(width, height, span, artwork)
        val color = BlurWidget.tintColor(tint.hue, tint.saturation, tint.value, tint.alpha)
        view.background = artwork?.let { view.context.getDrawable(it)?.mutate()?.apply { setTint(color) } }
            ?: GradientDrawable().apply {
                setColor(color)
                cornerRadius = radius * view.resources.displayMetrics.density
            }
        if (artwork != null) {
            view.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val corner = minOf(radius * view.resources.displayMetrics.density, minOf(view.width, view.height) / 2f)
                    outline.setRoundRect(0, 0, view.width, view.height, corner)
                }
            }
        }
        view.foreground = if (border.enabled) HighlightedSurfaceDrawable(
            Color.TRANSPARENT, radius * view.resources.displayMetrics.density,
            border.thicknessPx.toFloat(), border.opacityPercent
        ) else null
    }
}

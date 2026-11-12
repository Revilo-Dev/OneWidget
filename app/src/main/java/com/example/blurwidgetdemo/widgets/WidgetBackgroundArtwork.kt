package dev.revilodev.onewidget.widgets

import android.util.Size
import dev.revilodev.onewidget.R
import kotlin.math.roundToInt

internal object WidgetBackgroundArtwork {
    fun drawable(span: Size?, widthDp: Float, heightDp: Float): Int? {
        val columns = span?.width ?: ((widthDp + 30f) / 70f).roundToInt()
        val rows = span?.height ?: ((heightDp + 30f) / 70f).roundToInt()
        return when (columns to rows) {
            2 to 1 -> R.drawable.widget_fill_2x1
            2 to 2 -> R.drawable.widget_fill_2x2
            3 to 2 -> R.drawable.widget_fill_3x2
            4 to 2 -> R.drawable.widget_fill_4x2
            4 to 4 -> R.drawable.widget_fill_4x4
            4 to 5 -> R.drawable.widget_fill_4x5
            else -> null
        }
    }

    fun radius(widthDp: Float, heightDp: Float, span: Size?, artwork: Int?): Float =
        when (artwork) {
            null -> WidgetSurfaceGeometry.radiusDp(widthDp, heightDp, span?.height, span?.width)
            R.drawable.widget_fill_2x1 -> minOf(widthDp, heightDp) / 2f
            else -> minOf(26f, widthDp / 2f, heightDp / 2f)
        }
}

package dev.revilodev.onewidget.widgets

import kotlin.math.min

data class WidgetBorderStyle(
    val enabled: Boolean = true,
    val thicknessPx: Int = 4,
    val opacityPercent: Int = 20
) {
    fun normalized() = copy(thicknessPx = thicknessPx.coerceIn(1, 16), opacityPercent = opacityPercent.coerceIn(0, 100))
}

object WidgetSurfaceGeometry {
    const val CORNER_RADIUS_DP = 24f

    const val WIDE_TWO_ROW_RADIUS_DP = 20f

    fun radiusDp(widthDp: Float, heightDp: Float, rowSpan: Int? = null, columnSpan: Int? = null): Float {
        val halfShortSide = min(widthDp, heightDp) / 2f
        val singleRow = rowSpan?.let { it == 1 } ?: (heightDp <= 88f)
        val twoRows = rowSpan?.let { it == 2 } ?: (heightDp > 88f && heightDp < 150f)
        val wide = columnSpan?.let { it >= 3 } ?: (widthDp >= heightDp * 1.35f)
        return when {
            singleRow -> halfShortSide
            twoRows && wide -> min(WIDE_TWO_ROW_RADIUS_DP, halfShortSide)
            else -> min(CORNER_RADIUS_DP, halfShortSide)
        }
    }

    fun strokeInset(thicknessPx: Float) = thicknessPx / 2f
}

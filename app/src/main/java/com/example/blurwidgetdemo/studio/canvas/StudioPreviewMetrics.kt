package com.example.blurwidgetdemo.studio.canvas

import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import kotlin.math.round

data class StudioCanvasSize(val widthDp: Float, val heightDp: Float)

data class StudioPreviewMetrics(
    val approximateCellWidthDp: Float = 72f,
    val approximateCellHeightDp: Float = 72f,
    val cellGapDp: Float = 8f
) {
    fun canvasSize(variantId: WidgetVariantId.Standard): StudioCanvasSize = StudioCanvasSize(
        widthDp = variantId.columns * approximateCellWidthDp + (variantId.columns - 1) * cellGapDp,
        heightDp = variantId.rows * approximateCellHeightDp + (variantId.rows - 1) * cellGapDp
    )
}

enum class StudioSnapMode(val stepDp: Float) {
    OFF(0f),
    ONE_DP(1f),
    FOUR_DP(4f),
    EIGHT_DP(8f);

    fun snap(value: Float): Float = if (stepDp == 0f) value else round(value / stepDp) * stepDp
}

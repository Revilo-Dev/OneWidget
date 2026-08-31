package com.example.blurwidgetdemo.studio.canvas

import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import org.junit.Assert.assertEquals
import org.junit.Test

class StudioPreviewMetricsTest {
    @Test fun `standard canvas dimensions use centralized cell metrics and gaps`() {
        val metrics = StudioPreviewMetrics(
            approximateCellWidthDp = 72f,
            approximateCellHeightDp = 80f,
            cellGapDp = 8f
        )
        assertEquals(StudioCanvasSize(72f, 80f), metrics.canvasSize(WidgetVariantId.Standard(1, 1)))
        assertEquals(StudioCanvasSize(392f, 432f), metrics.canvasSize(WidgetVariantId.Standard(5, 5)))
    }

    @Test fun `snap modes round to their configured dp grid`() {
        assertEquals(6.4f, StudioSnapMode.OFF.snap(6.4f), 0.001f)
        assertEquals(6f, StudioSnapMode.ONE_DP.snap(6.4f), 0.001f)
        assertEquals(8f, StudioSnapMode.FOUR_DP.snap(6.4f), 0.001f)
        assertEquals(8f, StudioSnapMode.EIGHT_DP.snap(6.4f), 0.001f)
    }
}

package com.example.blurwidgetdemo.widgets.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockConfigPreviewMapperTest {
    @Test fun `unsaved controls map directly to renderer configuration`() {
        val original = ClockWidgetSettings(showDate = false, font = ClockFont.LATO)
        val preview = ClockConfigPreviewMapper.map(
            original,
            hue = 210f,
            saturation = 0.4f,
            value = 0.7f,
            alpha = 73,
            textScalePercent = 132,
            textColor = 0xFF123456.toInt()
        )

        assertEquals(ClockFont.LATO, preview.font)
        assertEquals(false, preview.showDate)
        assertEquals(210f, preview.hue)
        assertEquals(73, preview.alpha)
        assertEquals(132, preview.textScalePercent)
        assertEquals(0xFF123456.toInt(), preview.textColor)
        assertEquals(ClockWidgetSettings(showDate = false, font = ClockFont.LATO), original)
    }

    @Test fun `config preview uses production layout and omits click actions`() {
        val settings = ClockWidgetSettings(font = ClockFont.CAPRIOLA)
        val size = ClockRenderSize(312f, 152f)
        val preview = ClockRemoteViewsRenderer.plan(settings, size, design = null, mode = ClockRenderMode.CONFIG_PREVIEW)
        val production = ClockRemoteViewsRenderer.plan(settings, size, design = null, mode = ClockRenderMode.PRODUCTION)

        assertEquals(production.productionLayoutResId, preview.appliedLayoutResId)
        assertEquals(production.appliedLayoutResId, preview.appliedLayoutResId)
        assertEquals(false, preview.installTapActions)
        assertEquals(emptySet<Int>(), preview.tapActionViewIds)
    }
}

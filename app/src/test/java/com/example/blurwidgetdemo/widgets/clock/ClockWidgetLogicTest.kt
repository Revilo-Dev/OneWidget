package com.example.blurwidgetdemo.widgets.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockWidgetLogicTest {
    @Test fun `short widgets use compact layout`() {
        assertEquals(ClockWidgetLayout.Category.COMPACT, ClockWidgetLayout.category(180, 80))
    }
    @Test fun `medium widgets use medium layout`() {
        assertEquals(ClockWidgetLayout.Category.MEDIUM, ClockWidgetLayout.category(280, 130))
    }
    @Test fun `large dimensions use large layout`() {
        assertEquals(ClockWidgetLayout.Category.LARGE, ClockWidgetLayout.category(360, 130))
        assertEquals(ClockWidgetLayout.Category.LARGE, ClockWidgetLayout.category(250, 180))
    }
    @Test fun `format and alignment defaults are stable`() {
        val settings = ClockWidgetSettings()
        assertEquals(ClockTimeFormat.SYSTEM, settings.timeFormat)
        assertEquals(ClockTextAlignment.CENTER, settings.alignment)
    }
}

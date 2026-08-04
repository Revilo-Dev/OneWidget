package com.example.blurwidgetdemo.widgets.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockWidgetLogicTest {
    @Test fun `short widgets use compact layout`() {
        assertEquals(ClockWidgetLayout.Category.COMPACT, ClockWidgetLayout.category(180, 80))
    }
    @Test fun `medium widgets use medium layout`() {
        assertEquals(ClockWidgetLayout.Category.MEDIUM, ClockWidgetLayout.category(280, 130))
    }
    @Test fun `narrow or short widgets use compact layout`() {
        assertEquals(ClockWidgetLayout.Category.COMPACT, ClockWidgetLayout.category(180, 160))
        assertEquals(ClockWidgetLayout.Category.COMPACT, ClockWidgetLayout.category(320, 80))
    }
    @Test fun `large dimensions use large layout`() {
        assertEquals(ClockWidgetLayout.Category.LARGE, ClockWidgetLayout.category(360, 130))
        assertEquals(ClockWidgetLayout.Category.LARGE, ClockWidgetLayout.category(250, 180))
    }
    @Test fun `format and alignment defaults are stable`() {
        val settings = ClockWidgetSettings()
        assertEquals(ClockTimeFormat.SYSTEM, settings.timeFormat)
        assertEquals(ClockTextAlignment.CENTER, settings.alignment)
        assertEquals(true, settings.showAmPm)
        assertEquals(false, settings.showSeconds)
    }

    @Test fun `font preference parsing is stable and safe`() {
        assertEquals(ClockFont.LATO, ClockFont.fromPreference("lato"))
        assertEquals(ClockFont.LATO, ClockFont.fromPreference("LATO"))
        assertEquals(ClockFont.SYSTEM, ClockFont.fromPreference("removed_font"))
    }

    @Test fun `every font and size resolves to a layout`() {
        ClockFont.entries.forEach { font ->
            ClockWidgetLayout.Category.entries.forEach { size ->
                assertTrue("$font / $size", ClockLayoutResolver.resolve(size, font) != 0)
            }
        }
    }
}

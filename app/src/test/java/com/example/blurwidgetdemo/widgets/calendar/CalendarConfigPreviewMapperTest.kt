package dev.revilodev.onewidget.widgets.calendar

import dev.revilodev.onewidget.widgets.WidgetAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarConfigPreviewMapperTest {
    @Test fun `unsaved controls map without mutating stored configuration`() {
        val base = CalendarWidgetConfigurationRepository.studioSample()
        val mapped = CalendarConfigPreviewMapper.map(
            base = base,
            surfaceColor = 0x33445566,
            textColor = 0xFF123456.toInt(),
            textScalePercent = 180,
            alignment = WidgetAppearance.Alignment.END
        )

        assertEquals(0x33445566, mapped.surfaceColor)
        assertEquals(0xFF123456.toInt(), mapped.textColor)
        assertEquals(150, mapped.textScalePercent)
        assertEquals(WidgetAppearance.Alignment.END, mapped.alignment)
        assertEquals(100, base.textScalePercent)
    }
}

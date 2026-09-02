package com.example.blurwidgetdemo.widgets.battery

import com.example.blurwidgetdemo.widgets.WidgetAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryConfigPreviewMapperTest {
    @Test fun `unsaved battery controls map without mutating base configuration`() {
        val base = BatteryWidgetConfigurationRepository.studioSample()
        val mapped = BatteryConfigPreviewMapper.map(
            base,
            surfaceColor = 0x66123456,
            textColor = 0xFFABCDEF.toInt(),
            textScalePercent = 125,
            alignment = WidgetAppearance.Alignment.END,
            compactDeviceKey = "bt:test",
            colors = mapOf("charging" to 0xFF010203.toInt())
        )

        assertEquals(0x66123456, mapped.surfaceColor)
        assertEquals(125, mapped.textScalePercent)
        assertEquals(WidgetAppearance.Alignment.END, mapped.alignment)
        assertEquals("bt:test", mapped.compactDeviceKey)
        assertEquals(0xFF010203.toInt(), mapped.stateColors.charging)
        assertEquals(BatteryWidget.PHONE, base.compactDeviceKey)
    }
}

package com.example.blurwidgetdemo.widgets.storage

import com.example.blurwidgetdemo.widgets.WidgetAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageConfigPreviewMapperTest {
    @Test fun `unsaved controls map without changing the base instance`() {
        val base = StorageWidgetConfigurationRepository.studioSample()
        val mapped = StorageConfigPreviewMapper.map(
            base = base,
            surfaceColor = 0x33445566,
            textColor = 0xFF123456.toInt(),
            textScalePercent = 180,
            alignment = WidgetAppearance.Alignment.END,
            compactMetric = StorageWidget.CompactMetric.MEMORY,
            hideData = true,
            showTitles = false,
            detailMode = StorageWidget.DetailMode.AMOUNT,
            colors = mapOf(StorageWidget.CompactMetric.MEMORY to 0xFFABCDEF.toInt())
        )

        assertEquals(0x33445566, mapped.surfaceColor)
        assertEquals(150, mapped.textScalePercent)
        assertEquals(StorageWidget.CompactMetric.MEMORY, mapped.compactMetric)
        assertEquals(true, mapped.hideData)
        assertEquals(false, mapped.showTitles)
        assertEquals(StorageWidget.DetailMode.AMOUNT, mapped.detailMode)
        assertEquals(0xFFABCDEF.toInt(), mapped.metricColors.memory)
        assertEquals(base.metricColors.storage, mapped.metricColors.storage)
        assertEquals(StorageWidget.CompactMetric.STORAGE, base.compactMetric)
    }
}

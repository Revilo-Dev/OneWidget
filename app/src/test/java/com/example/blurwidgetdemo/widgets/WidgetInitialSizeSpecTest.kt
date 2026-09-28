package dev.revilodev.onewidget.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetInitialSizeSpecTest {
    @Test
    fun `grid records the selected 4 by 1 variant`() {
        val spec = WidgetInitialSizeSpec.grid(4, 1)

        assertEquals("4x1", spec.stableId)
        assertEquals(250, spec.minWidthDp)
        assertEquals(40, spec.minHeightDp)
        assertEquals("wideSmall", spec.providerMetadataSize)
    }

    @Test
    fun `grid retains the compact toggle fallback`() {
        val spec = WidgetInitialSizeSpec.grid(1, 1)

        assertEquals(1, spec.targetCellWidth)
        assertEquals(1, spec.targetCellHeight)
        assertEquals("tiny", spec.providerMetadataSize)
    }

    @Test
    fun `grid classifies multi row selections as large`() {
        assertEquals("large", WidgetInitialSizeSpec.grid(3, 2).providerMetadataSize)
    }
}

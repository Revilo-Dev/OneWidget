package dev.revilodev.onewidget.studio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetVariantIdTest {
    @Test fun `standard variants include 1x1 and 5x5`() {
        assertTrue(WidgetVariantId.Standard(1, 1).isValid())
        assertTrue(WidgetVariantId.Standard(5, 5).isValid())
        assertEquals(25, WidgetVariantId.standardVariants.size)
        assertEquals(WidgetVariantId.Standard(1, 1), WidgetVariantId.parse("1x1"))
        assertEquals(WidgetVariantId.Standard(5, 5), WidgetVariantId.parse("5x5"))
    }

    @Test fun `zero grid dimension is invalid`() {
        assertFalse(WidgetVariantId.Standard(0, 1).isValid())
        assertEquals(null, WidgetVariantId.parse("0x1"))
    }

    @Test fun `custom variants have a stable representation`() {
        val custom = WidgetVariantId.Custom("device_size")
        assertTrue(custom.isValid())
        assertEquals("custom:device_size", custom.stableId)
        assertEquals(custom, WidgetVariantId.parse(custom.stableId))
    }
}

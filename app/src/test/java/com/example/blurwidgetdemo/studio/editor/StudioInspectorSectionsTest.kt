package dev.revilodev.onewidget.studio.editor

import dev.revilodev.onewidget.studio.battery.BatteryStudioDescriptor
import dev.revilodev.onewidget.studio.model.ElementCapability
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioInspectorSectionsTest {
    @Test fun `corner radius appears only for declared capability`() {
        val battery = BatteryStudioDescriptor.create()
        val progress = battery.elements.single { it.elementId == BatteryStudioDescriptor.PROGRESS }
        val percentage = battery.elements.single { it.elementId == BatteryStudioDescriptor.PERCENTAGE }

        assertTrue(StudioInspectorSections.from(progress.capabilities).cornerRadius)
        assertFalse(StudioInspectorSections.from(percentage.capabilities).cornerRadius)
        assertFalse(StudioInspectorSections.from(setOf(ElementCapability.SIZE)).cornerRadius)
    }
}

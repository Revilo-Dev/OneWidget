package com.example.blurwidgetdemo.studio.registry

import com.example.blurwidgetdemo.studio.clock.ClockStudioDescriptor
import com.example.blurwidgetdemo.studio.battery.BatteryStudioDescriptor
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStudioRegistryTest {
    @Test fun `default registry contains clock and battery only`() {
        val registry = DefaultWidgetStudioRegistry.create()
        assertEquals(2, registry.registeredWidgetCount())
        assertEquals("Digital Clock", registry.descriptor(ClockStudioDescriptor.WIDGET_ID)?.displayName)
        assertEquals("Battery", registry.descriptor(BatteryStudioDescriptor.WIDGET_ID)?.displayName)
        assertNull(registry.descriptor("unsupported_widget"))
    }

    @Test fun `clock exposes all standard variants`() {
        val descriptor = DefaultWidgetStudioRegistry.create().descriptor(ClockStudioDescriptor.WIDGET_ID)!!
        assertEquals(25, descriptor.supportedVariants.size)
        assertNotNull(descriptor.renderer)
        assertTrue(WidgetVariantId.Standard(1, 1) in descriptor.supportedVariants)
        assertTrue(WidgetVariantId.Standard(5, 5) in descriptor.supportedVariants)
    }

    @Test fun `clock element IDs are stable and unique`() {
        val descriptor = ClockStudioDescriptor.create()
        assertEquals(
            listOf("clock.time", "clock.date", "clock.day", "clock.battery"),
            descriptor.elements.map { it.elementId }
        )
        assertEquals(descriptor.elements.size, descriptor.elements.map { it.elementId }.toSet().size)
        assertEquals(setOf(ClockStudioDescriptor.TIME), descriptor.requiredElementIds)
    }

    @Test fun `duplicate widget IDs are rejected`() {
        val registry = WidgetStudioRegistry()
        val descriptor = ClockStudioDescriptor.create()
        assertTrue(registry.register(descriptor) is RegistryRegistrationResult.Registered)
        assertTrue(registry.register(descriptor) is RegistryRegistrationResult.Rejected)
        assertEquals(1, registry.registeredWidgetCount())
    }
}

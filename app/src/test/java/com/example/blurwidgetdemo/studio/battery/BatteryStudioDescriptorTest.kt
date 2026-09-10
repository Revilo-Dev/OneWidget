package dev.revilodev.onewidget.studio.battery

import dev.revilodev.onewidget.studio.editor.EditorActionResult
import dev.revilodev.onewidget.studio.editor.WidgetStudioEditCommand
import dev.revilodev.onewidget.studio.editor.WidgetStudioEditorController
import dev.revilodev.onewidget.studio.model.ElementCapability
import dev.revilodev.onewidget.studio.model.WidgetDesignResolver
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.data.StudioRepositoryError
import dev.revilodev.onewidget.studio.data.StudioRepositoryErrorKind
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryStudioDescriptorTest {
    private val descriptor = BatteryStudioDescriptor.create()

    @Test fun `battery ID variants and elements are stable`() {
        assertEquals("battery", descriptor.widgetId)
        assertEquals(25, descriptor.supportedVariants.size)
        assertEquals(
            listOf("battery.percentage", "battery.status", "battery.icon", "battery.progress"),
            descriptor.elements.map { it.elementId }
        )
        assertEquals(descriptor.elements.size, descriptor.elements.map { it.elementId }.toSet().size)
        assertNotNull(descriptor.renderer)
    }

    @Test fun `capabilities match actual battery content`() {
        val percentage = descriptor.elements.single { it.elementId == BatteryStudioDescriptor.PERCENTAGE }
        val icon = descriptor.elements.single { it.elementId == BatteryStudioDescriptor.ICON }
        val progress = descriptor.elements.single { it.elementId == BatteryStudioDescriptor.PROGRESS }
        assertTrue(ElementCapability.ALIGNMENT in percentage.capabilities)
        assertFalse(ElementCapability.ALIGNMENT in icon.capabilities)
        assertEquals(setOf(BatteryStudioDescriptor.PERCENTAGE), descriptor.requiredElementIds)
        assertTrue(ElementCapability.CORNER_RADIUS in progress.capabilities)
        assertTrue(descriptor.elements.filter { it.elementId != BatteryStudioDescriptor.PROGRESS }
            .none { ElementCapability.CORNER_RADIUS in it.capabilities })
    }

    @Test fun `base and edge variants resolve`() {
        listOf(WidgetVariantId.Standard(1, 1), WidgetVariantId.Standard(5, 5)).forEach { variant ->
            val result = WidgetDesignResolver.resolve(BatteryStudioDescriptor.defaultDesign(), variant)
            assertTrue(result.isSuccess)
            assertEquals(4, result.design?.elements?.size)
        }
    }

    @Test fun `visibility position and radius overrides remain sparse`() {
        val controller = controller()
        assertTrue(controller.selectVariant(WidgetVariantId.Standard(3, 2)))
        success(controller.execute(WidgetStudioEditCommand.SetVisibility(BatteryStudioDescriptor.STATUS, false)))
        success(controller.execute(WidgetStudioEditCommand.MoveElement(BatteryStudioDescriptor.PERCENTAGE, 9f, -12f)))
        success(controller.execute(WidgetStudioEditCommand.SetCornerRadius(BatteryStudioDescriptor.PROGRESS, 7f)))

        val resolved = controller.state.resolvedDesign!!
        assertFalse(resolved.elements.single { it.elementId == BatteryStudioDescriptor.STATUS }.visible)
        assertEquals(9f, resolved.elements.single { it.elementId == BatteryStudioDescriptor.PERCENTAGE }.xDp)
        assertEquals(7f, resolved.elements.single { it.elementId == BatteryStudioDescriptor.PROGRESS }.cornerRadiusDp)
        val overrides = controller.state.workingDesign.variants.getValue(WidgetVariantId.Standard(3, 2)).elementOverrides
        assertEquals(3, overrides.size)
    }

    @Test fun `corner radius is rejected for text elements`() {
        val controller = controller()
        val result = controller.execute(WidgetStudioEditCommand.SetCornerRadius(BatteryStudioDescriptor.PERCENTAGE, 8f))
        assertTrue(result is EditorActionResult.Failure)
        assertFalse(controller.state.dirty)
    }

    private fun controller() = WidgetStudioEditorController(
        descriptor = descriptor,
        loadDesign = {
            StudioRepositoryResult.Failure(
                StudioRepositoryError(StudioRepositoryErrorKind.NOT_FOUND, "missing")
            )
        },
        saveDesign = { StudioRepositoryResult.Success(it.copy(revision = it.revision + 1L)) }
    ).also { success(it.initialize()) }

    private fun success(result: EditorActionResult) {
        assertTrue("Expected success but got $result", result is EditorActionResult.Success)
    }
}

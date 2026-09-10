package dev.revilodev.onewidget.studio.calendar

import dev.revilodev.onewidget.studio.data.StudioRepositoryError
import dev.revilodev.onewidget.studio.data.StudioRepositoryErrorKind
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.editor.EditorActionResult
import dev.revilodev.onewidget.studio.editor.WidgetStudioEditCommand
import dev.revilodev.onewidget.studio.editor.WidgetStudioEditorController
import dev.revilodev.onewidget.studio.model.ElementCapability
import dev.revilodev.onewidget.studio.model.WidgetDesignResolver
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarStudioDescriptorTest {
    private val descriptor = CalendarStudioDescriptor.create()

    @Test fun `calendar ID variants elements and renderer are stable`() {
        assertEquals("calendar", descriptor.widgetId)
        assertEquals(25, descriptor.supportedVariants.size)
        assertEquals(listOf("calendar.title", "calendar.details"), descriptor.elements.map { it.elementId })
        assertEquals(descriptor.elements.size, descriptor.requiredElementIds.size)
        assertNotNull(descriptor.renderer)
    }

    @Test fun `calendar text capabilities exclude unrelated progress properties`() {
        assertTrue(descriptor.elements.all { ElementCapability.ALIGNMENT in it.capabilities })
        assertTrue(descriptor.elements.none { ElementCapability.CORNER_RADIUS in it.capabilities })
        assertTrue(descriptor.elements.none { ElementCapability.Z_ORDER in it.capabilities })
    }

    @Test fun `compact hides details and taller variants show both elements`() {
        val compact = requireNotNull(
            WidgetDesignResolver.resolve(
                CalendarStudioDescriptor.defaultDesign(),
                WidgetVariantId.Standard(2, 1)
            ).design
        )
        val large = requireNotNull(
            WidgetDesignResolver.resolve(
                CalendarStudioDescriptor.defaultDesign(),
                WidgetVariantId.Standard(4, 2)
            ).design
        )

        assertFalse(compact.elements.single { it.elementId == CalendarStudioDescriptor.DETAILS }.visible)
        assertEquals(2, large.elements.count { it.visible })
    }

    @Test fun `title and details edits remain sparse`() {
        val controller = controller()
        assertTrue(controller.selectVariant(WidgetVariantId.Standard(3, 2)))
        success(controller.execute(WidgetStudioEditCommand.MoveElement(CalendarStudioDescriptor.TITLE, 8f, -16f)))
        success(controller.execute(WidgetStudioEditCommand.SetVisibility(CalendarStudioDescriptor.DETAILS, false)))

        val overrides = controller.state.workingDesign.variants
            .getValue(WidgetVariantId.Standard(3, 2)).elementOverrides
        assertEquals(2, overrides.size)
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

package dev.revilodev.onewidget.studio.storage

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

class StorageStudioDescriptorTest {
    private val descriptor = StorageStudioDescriptor.create()

    @Test fun `storage ID variants elements and renderer are stable`() {
        assertEquals("storage", descriptor.widgetId)
        assertEquals(25, descriptor.supportedVariants.size)
        assertEquals(
            listOf("storage.primary", "storage.storage", "storage.memory", "storage.data"),
            descriptor.elements.map { it.elementId }
        )
        assertEquals(descriptor.elements.size, descriptor.elements.map { it.elementId }.toSet().size)
        assertNotNull(descriptor.renderer)
    }

    @Test fun `metric rows expose their actual shared capabilities`() {
        assertTrue(descriptor.elements.all { ElementCapability.ALIGNMENT in it.capabilities })
        assertTrue(descriptor.elements.all { ElementCapability.CORNER_RADIUS in it.capabilities })
        assertEquals(
            setOf(
                StorageStudioDescriptor.PRIMARY,
                StorageStudioDescriptor.STORAGE,
                StorageStudioDescriptor.MEMORY,
                StorageStudioDescriptor.DATA
            ),
            descriptor.requiredElementIds
        )
    }

    @Test fun `compact and large defaults resolve to distinct row visibility`() {
        val compact = requireNotNull(
            WidgetDesignResolver.resolve(
                StorageStudioDescriptor.defaultDesign(),
                WidgetVariantId.Standard(2, 1)
            ).design
        )
        val large = requireNotNull(
            WidgetDesignResolver.resolve(
                StorageStudioDescriptor.defaultDesign(),
                WidgetVariantId.Standard(4, 2)
            ).design
        )

        assertTrue(compact.elements.single { it.elementId == StorageStudioDescriptor.PRIMARY }.visible)
        assertTrue(compact.elements.filter { it.elementId != StorageStudioDescriptor.PRIMARY }.none { it.visible })
        assertFalse(large.elements.single { it.elementId == StorageStudioDescriptor.PRIMARY }.visible)
        assertEquals(3, large.elements.count { it.visible })
    }

    @Test fun `row edits remain sparse and capability checked`() {
        val controller = controller()
        assertTrue(controller.selectVariant(WidgetVariantId.Standard(3, 2)))
        success(controller.execute(WidgetStudioEditCommand.MoveElement(StorageStudioDescriptor.STORAGE, 8f, -34f)))
        success(controller.execute(WidgetStudioEditCommand.SetCornerRadius(StorageStudioDescriptor.DATA, 7f)))

        val overrides = controller.state.workingDesign.variants
            .getValue(WidgetVariantId.Standard(3, 2)).elementOverrides
        assertEquals(2, overrides.size)
        assertEquals(7f, controller.state.resolvedDesign!!.elements
            .single { it.elementId == StorageStudioDescriptor.DATA }.cornerRadiusDp)
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

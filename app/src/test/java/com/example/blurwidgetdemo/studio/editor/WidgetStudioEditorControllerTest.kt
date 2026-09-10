package dev.revilodev.onewidget.studio.editor

import dev.revilodev.onewidget.studio.clock.ClockStudioDescriptor
import dev.revilodev.onewidget.studio.battery.BatteryStudioDescriptor
import dev.revilodev.onewidget.studio.data.StudioRepositoryError
import dev.revilodev.onewidget.studio.data.StudioRepositoryErrorKind
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.model.DimensionMode
import dev.revilodev.onewidget.studio.model.HorizontalAlignment
import dev.revilodev.onewidget.studio.model.VerticalAlignment
import dev.revilodev.onewidget.studio.model.WidgetAnchor
import dev.revilodev.onewidget.studio.model.WidgetDesignSpec
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStudioEditorControllerTest {
    private val twoByOne = WidgetVariantId.Standard(2, 1)
    private val fourByTwo = WidgetVariantId.Standard(4, 2)

    @Test fun `default widget and variant are selected`() {
        val controller = controller()
        assertEquals(ClockStudioDescriptor.WIDGET_ID, controller.state.selectedWidgetId)
        assertEquals(twoByOne, controller.state.selectedVariantId)
        assertEquals(ClockStudioDescriptor.TIME, controller.state.selectedElementId)
        assertFalse(controller.state.dirty)
    }

    @Test fun `element selection accepts registered IDs and rejects invalid IDs`() {
        val controller = controller()
        assertTrue(controller.selectElement(ClockStudioDescriptor.DATE))
        assertEquals(ClockStudioDescriptor.DATE, controller.state.selectedElementId)
        assertFalse(controller.selectElement("clock.unknown"))
        assertEquals(ClockStudioDescriptor.DATE, controller.state.selectedElementId)
    }

    @Test fun `move element updates the selected variant`() {
        val controller = controller()
        success(controller.execute(WidgetStudioEditCommand.MoveElement(ClockStudioDescriptor.TIME, 12f, -6f)))
        assertEquals(12f, controller.element(ClockStudioDescriptor.TIME).xDp)
        assertEquals(-6f, controller.element(ClockStudioDescriptor.TIME).yDp)
    }

    @Test fun `visibility edit is applied`() {
        val controller = controller()
        success(controller.execute(WidgetStudioEditCommand.SetVisibility(ClockStudioDescriptor.TIME, false)))
        assertFalse(controller.element(ClockStudioDescriptor.TIME).visible)
    }

    @Test fun `padding edit is applied`() {
        val controller = controller()
        success(controller.execute(WidgetStudioEditCommand.SetPadding(ClockStudioDescriptor.TIME, 1f, 2f, 3f, 4f)))
        val element = controller.element(ClockStudioDescriptor.TIME)
        assertEquals(listOf(1f, 2f, 3f, 4f), listOf(
            element.paddingStartDp,
            element.paddingTopDp,
            element.paddingEndDp,
            element.paddingBottomDp
        ))
    }

    @Test fun `anchor change preserves absolute element centre`() {
        val controller = controller()
        val before = controller.element(ClockStudioDescriptor.TIME)
        val beforeAnchor = AnchorPositioning.anchorPoint(before.anchor, 200f, 100f)
        success(controller.execute(
            WidgetStudioEditCommand.SetAnchor(
                ClockStudioDescriptor.TIME,
                WidgetAnchor.TOP_START,
                200f,
                100f
            )
        ))
        val after = controller.element(ClockStudioDescriptor.TIME)
        val afterAnchor = AnchorPositioning.anchorPoint(after.anchor, 200f, 100f)
        assertEquals(beforeAnchor.x + before.xDp, afterAnchor.x + after.xDp, 0.001f)
        assertEquals(beforeAnchor.y + before.yDp, afterAnchor.y + after.yDp, 0.001f)
    }

    @Test fun `alignment edit is applied`() {
        val controller = controller()
        success(controller.execute(
            WidgetStudioEditCommand.SetAlignment(
                ClockStudioDescriptor.TIME,
                HorizontalAlignment.END,
                VerticalAlignment.BOTTOM
            )
        ))
        assertEquals(HorizontalAlignment.END, controller.element(ClockStudioDescriptor.TIME).horizontalAlignment)
        assertEquals(VerticalAlignment.BOTTOM, controller.element(ClockStudioDescriptor.TIME).verticalAlignment)
    }

    @Test fun `width and height modes are editable`() {
        val controller = controller()
        success(controller.execute(WidgetStudioEditCommand.SetWidthMode(ClockStudioDescriptor.TIME, DimensionMode.FIXED)))
        success(controller.execute(WidgetStudioEditCommand.SetHeightMode(ClockStudioDescriptor.TIME, DimensionMode.MATCH)))
        assertEquals(DimensionMode.FIXED, controller.element(ClockStudioDescriptor.TIME).widthMode)
        assertEquals(DimensionMode.MATCH, controller.element(ClockStudioDescriptor.TIME).heightMode)
    }

    @Test fun `fixed width and height are editable`() {
        val controller = controller()
        success(controller.execute(WidgetStudioEditCommand.SetFixedWidth(ClockStudioDescriptor.TIME, 120f)))
        success(controller.execute(WidgetStudioEditCommand.SetFixedHeight(ClockStudioDescriptor.TIME, 48f)))
        assertEquals(120f, controller.element(ClockStudioDescriptor.TIME).widthDp)
        assertEquals(48f, controller.element(ClockStudioDescriptor.TIME).heightDp)
    }

    @Test fun `edits create sparse overrides and restoring base removes the field`() {
        val controller = controller()
        success(controller.execute(WidgetStudioEditCommand.MoveElement(ClockStudioDescriptor.TIME, 20f, -8f)))
        assertEquals(
            20f,
            controller.state.workingDesign.variants[twoByOne]?.elementOverrides
                ?.get(ClockStudioDescriptor.TIME)?.xDp
        )
        success(controller.execute(WidgetStudioEditCommand.MoveElement(ClockStudioDescriptor.TIME, 0f, -8f)))
        assertNull(
            controller.state.workingDesign.variants[twoByOne]?.elementOverrides
                ?.get(ClockStudioDescriptor.TIME)?.xDp
        )
    }

    @Test fun `switching variants preserves both sets of edits`() {
        val controller = controller()
        success(controller.execute(WidgetStudioEditCommand.MoveElement(ClockStudioDescriptor.TIME, 11f, -8f)))
        assertTrue(controller.selectVariant(fourByTwo))
        success(controller.execute(WidgetStudioEditCommand.MoveElement(ClockStudioDescriptor.TIME, 31f, -28f)))
        assertTrue(controller.selectVariant(twoByOne))
        assertEquals(11f, controller.element(ClockStudioDescriptor.TIME).xDp)
        assertTrue(controller.selectVariant(fourByTwo))
        assertEquals(31f, controller.element(ClockStudioDescriptor.TIME).xDp)
    }

    @Test fun `copy variant changes target but not source and is undoable`() {
        val controller = controller()
        assertTrue(controller.selectVariant(fourByTwo))
        val sourceBefore = controller.state.workingDesign.variants[fourByTwo]
        val sourceResolved = controller.state.resolvedDesign
        val target = WidgetVariantId.Standard(3, 2)
        assertTrue(controller.selectVariant(target))
        success(controller.execute(WidgetStudioEditCommand.CopyVariant(fourByTwo)))
        assertEquals(sourceResolved?.elements, controller.state.resolvedDesign?.elements)
        assertEquals(sourceBefore, controller.state.workingDesign.variants[fourByTwo])
        assertTrue(controller.undo())
        assertFalse(controller.targetVariantHasOverrides())
    }

    @Test fun `reset element removes its override and undo restores it`() {
        val controller = controller()
        success(controller.execute(WidgetStudioEditCommand.MoveElement(ClockStudioDescriptor.TIME, 17f, -8f)))
        success(controller.execute(WidgetStudioEditCommand.ResetElement(ClockStudioDescriptor.TIME)))
        assertNull(controller.state.workingDesign.variants[twoByOne]?.elementOverrides?.get(ClockStudioDescriptor.TIME))
        assertTrue(controller.undo())
        assertEquals(17f, controller.element(ClockStudioDescriptor.TIME).xDp)
    }

    @Test fun `reset variant removes all overrides and undo restores them`() {
        val controller = controller()
        assertTrue(controller.targetVariantHasOverrides())
        success(controller.execute(WidgetStudioEditCommand.ResetVariant))
        assertFalse(controller.targetVariantHasOverrides())
        assertTrue(controller.undo())
        assertTrue(controller.targetVariantHasOverrides())
    }

    @Test fun `undo redo and new edit history behave correctly`() {
        val controller = controller()
        success(controller.execute(WidgetStudioEditCommand.MoveElement(ClockStudioDescriptor.TIME, 10f, -8f)))
        assertTrue(controller.undo())
        assertEquals(0f, controller.element(ClockStudioDescriptor.TIME).xDp)
        assertTrue(controller.redo())
        assertEquals(10f, controller.element(ClockStudioDescriptor.TIME).xDp)
        assertTrue(controller.undo())
        success(controller.execute(WidgetStudioEditCommand.SetVisibility(ClockStudioDescriptor.TIME, false)))
        assertFalse(controller.state.canRedo)
    }

    @Test fun `one drag produces one history entry`() {
        val controller = controller()
        success(controller.beginDrag(ClockStudioDescriptor.TIME))
        success(controller.updateDrag(1f, -8f))
        success(controller.updateDrag(2f, -8f))
        success(controller.updateDrag(3f, -8f))
        success(controller.endDrag())
        assertEquals(1, controller.undoDepth())
        assertTrue(controller.undo())
        assertEquals(0f, controller.element(ClockStudioDescriptor.TIME).xDp)
    }

    @Test fun `history retains the latest fifty operations`() {
        val controller = controller()
        repeat(55) { index ->
            success(controller.execute(
                WidgetStudioEditCommand.MoveElement(ClockStudioDescriptor.TIME, index + 1f, -8f)
            ))
        }
        assertEquals(50, controller.undoDepth())
        repeat(50) { assertTrue(controller.undo()) }
        assertEquals(5f, controller.element(ClockStudioDescriptor.TIME).xDp)
    }

    @Test fun `save increments revision reloads and clears dirty state`() {
        val repository = FakeRepository()
        val controller = controller(repository)
        success(controller.execute(WidgetStudioEditCommand.MoveElement(ClockStudioDescriptor.TIME, 15f, -8f)))
        assertTrue(controller.state.dirty)
        success(controller.save())
        assertEquals(1L, controller.state.workingDesign.revision)
        assertFalse(controller.state.dirty)

        val reopened = controller(repository)
        assertEquals(15f, reopened.element(ClockStudioDescriptor.TIME).xDp)
        success(reopened.execute(WidgetStudioEditCommand.SetVisibility(ClockStudioDescriptor.TIME, false)))
        success(reopened.reload())
        assertTrue(reopened.element(ClockStudioDescriptor.TIME).visible)
        assertFalse(reopened.state.dirty)
    }

    @Test fun `invalid numeric input is rejected`() {
        val controller = controller()
        failure(controller.execute(
            WidgetStudioEditCommand.MoveElement(ClockStudioDescriptor.TIME, Float.NaN, 0f)
        ))
        failure(controller.execute(
            WidgetStudioEditCommand.SetFixedWidth(ClockStudioDescriptor.TIME, 0f)
        ))
        failure(controller.execute(
            WidgetStudioEditCommand.SetPadding(ClockStudioDescriptor.TIME, -1f, 0f, 0f, 0f)
        ))
        assertFalse(controller.state.dirty)
    }

    @Test fun `invalid design cannot save`() {
        val repository = FakeRepository()
        val controller = controller(repository)
        val invalid = controller.state.workingDesign.copy(
            baseVariant = controller.state.workingDesign.baseVariant.copy(
                elements = controller.state.workingDesign.baseVariant.elements.map {
                    if (it.elementId == ClockStudioDescriptor.TIME) it.copy(xDp = Float.NaN) else it
                }
            )
        )
        success(controller.restore(
            controller.state.storedDesign,
            invalid,
            controller.state.selectedVariantId,
            ClockStudioDescriptor.TIME
        ))
        failure(controller.save())
        assertEquals(0, repository.saveCalls)
        assertTrue(controller.state.validationIssues.isNotEmpty())
    }

    @Test fun `new battery controller starts clean and resets selected element`() {
        val battery = WidgetStudioEditorController(
            descriptor = BatteryStudioDescriptor.create(),
            loadDesign = {
                StudioRepositoryResult.Failure(
                    StudioRepositoryError(StudioRepositoryErrorKind.NOT_FOUND, "Not found")
                )
            },
            saveDesign = { StudioRepositoryResult.Success(it.copy(revision = it.revision + 1L)) }
        )
        success(battery.initialize())

        assertEquals(BatteryStudioDescriptor.WIDGET_ID, battery.state.selectedWidgetId)
        assertEquals(BatteryStudioDescriptor.PERCENTAGE, battery.state.selectedElementId)
        assertEquals(twoByOne, battery.state.selectedVariantId)
        assertFalse(battery.state.dirty)
    }

    private fun controller(repository: FakeRepository = FakeRepository()): WidgetStudioEditorController =
        WidgetStudioEditorController(
            descriptor = ClockStudioDescriptor.create(),
            loadDesign = repository::load,
            saveDesign = repository::save
        ).also { success(it.initialize()) }

    private fun WidgetStudioEditorController.element(elementId: String) =
        state.resolvedDesign!!.elements.first { it.elementId == elementId }

    private fun success(result: EditorActionResult) {
        assertTrue("Expected success but was $result", result is EditorActionResult.Success)
    }

    private fun failure(result: EditorActionResult) {
        assertTrue("Expected failure but was $result", result is EditorActionResult.Failure)
    }

    private class FakeRepository {
        var persisted: WidgetDesignSpec? = null
        var saveCalls = 0

        fun load(widgetId: String): StudioRepositoryResult<WidgetDesignSpec> =
            persisted?.takeIf { it.widgetId == widgetId }?.let { StudioRepositoryResult.Success(it) }
                ?: StudioRepositoryResult.Failure(
                    StudioRepositoryError(StudioRepositoryErrorKind.NOT_FOUND, "Not found")
                )

        fun save(design: WidgetDesignSpec): StudioRepositoryResult<WidgetDesignSpec> {
            saveCalls++
            val revision = maxOf(persisted?.revision ?: 0L, design.revision) + 1L
            return StudioRepositoryResult.Success(design.copy(revision = revision).also { persisted = it })
        }
    }
}

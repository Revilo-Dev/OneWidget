package dev.revilodev.onewidget.studio.editor

import dev.revilodev.onewidget.studio.data.StudioRepositoryErrorKind
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.model.DimensionMode
import dev.revilodev.onewidget.studio.model.ElementCapability
import dev.revilodev.onewidget.studio.model.ResolvedWidgetDesign
import dev.revilodev.onewidget.studio.model.ValidationIssue
import dev.revilodev.onewidget.studio.model.WidgetDesignResolver
import dev.revilodev.onewidget.studio.model.WidgetDesignSpec
import dev.revilodev.onewidget.studio.model.WidgetDesignValidator
import dev.revilodev.onewidget.studio.model.WidgetElementSpec
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.registry.StudioWidgetDescriptor
import java.util.ArrayDeque

class WidgetStudioEditorController(
    val descriptor: StudioWidgetDescriptor,
    private val loadDesign: (String) -> StudioRepositoryResult<WidgetDesignSpec>,
    private val saveDesign: (WidgetDesignSpec) -> StudioRepositoryResult<WidgetDesignSpec>,
    private val historyCapacity: Int = DEFAULT_HISTORY_CAPACITY
) {
    init {
        require(historyCapacity >= DEFAULT_HISTORY_CAPACITY) {
            "Editor history must retain at least $DEFAULT_HISTORY_CAPACITY operations"
        }
    }

    private data class HistoryEntry(
        val label: String,
        val before: WidgetDesignSpec,
        val after: WidgetDesignSpec
    )

    private data class DragSession(
        val elementId: String,
        val before: WidgetDesignSpec
    )

    private val undoHistory = ArrayDeque<HistoryEntry>()
    private val redoHistory = ArrayDeque<HistoryEntry>()
    private var dragSession: DragSession? = null
    private var storedDesign = descriptor.defaultDesign
    private var workingDesign = descriptor.defaultDesign
    private var selectedVariantId: WidgetVariantId = defaultVariant()
    private var selectedElementId: String? = descriptor.elements.firstOrNull()?.elementId
    private var validationIssues: List<ValidationIssue> = emptyList()

    val state: WidgetStudioEditorState
        get() = WidgetStudioEditorState(
            selectedWidgetId = descriptor.widgetId,
            selectedVariantId = selectedVariantId,
            storedDesign = storedDesign,
            workingDesign = workingDesign,
            resolvedDesign = resolve(workingDesign, selectedVariantId),
            selectedElementId = selectedElementId,
            dirty = workingDesign != storedDesign,
            canUndo = undoHistory.isNotEmpty(),
            canRedo = redoHistory.isNotEmpty(),
            validationIssues = validationIssues
        )

    fun initialize(preferredVariantId: WidgetVariantId? = null): EditorActionResult {
        preferredVariantId?.takeIf { it in descriptor.supportedVariants }?.let { selectedVariantId = it }
        return when (val loaded = loadDesign(descriptor.widgetId)) {
            is StudioRepositoryResult.Success -> {
                storedDesign = loaded.value
                workingDesign = loaded.value
                clearHistory()
                EditorActionResult.Success(changed = false)
            }
            is StudioRepositoryResult.Failure -> {
                storedDesign = descriptor.defaultDesign
                workingDesign = descriptor.defaultDesign
                clearHistory()
                if (loaded.error.kind == StudioRepositoryErrorKind.NOT_FOUND) {
                    EditorActionResult.Success(changed = false)
                } else {
                    EditorActionResult.Failure(loaded.error.message)
                }
            }
        }
    }

    fun restore(
        restoredStoredDesign: WidgetDesignSpec,
        restoredWorkingDesign: WidgetDesignSpec,
        restoredVariantId: WidgetVariantId,
        restoredElementId: String?
    ): EditorActionResult {
        if (restoredStoredDesign.widgetId != descriptor.widgetId ||
            restoredWorkingDesign.widgetId != descriptor.widgetId ||
            restoredVariantId !in descriptor.supportedVariants
        ) {
            return EditorActionResult.Failure("Saved editor state does not match the registered widget")
        }
        storedDesign = restoredStoredDesign
        workingDesign = restoredWorkingDesign
        selectedVariantId = restoredVariantId
        selectedElementId = restoredElementId?.takeIf(::isKnownElement)
            ?: descriptor.elements.firstOrNull()?.elementId
        validationIssues = emptyList()
        clearHistory()
        return EditorActionResult.Success(changed = false)
    }

    fun selectVariant(variantId: WidgetVariantId): Boolean {
        if (variantId !in descriptor.supportedVariants) return false
        selectedVariantId = variantId
        validationIssues = emptyList()
        return true
    }

    fun selectElement(elementId: String?): Boolean {
        if (elementId != null && !isKnownElement(elementId)) return false
        selectedElementId = elementId
        return true
    }

    fun selectedElement(): WidgetElementSpec? =
        state.resolvedDesign?.elements?.firstOrNull { it.elementId == selectedElementId }

    fun execute(command: WidgetStudioEditCommand): EditorActionResult {
        if (dragSession != null) endDrag()
        val before = workingDesign
        val mutation = mutate(before, command)
        if (mutation is Mutation.Failure) return EditorActionResult.Failure(mutation.message)
        val after = (mutation as Mutation.Success).design
        if (after == before) return EditorActionResult.Success(changed = false)
        workingDesign = after
        pushUndo(HistoryEntry(command.label, before, after))
        redoHistory.clear()
        validationIssues = emptyList()
        return EditorActionResult.Success()
    }

    fun beginDrag(elementId: String): EditorActionResult {
        if (!isKnownElement(elementId)) return EditorActionResult.Failure("Unknown element: $elementId")
        if (!hasCapability(elementId, ElementCapability.POSITION)) {
            return EditorActionResult.Failure("Position is not supported for $elementId")
        }
        dragSession?.let { endDrag() }
        selectedElementId = elementId
        dragSession = DragSession(elementId, workingDesign)
        return EditorActionResult.Success(changed = false)
    }

    fun updateDrag(xDp: Float, yDp: Float): EditorActionResult {
        val session = dragSession ?: return EditorActionResult.Failure("No drag is active")
        if (!xDp.isFinite() || !yDp.isFinite()) return EditorActionResult.Failure("Position must be finite")
        val mutation = mutateElement(workingDesign, session.elementId) { it.copy(xDp = xDp, yDp = yDp) }
        if (mutation is Mutation.Failure) return EditorActionResult.Failure(mutation.message)
        workingDesign = (mutation as Mutation.Success).design
        validationIssues = emptyList()
        return EditorActionResult.Success()
    }

    fun endDrag(): EditorActionResult {
        val session = dragSession ?: return EditorActionResult.Success(changed = false)
        dragSession = null
        if (session.before == workingDesign) return EditorActionResult.Success(changed = false)
        pushUndo(HistoryEntry("Move element", session.before, workingDesign))
        redoHistory.clear()
        return EditorActionResult.Success()
    }

    fun cancelDrag() {
        dragSession?.let { workingDesign = it.before }
        dragSession = null
    }

    fun undo(): Boolean {
        if (dragSession != null) endDrag()
        val entry = undoHistory.pollLast() ?: return false
        workingDesign = entry.before
        redoHistory.addLast(entry)
        validationIssues = emptyList()
        return true
    }

    fun redo(): Boolean {
        val entry = redoHistory.pollLast() ?: return false
        workingDesign = entry.after
        pushUndo(entry)
        validationIssues = emptyList()
        return true
    }

    fun save(): EditorActionResult {
        if (dragSession != null) endDrag()
        val validation = WidgetDesignValidator.validate(workingDesign, descriptor.requiredElementIds)
        validationIssues = validation.issues
        if (!validation.isValid) {
            selectElementFromIssues(validation.issues)
            return EditorActionResult.Failure("The design contains validation errors", validation.issues)
        }
        return when (val saved = saveDesign(workingDesign)) {
            is StudioRepositoryResult.Success -> {
                storedDesign = saved.value
                workingDesign = saved.value
                validationIssues = emptyList()
                EditorActionResult.Success()
            }
            is StudioRepositoryResult.Failure -> EditorActionResult.Failure(saved.error.message)
        }
    }

    fun reload(): EditorActionResult = when (val loaded = loadDesign(descriptor.widgetId)) {
        is StudioRepositoryResult.Success -> {
            storedDesign = loaded.value
            workingDesign = loaded.value
            validationIssues = emptyList()
            clearHistory()
            EditorActionResult.Success()
        }
        is StudioRepositoryResult.Failure -> EditorActionResult.Failure(loaded.error.message)
    }

    fun targetVariantHasOverrides(): Boolean =
        workingDesign.variants[selectedVariantId]?.elementOverrides?.isNotEmpty() == true

    fun undoDepth(): Int = undoHistory.size
    fun redoDepth(): Int = redoHistory.size

    private sealed class Mutation {
        data class Success(val design: WidgetDesignSpec) : Mutation()
        data class Failure(val message: String) : Mutation()
    }

    private fun mutate(design: WidgetDesignSpec, command: WidgetStudioEditCommand): Mutation = when (command) {
        is WidgetStudioEditCommand.MoveElement -> {
            if (!command.xDp.isFinite() || !command.yDp.isFinite()) Mutation.Failure("Position must be finite")
            else withCapability(command.elementId, ElementCapability.POSITION) {
                mutateElement(design, command.elementId) { it.copy(xDp = command.xDp, yDp = command.yDp) }
            }
        }
        is WidgetStudioEditCommand.SetVisibility -> withCapability(command.elementId, ElementCapability.VISIBILITY) {
            mutateElement(design, command.elementId) { it.copy(visible = command.visible) }
        }
        is WidgetStudioEditCommand.SetPadding -> {
            val values = listOf(command.startDp, command.topDp, command.endDp, command.bottomDp)
            if (values.any { !it.isFinite() || it < 0f }) Mutation.Failure("Padding must be finite and non-negative")
            else withCapability(command.elementId, ElementCapability.PADDING) {
                mutateElement(design, command.elementId) {
                    it.copy(
                        paddingStartDp = command.startDp,
                        paddingTopDp = command.topDp,
                        paddingEndDp = command.endDp,
                        paddingBottomDp = command.bottomDp
                    )
                }
            }
        }
        is WidgetStudioEditCommand.SetAnchor -> {
            if (!command.canvasWidthDp.isFinite() || !command.canvasHeightDp.isFinite() ||
                command.canvasWidthDp <= 0f || command.canvasHeightDp <= 0f
            ) {
                Mutation.Failure("Canvas dimensions must be positive and finite")
            } else withCapability(command.elementId, ElementCapability.POSITION) {
                mutateElement(design, command.elementId) { element ->
                    val position = AnchorPositioning.preservePosition(
                        element.anchor,
                        command.anchor,
                        element.xDp,
                        element.yDp,
                        command.canvasWidthDp,
                        command.canvasHeightDp
                    )
                    element.copy(anchor = command.anchor, xDp = position.x, yDp = position.y)
                }
            }
        }
        is WidgetStudioEditCommand.SetAlignment -> withCapability(command.elementId, ElementCapability.ALIGNMENT) {
            mutateElement(design, command.elementId) {
                it.copy(
                    horizontalAlignment = command.horizontal,
                    verticalAlignment = command.vertical
                )
            }
        }
        is WidgetStudioEditCommand.SetWidthMode -> withCapability(command.elementId, ElementCapability.SIZE) {
            mutateElement(design, command.elementId) {
                it.copy(
                    widthMode = command.mode,
                    widthDp = if (command.mode == DimensionMode.FIXED && it.widthDp <= 0f) {
                        DEFAULT_FIXED_WIDTH_DP
                    } else it.widthDp
                )
            }
        }
        is WidgetStudioEditCommand.SetHeightMode -> withCapability(command.elementId, ElementCapability.SIZE) {
            mutateElement(design, command.elementId) {
                it.copy(
                    heightMode = command.mode,
                    heightDp = if (command.mode == DimensionMode.FIXED && it.heightDp <= 0f) {
                        DEFAULT_FIXED_HEIGHT_DP
                    } else it.heightDp
                )
            }
        }
        is WidgetStudioEditCommand.SetFixedWidth -> {
            if (!command.widthDp.isFinite() || command.widthDp <= 0f) {
                Mutation.Failure("Fixed width must be positive and finite")
            } else withCapability(command.elementId, ElementCapability.SIZE) {
                mutateElement(design, command.elementId) { it.copy(widthDp = command.widthDp) }
            }
        }
        is WidgetStudioEditCommand.SetFixedHeight -> {
            if (!command.heightDp.isFinite() || command.heightDp <= 0f) {
                Mutation.Failure("Fixed height must be positive and finite")
            } else withCapability(command.elementId, ElementCapability.SIZE) {
                mutateElement(design, command.elementId) { it.copy(heightDp = command.heightDp) }
            }
        }
        is WidgetStudioEditCommand.SetCornerRadius -> {
            if (!command.radiusDp.isFinite() || command.radiusDp < 0f) {
                Mutation.Failure("Corner radius must be finite and non-negative")
            } else withCapability(command.elementId, ElementCapability.CORNER_RADIUS) {
                mutateElement(design, command.elementId) { it.copy(cornerRadiusDp = command.radiusDp) }
            }
        }
        is WidgetStudioEditCommand.CopyVariant -> {
            if (command.sourceVariantId !in descriptor.supportedVariants) {
                Mutation.Failure("Unsupported source variant: ${command.sourceVariantId.stableId}")
            } else {
                val source = resolve(design, command.sourceVariantId)
                if (source == null) Mutation.Failure("Unable to resolve source variant")
                else Mutation.Success(
                    WidgetVariantOverrideEditor.copyResolvedVariant(design, source, selectedVariantId)
                )
            }
        }
        is WidgetStudioEditCommand.ResetElement -> Mutation.Success(
            WidgetVariantOverrideEditor.resetElement(design, selectedVariantId, command.elementId)
        )
        WidgetStudioEditCommand.ResetVariant -> Mutation.Success(
            WidgetVariantOverrideEditor.resetVariant(design, selectedVariantId)
        )
        WidgetStudioEditCommand.ResetWidget -> Mutation.Success(
            descriptor.defaultDesign.copy(revision = design.revision)
        )
    }

    private fun mutateElement(
        design: WidgetDesignSpec,
        elementId: String,
        transform: (WidgetElementSpec) -> WidgetElementSpec
    ): Mutation {
        if (!isKnownElement(elementId)) return Mutation.Failure("Unknown element: $elementId")
        val resolved = resolve(design, selectedVariantId)
            ?: return Mutation.Failure("Unable to resolve ${selectedVariantId.stableId}")
        val current = resolved.elements.firstOrNull { it.elementId == elementId }
            ?: return Mutation.Failure("Element is missing from the design: $elementId")
        return Mutation.Success(
            WidgetVariantOverrideEditor.updateElement(design, selectedVariantId, transform(current))
        )
    }

    private inline fun withCapability(
        elementId: String,
        capability: ElementCapability,
        mutation: () -> Mutation
    ): Mutation = if (hasCapability(elementId, capability)) mutation()
    else Mutation.Failure("$capability is not supported for $elementId")

    private fun hasCapability(elementId: String, capability: ElementCapability): Boolean =
        descriptor.elements.firstOrNull { it.elementId == elementId }?.capabilities?.contains(capability) == true

    private fun isKnownElement(elementId: String): Boolean =
        descriptor.elements.any { it.elementId == elementId } &&
            workingDesign.baseVariant.elements.any { it.elementId == elementId }

    private fun resolve(design: WidgetDesignSpec, variantId: WidgetVariantId): ResolvedWidgetDesign? =
        WidgetDesignResolver.resolve(design, variantId).design

    private fun defaultVariant(): WidgetVariantId =
        WidgetVariantId.Standard(2, 1).takeIf { it in descriptor.supportedVariants }
            ?: descriptor.supportedVariants.sortedBy { it.stableId }.first()

    private fun pushUndo(entry: HistoryEntry) {
        undoHistory.addLast(entry)
        while (undoHistory.size > historyCapacity) undoHistory.removeFirst()
    }

    private fun clearHistory() {
        undoHistory.clear()
        redoHistory.clear()
        dragSession = null
    }

    private fun selectElementFromIssues(issues: List<ValidationIssue>) {
        selectedElementId = descriptor.elements.firstOrNull { descriptorElement ->
            issues.any { it.field.contains(descriptorElement.elementId) }
        }?.elementId ?: selectedElementId
    }

    companion object {
        const val DEFAULT_HISTORY_CAPACITY = 50
        private const val DEFAULT_FIXED_WIDTH_DP = 64f
        private const val DEFAULT_FIXED_HEIGHT_DP = 32f
    }
}

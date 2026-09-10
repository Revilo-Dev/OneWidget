package dev.revilodev.onewidget.studio.editor

import dev.revilodev.onewidget.studio.model.ElementCapability

data class StudioInspectorSections(
    val visibility: Boolean,
    val position: Boolean,
    val size: Boolean,
    val padding: Boolean,
    val alignment: Boolean,
    val cornerRadius: Boolean
) {
    companion object {
        fun from(capabilities: Set<ElementCapability>) = StudioInspectorSections(
            visibility = ElementCapability.VISIBILITY in capabilities,
            position = ElementCapability.POSITION in capabilities,
            size = ElementCapability.SIZE in capabilities,
            padding = ElementCapability.PADDING in capabilities,
            alignment = ElementCapability.ALIGNMENT in capabilities,
            cornerRadius = ElementCapability.CORNER_RADIUS in capabilities
        )
    }
}

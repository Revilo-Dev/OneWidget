package dev.revilodev.onewidget.studio.editor

import dev.revilodev.onewidget.studio.model.DimensionMode
import dev.revilodev.onewidget.studio.model.HorizontalAlignment
import dev.revilodev.onewidget.studio.model.VerticalAlignment
import dev.revilodev.onewidget.studio.model.WidgetAnchor
import dev.revilodev.onewidget.studio.model.WidgetVariantId

sealed interface WidgetStudioEditCommand {
    val label: String

    data class MoveElement(val elementId: String, val xDp: Float, val yDp: Float) : WidgetStudioEditCommand {
        override val label = "Move element"
    }

    data class SetVisibility(val elementId: String, val visible: Boolean) : WidgetStudioEditCommand {
        override val label = "Set visibility"
    }

    data class SetPadding(
        val elementId: String,
        val startDp: Float,
        val topDp: Float,
        val endDp: Float,
        val bottomDp: Float
    ) : WidgetStudioEditCommand {
        override val label = "Set padding"
    }

    data class SetAnchor(
        val elementId: String,
        val anchor: WidgetAnchor,
        val canvasWidthDp: Float,
        val canvasHeightDp: Float
    ) : WidgetStudioEditCommand {
        override val label = "Set anchor"
    }

    data class SetAlignment(
        val elementId: String,
        val horizontal: HorizontalAlignment,
        val vertical: VerticalAlignment
    ) : WidgetStudioEditCommand {
        override val label = "Set alignment"
    }

    data class SetWidthMode(val elementId: String, val mode: DimensionMode) : WidgetStudioEditCommand {
        override val label = "Set width mode"
    }

    data class SetHeightMode(val elementId: String, val mode: DimensionMode) : WidgetStudioEditCommand {
        override val label = "Set height mode"
    }

    data class SetFixedWidth(val elementId: String, val widthDp: Float) : WidgetStudioEditCommand {
        override val label = "Set fixed width"
    }

    data class SetFixedHeight(val elementId: String, val heightDp: Float) : WidgetStudioEditCommand {
        override val label = "Set fixed height"
    }

    data class SetCornerRadius(val elementId: String, val radiusDp: Float) : WidgetStudioEditCommand {
        override val label = "Set corner radius"
    }

    data class CopyVariant(val sourceVariantId: WidgetVariantId) : WidgetStudioEditCommand {
        override val label = "Copy variant"
    }

    data class ResetElement(val elementId: String) : WidgetStudioEditCommand {
        override val label = "Reset element"
    }

    data object ResetVariant : WidgetStudioEditCommand {
        override val label = "Reset variant"
    }

    data object ResetWidget : WidgetStudioEditCommand {
        override val label = "Reset widget"
    }
}

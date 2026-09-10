package dev.revilodev.onewidget.studio.model

const val WIDGET_DESIGN_SCHEMA_VERSION = 1

enum class DimensionMode { WRAP, MATCH, FIXED }

enum class WidgetAnchor {
    TOP_START,
    TOP_CENTER,
    TOP_END,
    CENTER_START,
    CENTER,
    CENTER_END,
    BOTTOM_START,
    BOTTOM_CENTER,
    BOTTOM_END
}

enum class HorizontalAlignment { START, CENTER, END }
enum class VerticalAlignment { TOP, CENTER, BOTTOM }

enum class ElementCapability {
    VISIBILITY,
    POSITION,
    SIZE,
    PADDING,
    ALIGNMENT,
    CORNER_RADIUS,
    Z_ORDER
}

data class WidgetElementSpec(
    val elementId: String,
    val visible: Boolean = true,
    val xDp: Float = 0f,
    val yDp: Float = 0f,
    val widthMode: DimensionMode = DimensionMode.WRAP,
    val heightMode: DimensionMode = DimensionMode.WRAP,
    val widthDp: Float = 0f,
    val heightDp: Float = 0f,
    val paddingStartDp: Float = 0f,
    val paddingTopDp: Float = 0f,
    val paddingEndDp: Float = 0f,
    val paddingBottomDp: Float = 0f,
    val anchor: WidgetAnchor = WidgetAnchor.TOP_START,
    val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.START,
    val verticalAlignment: VerticalAlignment = VerticalAlignment.TOP,
    val zIndex: Int = 0,
    val cornerRadiusDp: Float = 0f
)

data class WidgetVariantSpec(
    val elements: List<WidgetElementSpec>
)

/** Null fields inherit their value from the base element. */
data class WidgetElementOverride(
    val visible: Boolean? = null,
    val xDp: Float? = null,
    val yDp: Float? = null,
    val widthMode: DimensionMode? = null,
    val heightMode: DimensionMode? = null,
    val widthDp: Float? = null,
    val heightDp: Float? = null,
    val paddingStartDp: Float? = null,
    val paddingTopDp: Float? = null,
    val paddingEndDp: Float? = null,
    val paddingBottomDp: Float? = null,
    val anchor: WidgetAnchor? = null,
    val horizontalAlignment: HorizontalAlignment? = null,
    val verticalAlignment: VerticalAlignment? = null,
    val zIndex: Int? = null,
    val cornerRadiusDp: Float? = null
)

data class WidgetVariantOverride(
    val elementOverrides: Map<String, WidgetElementOverride> = emptyMap()
)

data class WidgetDesignSpec(
    val schemaVersion: Int = WIDGET_DESIGN_SCHEMA_VERSION,
    val widgetId: String,
    val revision: Long = 0L,
    val baseVariant: WidgetVariantSpec,
    val variants: Map<WidgetVariantId, WidgetVariantOverride> = emptyMap()
)

data class ResolvedWidgetDesign(
    val schemaVersion: Int,
    val widgetId: String,
    val revision: Long,
    val variantId: WidgetVariantId,
    val elements: List<WidgetElementSpec>
)

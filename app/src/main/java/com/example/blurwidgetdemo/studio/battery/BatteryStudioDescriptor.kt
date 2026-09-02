package com.example.blurwidgetdemo.studio.battery

import com.example.blurwidgetdemo.studio.model.DimensionMode
import com.example.blurwidgetdemo.studio.model.ElementCapability
import com.example.blurwidgetdemo.studio.model.HorizontalAlignment
import com.example.blurwidgetdemo.studio.model.VerticalAlignment
import com.example.blurwidgetdemo.studio.model.WidgetAnchor
import com.example.blurwidgetdemo.studio.model.WidgetDesignSpec
import com.example.blurwidgetdemo.studio.model.WidgetElementOverride
import com.example.blurwidgetdemo.studio.model.WidgetElementSpec
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import com.example.blurwidgetdemo.studio.model.WidgetVariantOverride
import com.example.blurwidgetdemo.studio.model.WidgetVariantSpec
import com.example.blurwidgetdemo.studio.registry.StudioElementDescriptor
import com.example.blurwidgetdemo.studio.registry.StudioElementVisualType
import com.example.blurwidgetdemo.studio.registry.StudioWidgetDescriptor

object BatteryStudioDescriptor {
    const val WIDGET_ID = "battery"
    const val PERCENTAGE = "battery.percentage"
    const val STATUS = "battery.status"
    const val ICON = "battery.icon"
    const val PROGRESS = "battery.progress"

    private val POSITIONABLE = setOf(
        ElementCapability.VISIBILITY,
        ElementCapability.POSITION,
        ElementCapability.SIZE,
        ElementCapability.PADDING,
        ElementCapability.ALIGNMENT
    )

    fun create(): StudioWidgetDescriptor = StudioWidgetDescriptor(
        widgetId = WIDGET_ID,
        displayName = "Battery",
        supportedVariants = WidgetVariantId.standardVariants.toSet(),
        elements = listOf(
            StudioElementDescriptor(
                PERCENTAGE,
                "Percentage",
                POSITIONABLE,
                required = true,
                visualType = StudioElementVisualType.TEXT,
                sampleText = "72%",
                wrapWidthDp = 78f,
                wrapHeightDp = 48f
            ),
            StudioElementDescriptor(
                STATUS,
                "Status",
                POSITIONABLE,
                visualType = StudioElementVisualType.TEXT,
                sampleText = "Charging",
                wrapWidthDp = 84f,
                wrapHeightDp = 28f
            ),
            StudioElementDescriptor(
                ICON,
                "Battery icon",
                POSITIONABLE - ElementCapability.ALIGNMENT,
                visualType = StudioElementVisualType.ICON,
                wrapWidthDp = 32f,
                wrapHeightDp = 32f
            ),
            StudioElementDescriptor(
                PROGRESS,
                "Progress bar",
                POSITIONABLE + ElementCapability.CORNER_RADIUS,
                visualType = StudioElementVisualType.PROGRESS,
                wrapWidthDp = 160f,
                wrapHeightDp = 24f
            )
        ),
        defaultDesign = defaultDesign(),
        renderer = BatteryStudioRemoteViewsRenderer,
        sampleDataProvider = { mapOf("percentage" to "72%", "status" to "Charging") }
    )

    /** Conservative single-device representation of the current compact/large Battery content. */
    fun defaultDesign(): WidgetDesignSpec {
        val compact = WidgetVariantOverride(
            mapOf(
                STATUS to WidgetElementOverride(visible = false),
                ICON to WidgetElementOverride(
                    anchor = WidgetAnchor.CENTER_START,
                    xDp = 26f,
                    yDp = 0f
                ),
                PERCENTAGE to WidgetElementOverride(yDp = 0f),
                PROGRESS to WidgetElementOverride(
                    anchor = WidgetAnchor.CENTER,
                    yDp = 0f,
                    heightDp = 32f,
                    cornerRadiusDp = 16f
                )
            )
        )
        val large = WidgetVariantOverride(
            mapOf(
                PERCENTAGE to WidgetElementOverride(yDp = -22f),
                STATUS to WidgetElementOverride(visible = true, yDp = 14f),
                ICON to WidgetElementOverride(
                    anchor = WidgetAnchor.CENTER_START,
                    xDp = 32f,
                    yDp = -4f
                ),
                PROGRESS to WidgetElementOverride(
                    anchor = WidgetAnchor.BOTTOM_CENTER,
                    yDp = -24f,
                    heightDp = 26f,
                    cornerRadiusDp = 13f
                )
            )
        )
        return WidgetDesignSpec(
            widgetId = WIDGET_ID,
            baseVariant = WidgetVariantSpec(
                listOf(
                    WidgetElementSpec(
                        elementId = PERCENTAGE,
                        visible = true,
                        yDp = -18f,
                        widthMode = DimensionMode.WRAP,
                        heightMode = DimensionMode.WRAP,
                        anchor = WidgetAnchor.CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        verticalAlignment = VerticalAlignment.CENTER,
                        zIndex = 2
                    ),
                    WidgetElementSpec(
                        elementId = STATUS,
                        visible = true,
                        yDp = 18f,
                        widthMode = DimensionMode.WRAP,
                        heightMode = DimensionMode.WRAP,
                        anchor = WidgetAnchor.CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        verticalAlignment = VerticalAlignment.CENTER,
                        zIndex = 3
                    ),
                    WidgetElementSpec(
                        elementId = ICON,
                        visible = true,
                        xDp = 26f,
                        yDp = 26f,
                        widthMode = DimensionMode.FIXED,
                        heightMode = DimensionMode.FIXED,
                        widthDp = 32f,
                        heightDp = 32f,
                        anchor = WidgetAnchor.TOP_START,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        verticalAlignment = VerticalAlignment.CENTER,
                        zIndex = 4
                    ),
                    WidgetElementSpec(
                        elementId = PROGRESS,
                        visible = true,
                        yDp = -18f,
                        widthMode = DimensionMode.MATCH,
                        heightMode = DimensionMode.FIXED,
                        heightDp = 24f,
                        paddingStartDp = 16f,
                        paddingEndDp = 16f,
                        anchor = WidgetAnchor.BOTTOM_CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        verticalAlignment = VerticalAlignment.CENTER,
                        zIndex = 1,
                        cornerRadiusDp = 12f
                    )
                )
            ),
            variants = buildMap {
                put(WidgetVariantId.Standard(1, 1), compact)
                put(WidgetVariantId.Standard(2, 1), compact)
                put(WidgetVariantId.Standard(2, 2), large)
                put(WidgetVariantId.Standard(4, 2), large)
                put(WidgetVariantId.Standard(5, 5), large)
            }
        )
    }
}

package com.example.blurwidgetdemo.studio.calendar

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

object CalendarStudioDescriptor {
    const val WIDGET_ID = "calendar"
    const val TITLE = "calendar.title"
    const val DETAILS = "calendar.details"

    private val TEXT_CAPABILITIES = setOf(
        ElementCapability.VISIBILITY,
        ElementCapability.POSITION,
        ElementCapability.SIZE,
        ElementCapability.PADDING,
        ElementCapability.ALIGNMENT
    )

    fun create(): StudioWidgetDescriptor = StudioWidgetDescriptor(
        widgetId = WIDGET_ID,
        displayName = "Next event",
        supportedVariants = WidgetVariantId.standardVariants.toSet(),
        elements = listOf(
            StudioElementDescriptor(
                elementId = TITLE,
                displayName = "Event title",
                capabilities = TEXT_CAPABILITIES,
                required = true,
                visualType = StudioElementVisualType.TEXT,
                sampleText = "Team catch-up",
                wrapWidthDp = 190f,
                wrapHeightDp = 42f
            ),
            StudioElementDescriptor(
                elementId = DETAILS,
                displayName = "Event details",
                capabilities = TEXT_CAPABILITIES,
                required = true,
                visualType = StudioElementVisualType.TEXT,
                sampleText = "Starts 10:00 AM",
                wrapWidthDp = 190f,
                wrapHeightDp = 28f
            )
        ),
        defaultDesign = defaultDesign(),
        renderer = CalendarStudioRemoteViewsRenderer,
        sampleDataProvider = {
            mapOf("title" to "Team catch-up", "details" to "Starts 10:00 AM")
        }
    )

    fun defaultDesign(): WidgetDesignSpec {
        val compact = WidgetVariantOverride(
            mapOf(
                TITLE to WidgetElementOverride(yDp = 0f),
                DETAILS to WidgetElementOverride(visible = false)
            )
        )
        return WidgetDesignSpec(
            widgetId = WIDGET_ID,
            baseVariant = WidgetVariantSpec(
                listOf(
                    WidgetElementSpec(
                        elementId = TITLE,
                        visible = true,
                        yDp = -20f,
                        widthMode = DimensionMode.MATCH,
                        heightMode = DimensionMode.WRAP,
                        paddingStartDp = 12f,
                        paddingEndDp = 12f,
                        anchor = WidgetAnchor.CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        verticalAlignment = VerticalAlignment.CENTER,
                        zIndex = 2
                    ),
                    WidgetElementSpec(
                        elementId = DETAILS,
                        visible = true,
                        yDp = 22f,
                        widthMode = DimensionMode.MATCH,
                        heightMode = DimensionMode.WRAP,
                        paddingStartDp = 12f,
                        paddingEndDp = 12f,
                        anchor = WidgetAnchor.CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        verticalAlignment = VerticalAlignment.CENTER,
                        zIndex = 1
                    )
                )
            ),
            variants = buildMap {
                (1..5).forEach { columns ->
                    put(WidgetVariantId.Standard(columns, 1), compact)
                }
            }
        )
    }
}

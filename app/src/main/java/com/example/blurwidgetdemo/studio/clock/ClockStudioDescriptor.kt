package dev.revilodev.onewidget.studio.clock

import dev.revilodev.onewidget.studio.model.DimensionMode
import dev.revilodev.onewidget.studio.model.ElementCapability
import dev.revilodev.onewidget.studio.model.HorizontalAlignment
import dev.revilodev.onewidget.studio.model.VerticalAlignment
import dev.revilodev.onewidget.studio.model.WidgetAnchor
import dev.revilodev.onewidget.studio.model.WidgetDesignSpec
import dev.revilodev.onewidget.studio.model.WidgetElementOverride
import dev.revilodev.onewidget.studio.model.WidgetElementSpec
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.model.WidgetVariantOverride
import dev.revilodev.onewidget.studio.model.WidgetVariantSpec
import dev.revilodev.onewidget.studio.registry.StudioElementDescriptor
import dev.revilodev.onewidget.studio.registry.StudioWidgetDescriptor

object ClockStudioDescriptor {
    const val WIDGET_ID = "digital_clock"
    const val TIME = "clock.time"
    const val DATE = "clock.date"
    const val DAY = "clock.day"
    const val BATTERY = "clock.battery"

    private val TEXT_CAPABILITIES = setOf(
        ElementCapability.VISIBILITY,
        ElementCapability.POSITION,
        ElementCapability.SIZE,
        ElementCapability.PADDING,
        ElementCapability.ALIGNMENT
    )

    fun create(): StudioWidgetDescriptor = StudioWidgetDescriptor(
        widgetId = WIDGET_ID,
        displayName = "Digital Clock",
        supportedVariants = WidgetVariantId.standardVariants.toSet(),
        elements = listOf(
            StudioElementDescriptor(TIME, "Time", TEXT_CAPABILITIES, required = true, sampleText = "12:45", wrapWidthDp = 92f),
            StudioElementDescriptor(DATE, "Date", TEXT_CAPABILITIES, sampleText = "31 Aug", wrapWidthDp = 68f),
            StudioElementDescriptor(DAY, "Day", TEXT_CAPABILITIES, sampleText = "Monday", wrapWidthDp = 76f),
            StudioElementDescriptor(BATTERY, "Phone battery", TEXT_CAPABILITIES, sampleText = "84%", wrapWidthDp = 54f)
        ),
        defaultDesign = defaultDesign(),
        renderer = ClockStudioRemoteViewsRenderer
    )

    /**
     * Conservative geometry matching the current medium layout, with sparse overrides only for
     * production sizes already represented in the app gallery. These values do not affect the
     * production renderer in Phase 1.
     */
    fun defaultDesign(): WidgetDesignSpec {
        val compact = WidgetVariantOverride(
            mapOf(
                TIME to WidgetElementOverride(yDp = -8f, paddingStartDp = 18f, paddingEndDp = 18f),
                DATE to WidgetElementOverride(
                    yDp = 20f,
                    paddingStartDp = 18f,
                    paddingEndDp = 18f,
                    horizontalAlignment = HorizontalAlignment.END
                ),
                DAY to WidgetElementOverride(visible = false)
            )
        )
        val large = WidgetVariantOverride(
            mapOf(
                TIME to WidgetElementOverride(
                    yDp = -28f,
                    paddingStartDp = 26f,
                    paddingTopDp = 18f,
                    paddingEndDp = 26f,
                    paddingBottomDp = 18f
                ),
                DAY to WidgetElementOverride(visible = true, yDp = 18f),
                DATE to WidgetElementOverride(yDp = 42f)
            )
        )
        return WidgetDesignSpec(
            widgetId = WIDGET_ID,
            baseVariant = WidgetVariantSpec(
                listOf(
                    WidgetElementSpec(
                        elementId = TIME,
                        visible = true,
                        yDp = -14f,
                        widthMode = DimensionMode.MATCH,
                        heightMode = DimensionMode.WRAP,
                        paddingStartDp = 22f,
                        paddingTopDp = 14f,
                        paddingEndDp = 22f,
                        paddingBottomDp = 14f,
                        anchor = WidgetAnchor.CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        verticalAlignment = VerticalAlignment.CENTER,
                        zIndex = 0
                    ),
                    WidgetElementSpec(
                        elementId = DATE,
                        visible = true,
                        yDp = 24f,
                        widthMode = DimensionMode.MATCH,
                        heightMode = DimensionMode.WRAP,
                        paddingStartDp = 22f,
                        paddingEndDp = 22f,
                        anchor = WidgetAnchor.CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        verticalAlignment = VerticalAlignment.CENTER,
                        zIndex = 1
                    ),
                    WidgetElementSpec(
                        elementId = DAY,
                        visible = false,
                        yDp = 42f,
                        widthMode = DimensionMode.MATCH,
                        heightMode = DimensionMode.WRAP,
                        paddingStartDp = 22f,
                        paddingEndDp = 22f,
                        anchor = WidgetAnchor.CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        verticalAlignment = VerticalAlignment.CENTER,
                        zIndex = 2
                    ),
                    WidgetElementSpec(
                        elementId = BATTERY,
                        visible = false,
                        yDp = -10f,
                        widthMode = DimensionMode.WRAP,
                        heightMode = DimensionMode.WRAP,
                        anchor = WidgetAnchor.BOTTOM_CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        verticalAlignment = VerticalAlignment.CENTER,
                        zIndex = 3
                    )
                )
            ),
            variants = mapOf(
                WidgetVariantId.Standard(1, 1) to compact,
                WidgetVariantId.Standard(2, 1) to compact,
                WidgetVariantId.Standard(4, 2) to large,
                WidgetVariantId.Standard(4, 4) to large
            )
        )
    }
}

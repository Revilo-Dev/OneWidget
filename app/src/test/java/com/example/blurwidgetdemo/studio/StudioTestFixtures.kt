package dev.revilodev.onewidget.studio

import dev.revilodev.onewidget.studio.model.DimensionMode
import dev.revilodev.onewidget.studio.model.HorizontalAlignment
import dev.revilodev.onewidget.studio.model.VerticalAlignment
import dev.revilodev.onewidget.studio.model.WidgetAnchor
import dev.revilodev.onewidget.studio.model.WidgetDesignSpec
import dev.revilodev.onewidget.studio.model.WidgetElementSpec
import dev.revilodev.onewidget.studio.model.WidgetVariantSpec

fun studioTestDesign(widgetId: String = "test_widget"): WidgetDesignSpec = WidgetDesignSpec(
    widgetId = widgetId,
    baseVariant = WidgetVariantSpec(
        listOf(
            WidgetElementSpec(
                elementId = "test.primary",
                visible = true,
                xDp = 4f,
                yDp = 8f,
                widthMode = DimensionMode.FIXED,
                heightMode = DimensionMode.WRAP,
                widthDp = 120f,
                paddingStartDp = 2f,
                paddingTopDp = 3f,
                paddingEndDp = 4f,
                paddingBottomDp = 5f,
                anchor = WidgetAnchor.CENTER,
                horizontalAlignment = HorizontalAlignment.CENTER,
                verticalAlignment = VerticalAlignment.CENTER,
                zIndex = 1,
                cornerRadiusDp = 6f
            ),
            WidgetElementSpec(elementId = "test.secondary", visible = true, zIndex = 2)
        )
    )
)

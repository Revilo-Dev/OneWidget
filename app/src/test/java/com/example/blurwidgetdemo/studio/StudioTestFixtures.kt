package com.example.blurwidgetdemo.studio

import com.example.blurwidgetdemo.studio.model.DimensionMode
import com.example.blurwidgetdemo.studio.model.HorizontalAlignment
import com.example.blurwidgetdemo.studio.model.VerticalAlignment
import com.example.blurwidgetdemo.studio.model.WidgetAnchor
import com.example.blurwidgetdemo.studio.model.WidgetDesignSpec
import com.example.blurwidgetdemo.studio.model.WidgetElementSpec
import com.example.blurwidgetdemo.studio.model.WidgetVariantSpec

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

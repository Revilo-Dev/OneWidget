package com.example.blurwidgetdemo.studio.storage

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

object StorageStudioDescriptor {
    const val WIDGET_ID = "storage"
    const val PRIMARY = "storage.primary"
    const val STORAGE = "storage.storage"
    const val MEMORY = "storage.memory"
    const val DATA = "storage.data"

    private val METRIC_ROW_CAPABILITIES = setOf(
        ElementCapability.VISIBILITY,
        ElementCapability.POSITION,
        ElementCapability.SIZE,
        ElementCapability.PADDING,
        ElementCapability.ALIGNMENT,
        ElementCapability.CORNER_RADIUS
    )

    fun create(): StudioWidgetDescriptor = StudioWidgetDescriptor(
        widgetId = WIDGET_ID,
        displayName = "Storage",
        supportedVariants = WidgetVariantId.standardVariants.toSet(),
        elements = listOf(
            metricDescriptor(PRIMARY, "Compact metric", "Storage  62% • 79GB/128GB", 160f, 40f),
            metricDescriptor(STORAGE, "Storage row", "Storage  62% • 79GB/128GB"),
            metricDescriptor(MEMORY, "Memory row", "Memory  58% • 5GB/8GB"),
            metricDescriptor(DATA, "Data row", "Data usage  34% • 3GB/10GB")
        ),
        defaultDesign = defaultDesign(),
        renderer = StorageStudioRemoteViewsRenderer,
        sampleDataProvider = {
            mapOf(
                "storage" to "62%",
                "memory" to "58%",
                "data" to "34%"
            )
        }
    )

    fun defaultDesign(): WidgetDesignSpec {
        val compact = WidgetVariantOverride(
            mapOf(
                PRIMARY to WidgetElementOverride(visible = true, yDp = 0f, heightDp = 40f),
                STORAGE to WidgetElementOverride(visible = false),
                MEMORY to WidgetElementOverride(visible = false),
                DATA to WidgetElementOverride(visible = false)
            )
        )
        return WidgetDesignSpec(
            widgetId = WIDGET_ID,
            baseVariant = WidgetVariantSpec(
                listOf(
                    metricElement(PRIMARY, visible = false, yDp = 0f, heightDp = 40f, zIndex = 4),
                    metricElement(STORAGE, visible = true, yDp = -40f, heightDp = 30f, zIndex = 3),
                    metricElement(MEMORY, visible = true, yDp = 0f, heightDp = 30f, zIndex = 2),
                    metricElement(DATA, visible = true, yDp = 40f, heightDp = 30f, zIndex = 1)
                )
            ),
            variants = buildMap {
                (1..5).forEach { columns ->
                    put(WidgetVariantId.Standard(columns, 1), compact)
                }
            }
        )
    }

    private fun metricDescriptor(
        elementId: String,
        displayName: String,
        sampleText: String,
        wrapWidthDp: Float = 180f,
        wrapHeightDp: Float = 30f
    ) = StudioElementDescriptor(
        elementId = elementId,
        displayName = displayName,
        capabilities = METRIC_ROW_CAPABILITIES,
        required = true,
        visualType = StudioElementVisualType.PROGRESS,
        sampleText = sampleText,
        wrapWidthDp = wrapWidthDp,
        wrapHeightDp = wrapHeightDp
    )

    private fun metricElement(
        elementId: String,
        visible: Boolean,
        yDp: Float,
        heightDp: Float,
        zIndex: Int
    ) = WidgetElementSpec(
        elementId = elementId,
        visible = visible,
        yDp = yDp,
        widthMode = DimensionMode.MATCH,
        heightMode = DimensionMode.FIXED,
        heightDp = heightDp,
        paddingStartDp = 16f,
        paddingEndDp = 16f,
        anchor = WidgetAnchor.CENTER,
        horizontalAlignment = HorizontalAlignment.CENTER,
        verticalAlignment = VerticalAlignment.CENTER,
        zIndex = zIndex,
        cornerRadiusDp = heightDp / 2f
    )
}

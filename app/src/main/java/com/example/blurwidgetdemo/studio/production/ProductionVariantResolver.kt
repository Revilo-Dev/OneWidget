package dev.revilodev.onewidget.studio.production

import dev.revilodev.onewidget.studio.battery.BatteryStudioDescriptor
import dev.revilodev.onewidget.studio.clock.ClockStudioDescriptor
import dev.revilodev.onewidget.studio.storage.StorageStudioDescriptor
import dev.revilodev.onewidget.studio.calendar.CalendarStudioDescriptor
import dev.revilodev.onewidget.studio.universal.UniversalSizeProfile
import dev.revilodev.onewidget.studio.universal.UniversalWidgetStudioCatalog
import dev.revilodev.onewidget.studio.model.ResolvedWidgetDesign
import dev.revilodev.onewidget.studio.model.WidgetDesignResolver
import dev.revilodev.onewidget.studio.model.WidgetDesignSpec
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.widgets.battery.BatteryRenderSize
import dev.revilodev.onewidget.widgets.clock.ClockWidgetLayout
import dev.revilodev.onewidget.widgets.storage.StorageRenderSize
import dev.revilodev.onewidget.widgets.calendar.CalendarRenderSize

enum class ProductionSizeCategory { COMPACT, MEDIUM, LARGE }

data class ProductionVariantSelection(
    val category: ProductionSizeCategory,
    val variantId: WidgetVariantId.Standard,
    val usedUnknownSizeFallback: Boolean = false
)

object ProductionVariantResolver {
    private val adaptiveMapping = mapOf(
        ProductionSizeCategory.COMPACT to WidgetVariantId.Standard(2, 1),
        ProductionSizeCategory.MEDIUM to WidgetVariantId.Standard(2, 2),
        ProductionSizeCategory.LARGE to WidgetVariantId.Standard(4, 2)
    )

    private val rowMapping = mapOf(
        ProductionSizeCategory.COMPACT to WidgetVariantId.Standard(1, 1),
        ProductionSizeCategory.MEDIUM to WidgetVariantId.Standard(2, 1),
        ProductionSizeCategory.LARGE to WidgetVariantId.Standard(4, 1)
    )

    private val gridMapping = mapOf(
        ProductionSizeCategory.COMPACT to WidgetVariantId.Standard(2, 2),
        ProductionSizeCategory.MEDIUM to WidgetVariantId.Standard(2, 2),
        ProductionSizeCategory.LARGE to WidgetVariantId.Standard(4, 2)
    )

    private val fixedSmallMapping = ProductionSizeCategory.entries.associateWith {
        WidgetVariantId.Standard(1, 1)
    }

    private val fixedWideMapping = ProductionSizeCategory.entries.associateWith {
        WidgetVariantId.Standard(2, 1)
    }

    fun mappingFor(widgetId: String): Map<ProductionSizeCategory, WidgetVariantId.Standard> =
        if (widgetId == ClockStudioDescriptor.WIDGET_ID ||
            widgetId == BatteryStudioDescriptor.WIDGET_ID ||
            widgetId == StorageStudioDescriptor.WIDGET_ID ||
            widgetId == CalendarStudioDescriptor.WIDGET_ID
        ) {
            adaptiveMapping
        } else {
            when (UniversalWidgetStudioCatalog.definition(widgetId)?.sizeProfile) {
                UniversalSizeProfile.ADAPTIVE -> adaptiveMapping
                UniversalSizeProfile.ROW -> rowMapping
                UniversalSizeProfile.GRID -> gridMapping
                UniversalSizeProfile.FIXED_SMALL -> fixedSmallMapping
                UniversalSizeProfile.FIXED_WIDE -> fixedWideMapping
                null -> emptyMap()
            }
        }

    fun resolve(widgetId: String, widthDp: Float, heightDp: Float): ProductionVariantSelection {
        if (!widthDp.isFinite() || !heightDp.isFinite() || widthDp <= 0f || heightDp <= 0f) {
            return ProductionVariantSelection(
                ProductionSizeCategory.MEDIUM,
                mappingFor(widgetId).getOrDefault(
                    ProductionSizeCategory.MEDIUM,
                    adaptiveMapping.getValue(ProductionSizeCategory.MEDIUM)
                ),
                usedUnknownSizeFallback = true
            )
        }
        val category = when (widgetId) {
            ClockStudioDescriptor.WIDGET_ID -> when (
                ClockWidgetLayout.category(widthDp.toInt(), heightDp.toInt())
            ) {
                ClockWidgetLayout.Category.COMPACT -> ProductionSizeCategory.COMPACT
                ClockWidgetLayout.Category.MEDIUM -> ProductionSizeCategory.MEDIUM
                ClockWidgetLayout.Category.LARGE -> ProductionSizeCategory.LARGE
            }
            BatteryStudioDescriptor.WIDGET_ID -> {
                val size = BatteryRenderSize(widthDp, heightDp)
                when {
                    size.compact -> ProductionSizeCategory.COMPACT
                    widthDp >= ClockWidgetLayout.LARGE_MIN_WIDTH_DP ||
                        heightDp >= ClockWidgetLayout.LARGE_MIN_HEIGHT_DP -> ProductionSizeCategory.LARGE
                    else -> ProductionSizeCategory.MEDIUM
                }
            }
            StorageStudioDescriptor.WIDGET_ID -> {
                val size = StorageRenderSize(widthDp, heightDp)
                when {
                    size.compact -> ProductionSizeCategory.COMPACT
                    widthDp >= ClockWidgetLayout.LARGE_MIN_WIDTH_DP ||
                        heightDp >= ClockWidgetLayout.LARGE_MIN_HEIGHT_DP -> ProductionSizeCategory.LARGE
                    else -> ProductionSizeCategory.MEDIUM
                }
            }
            CalendarStudioDescriptor.WIDGET_ID -> {
                val size = CalendarRenderSize(widthDp, heightDp)
                when {
                    size.compact -> ProductionSizeCategory.COMPACT
                    widthDp >= ClockWidgetLayout.LARGE_MIN_WIDTH_DP ||
                        heightDp >= ClockWidgetLayout.LARGE_MIN_HEIGHT_DP -> ProductionSizeCategory.LARGE
                    else -> ProductionSizeCategory.MEDIUM
                }
            }
            else -> when (UniversalWidgetStudioCatalog.definition(widgetId)?.sizeProfile) {
                UniversalSizeProfile.ADAPTIVE -> when {
                    heightDp <= 100f -> ProductionSizeCategory.COMPACT
                    widthDp >= ClockWidgetLayout.LARGE_MIN_WIDTH_DP ||
                        heightDp >= ClockWidgetLayout.LARGE_MIN_HEIGHT_DP -> ProductionSizeCategory.LARGE
                    else -> ProductionSizeCategory.MEDIUM
                }
                UniversalSizeProfile.ROW -> when {
                    widthDp <= 100f && heightDp <= 100f -> ProductionSizeCategory.COMPACT
                    widthDp >= ClockWidgetLayout.LARGE_MIN_WIDTH_DP -> ProductionSizeCategory.LARGE
                    else -> ProductionSizeCategory.MEDIUM
                }
                UniversalSizeProfile.GRID -> if (widthDp >= ClockWidgetLayout.LARGE_MIN_WIDTH_DP) {
                    ProductionSizeCategory.LARGE
                } else {
                    ProductionSizeCategory.MEDIUM
                }
                UniversalSizeProfile.FIXED_SMALL -> ProductionSizeCategory.COMPACT
                UniversalSizeProfile.FIXED_WIDE -> ProductionSizeCategory.MEDIUM
                null -> ProductionSizeCategory.MEDIUM
            }
        }
        val mapping = mappingFor(widgetId).ifEmpty { adaptiveMapping }
        return ProductionVariantSelection(category, mapping.getValue(category))
    }
}

object ProductionWidgetDesignResolver {
    fun resolve(
        mode: WidgetProductionDesignMode,
        promotedDesign: PromotedWidgetDesign?,
        widgetId: String,
        widthDp: Float,
        heightDp: Float
    ): ResolvedWidgetDesign? {
        if (mode != WidgetProductionDesignMode.PROMOTED_DEFAULT || promotedDesign == null) return null
        if (promotedDesign.design.widgetId != widgetId) return null
        val selection = ProductionVariantResolver.resolve(widgetId, widthDp, heightDp)
        return WidgetDesignResolver.resolve(promotedDesign.design, selection.variantId).design
    }
}

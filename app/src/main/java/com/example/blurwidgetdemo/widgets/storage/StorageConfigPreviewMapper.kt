package dev.revilodev.onewidget.widgets.storage

import dev.revilodev.onewidget.widgets.WidgetAppearance

/** Maps unsaved Storage controls without writing production preferences. */
object StorageConfigPreviewMapper {
    fun map(
        base: StorageWidgetConfiguration,
        surfaceColor: Int,
        textColor: Int,
        textScalePercent: Int,
        alignment: WidgetAppearance.Alignment,
        compactMetric: StorageWidget.CompactMetric,
        hideData: Boolean,
        showTitles: Boolean,
        detailMode: StorageWidget.DetailMode,
        colors: Map<StorageWidget.CompactMetric, Int>
    ): StorageWidgetConfiguration = base.copy(
        surfaceColor = surfaceColor,
        textColor = textColor,
        textScalePercent = textScalePercent.coerceIn(50, 150),
        alignment = alignment,
        compactMetric = compactMetric,
        hideData = hideData,
        showTitles = showTitles,
        detailMode = detailMode,
        metricColors = base.metricColors.copy(
            storage = colors[StorageWidget.CompactMetric.STORAGE] ?: base.metricColors.storage,
            data = colors[StorageWidget.CompactMetric.DATA] ?: base.metricColors.data,
            memory = colors[StorageWidget.CompactMetric.MEMORY] ?: base.metricColors.memory
        )
    )
}

package dev.revilodev.onewidget.widgets.storage

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.widgets.WidgetAppearance

enum class StorageRenderMode {
    PRODUCTION,
    CONFIG_PREVIEW,
    STUDIO_ACTUAL_PREVIEW,
    DEVELOPER_WIDGET,
    SNAPSHOT
}

data class StorageRenderSize(val widthDp: Float, val heightDp: Float) {
    val compact: Boolean
        get() = heightDp <= 100f
}

data class StorageMetricValue(
    val percent: Int,
    val usedBytes: Long,
    val totalBytes: Long
)

data class StorageRuntimeData(
    val storage: StorageMetricValue,
    val data: StorageMetricValue,
    val memory: StorageMetricValue
) {
    fun metric(metric: StorageWidget.CompactMetric): StorageMetricValue = when (metric) {
        StorageWidget.CompactMetric.STORAGE -> storage
        StorageWidget.CompactMetric.DATA -> data
        StorageWidget.CompactMetric.MEMORY -> memory
    }
}

data class StorageMetricColors(
    val storage: Int,
    val data: Int,
    val memory: Int
) {
    fun color(metric: StorageWidget.CompactMetric): Int = when (metric) {
        StorageWidget.CompactMetric.STORAGE -> storage
        StorageWidget.CompactMetric.DATA -> data
        StorageWidget.CompactMetric.MEMORY -> memory
    }
}

data class StorageWidgetConfiguration(
    val surfaceColor: Int,
    val textColor: Int,
    val textScalePercent: Int,
    val alignment: WidgetAppearance.Alignment,
    val gradientsEnabled: Boolean,
    val compactMetric: StorageWidget.CompactMetric,
    val hideData: Boolean,
    val showTitles: Boolean,
    val detailMode: StorageWidget.DetailMode,
    val metricColors: StorageMetricColors
)

object StorageSamples {
    private const val GB = 1_073_741_824L

    fun runtime() = StorageRuntimeData(
        storage = StorageMetricValue(62, 79L * GB, 128L * GB),
        data = StorageMetricValue(34, 3L * GB, 10L * GB),
        memory = StorageMetricValue(58, 5L * GB, 8L * GB)
    )
}

object StorageWidgetConfigurationRepository {
    fun load(context: Context, appWidgetId: Int): StorageWidgetConfiguration {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        return StorageWidgetConfiguration(
            surfaceColor = BlurWidget.tintColor(prefs, appWidgetId),
            textColor = WidgetAppearance.textColor(prefs, appWidgetId),
            textScalePercent = WidgetAppearance.textScale(prefs, appWidgetId),
            alignment = WidgetAppearance.alignment(prefs, appWidgetId),
            gradientsEnabled = WidgetAppearance.gradientsEnabled(prefs),
            compactMetric = StorageWidget.compactMetric(context, appWidgetId),
            hideData = prefs.getBoolean("storage_hide_data_$appWidgetId", false),
            showTitles = prefs.getBoolean("storage_show_titles_$appWidgetId", true),
            detailMode = StorageWidget.DetailMode.from(
                prefs.getString("storage_detail_mode_$appWidgetId", null)
            ),
            metricColors = StorageMetricColors(
                storage = StorageWidget.barColor(context, appWidgetId, StorageWidget.CompactMetric.STORAGE),
                data = StorageWidget.barColor(context, appWidgetId, StorageWidget.CompactMetric.DATA),
                memory = StorageWidget.barColor(context, appWidgetId, StorageWidget.CompactMetric.MEMORY)
            )
        )
    }

    fun studioSample(): StorageWidgetConfiguration = StorageWidgetConfiguration(
        surfaceColor = 0x66FFFFFF,
        textColor = 0xFFFFFFFF.toInt(),
        textScalePercent = 100,
        alignment = WidgetAppearance.Alignment.CENTRE,
        gradientsEnabled = true,
        compactMetric = StorageWidget.CompactMetric.STORAGE,
        hideData = false,
        showTitles = true,
        detailMode = StorageWidget.DetailMode.BOTH,
        metricColors = StorageMetricColors(
            storage = 0xFF08B0CE.toInt(),
            data = 0xFF5BD686.toInt(),
            memory = 0xFF1D79ED.toInt()
        )
    )
}

object StorageWidgetSizeResolver {
    fun fromOptions(options: Bundle): StorageRenderSize = StorageRenderSize(
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180).toFloat(),
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80).toFloat()
    )
}

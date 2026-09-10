package dev.revilodev.onewidget.widgets.storage

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.StatFs
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.production.ProductionWidgetDesignRepository
import dev.revilodev.onewidget.studio.production.ProductionWidgetDesignResolver
import dev.revilodev.onewidget.studio.production.WidgetProductionDesignModeRepository
import dev.revilodev.onewidget.widgets.variants.ExactWidgetVariants
import dev.revilodev.onewidget.studio.storage.StorageStudioDescriptor

open class StorageWidget : AppWidgetProvider() {
    enum class DetailMode(val label: String) {
        BOTH("Percentage and amount"),
        PERCENT("Percentage only"),
        AMOUNT("Amount only");

        companion object {
            fun from(value: String?) = entries.firstOrNull { it.name == value } ?: BOTH
        }
    }

    enum class CompactMetric(val label: String) {
        STORAGE("Storage"),
        DATA("Data usage"),
        MEMORY("Memory");

        companion object {
            fun from(value: String?): CompactMetric =
                entries.firstOrNull { it.name == value } ?: STORAGE
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
        scheduleRefresh(context)
    }

    override fun onEnabled(context: Context) = scheduleRefresh(context)

    override fun onDisabled(context: Context) =
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))

    override fun onDeleted(context: Context, ids: IntArray) {
        val designModes = WidgetProductionDesignModeRepository(context)
        ids.forEach { designModes.delete(StorageStudioDescriptor.WIDGET_ID, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = ExactWidgetVariants.providersFor(StorageWidget::class.java)
                .flatMap { provider -> manager.getAppWidgetIds(ComponentName(context, provider)).toList() }
                .distinct()
                .toIntArray()
            onUpdate(context, manager, ids)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        options: android.os.Bundle
    ) = updateWidget(context, manager, id)

    companion object {
        private const val ACTION_REFRESH = "dev.revilodev.onewidget.action.REFRESH_STORAGE_WIDGET"
        private const val DEFAULT_STORAGE_COLOR = 0xFF08B0CE.toInt()
        private const val DEFAULT_MEMORY_COLOR = 0xFF1D79ED.toInt()
        private const val DEFAULT_DATA_COLOR = 0xFF5BD686.toInt()
        private const val GB = 1_073_741_824L

        fun barColor(context: Context, id: Int, metric: CompactMetric): Int =
            context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getInt(
                "storage_bar_${metric.name.lowercase()}_$id",
                when (metric) {
                    CompactMetric.STORAGE -> DEFAULT_STORAGE_COLOR
                    CompactMetric.DATA -> DEFAULT_DATA_COLOR
                    CompactMetric.MEMORY -> DEFAULT_MEMORY_COLOR
                }
            )

        fun compactMetric(context: Context, id: Int): CompactMetric = CompactMetric.from(
            context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
                .getString("storage_compact_metric_$id", null)
        )

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val renderSize = StorageWidgetSizeResolver.fromOptions(manager.getAppWidgetOptions(id))
            val mode = WidgetProductionDesignModeRepository(context).mode(
                StorageStudioDescriptor.WIDGET_ID,
                id
            )
            val promoted = when (
                val loaded = ProductionWidgetDesignRepository(context)
                    .loadActive(StorageStudioDescriptor.WIDGET_ID)
            ) {
                is StudioRepositoryResult.Success -> loaded.value
                is StudioRepositoryResult.Failure -> null
            }
            val productionDesign = ProductionWidgetDesignResolver.resolve(
                mode = mode,
                promotedDesign = promoted,
                widgetId = StorageStudioDescriptor.WIDGET_ID,
                widthDp = renderSize.widthDp,
                heightDp = renderSize.heightDp
            )
            val rendered = StorageRemoteViewsRenderer.build(
                context = context,
                configuration = StorageWidgetConfigurationRepository.load(context, id),
                runtimeData = runtimeData(context),
                renderSize = renderSize,
                design = productionDesign,
                mode = StorageRenderMode.PRODUCTION
            )
            manager.updateAppWidget(id, rendered.remoteViews)
        }

        fun runtimeData(context: Context): StorageRuntimeData {
            val disk = StatFs(context.filesDir.absolutePath)
            val diskTotal = disk.totalBytes
            val diskUsed = diskTotal - disk.availableBytes
            val dataBytes = (TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes())
                .coerceAtLeast(0)
            val dataPlan = 10L * GB
            val memory = ActivityManager.MemoryInfo().also {
                context.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
            }
            val memoryUsed = memory.totalMem - memory.availMem
            return StorageRuntimeData(
                storage = metric(diskUsed, diskTotal),
                data = metric(dataBytes, dataPlan),
                memory = metric(memoryUsed, memory.totalMem)
            )
        }

        private fun metric(used: Long, total: Long) = StorageMetricValue(
            percent = (used * 100 / total.coerceAtLeast(1)).toInt().coerceIn(0, 100),
            usedBytes = used,
            totalBytes = total
        )

        private fun refreshIntent(context: Context) = PendingIntent.getBroadcast(
            context,
            7002,
            Intent(context, StorageWidget::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun scheduleRefresh(context: Context) {
            context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
                AlarmManager.RTC,
                System.currentTimeMillis() + 15_000L,
                refreshIntent(context)
            )
        }
    }
}

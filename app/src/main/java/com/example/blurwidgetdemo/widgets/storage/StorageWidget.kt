package com.example.blurwidgetdemo.widgets.storage

import android.app.ActivityManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.TrafficStats
import android.os.StatFs
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetAppearance
import java.util.Locale

class StorageWidget : AppWidgetProvider() {
    enum class CompactMetric(val label: String) {
        STORAGE("Storage"),
        DATA("Data usage"),
        MEMORY("Memory");

        companion object {
            fun from(value: String?): CompactMetric =
                entries.firstOrNull { it.name == value } ?: STORAGE
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { updateWidget(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) = updateWidget(context, manager, id)
    companion object {
        private const val DEFAULT_STORAGE_COLOR = 0xFF08B0CE.toInt()
        private const val DEFAULT_MEMORY_COLOR = 0xFF1D79ED.toInt()
        private const val DEFAULT_DATA_COLOR = 0xFF5BD686.toInt()
        fun barColor(context: Context, id: Int, metric: CompactMetric): Int = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getInt("storage_bar_${metric.name.lowercase()}_$id", when (metric) { CompactMetric.STORAGE -> DEFAULT_STORAGE_COLOR; CompactMetric.DATA -> DEFAULT_DATA_COLOR; CompactMetric.MEMORY -> DEFAULT_MEMORY_COLOR })
        fun compactMetric(context: Context, id: Int) = CompactMetric.from(context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getString("storage_compact_metric_$id", null))
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val stats = stats(context)
            val compact = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
            val color = WidgetAppearance.textColor(prefs, id)
            if (compact) {
                val metric = compactMetric(context, id)
                val selected = when (metric) { CompactMetric.STORAGE -> stats.storage; CompactMetric.DATA -> stats.data; CompactMetric.MEMORY -> stats.memory }
                RemoteViews(context.packageName, R.layout.widget_storage_compact).apply {
                    setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(prefs, id))
                    setTextViewText(R.id.widget_primary, "${metric.label} ${selected.percent}%")
                    setTextColor(R.id.widget_primary, color)
                    setInt(R.id.widget_primary, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                    setFloat(R.id.widget_primary, "setTextSize", 20f * WidgetAppearance.textScale(prefs, id) / 100f)
                    setProgressBar(R.id.compact_progress, 100, selected.percent, false)
                    setColorStateList(R.id.compact_progress, "setProgressTintList", ColorStateList.valueOf(barColor(context, id, metric)))
                }.also { manager.updateAppWidget(id, it) }
            } else RemoteViews(context.packageName, R.layout.widget_storage_large).apply {
                setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(prefs, id))
                setTextViewText(R.id.storage_percent, "${stats.storage.percent}%")
                setTextViewText(R.id.storage_capacity, stats.storage.label)
                setTextViewText(R.id.data_capacity, stats.data.label)
                setTextViewText(R.id.memory_capacity, stats.memory.label)
                setProgressBar(R.id.storage_bar, 100, stats.storage.percent, false)
                setProgressBar(R.id.data_bar, 100, stats.data.percent, false)
                setProgressBar(R.id.memory_bar, 100, stats.memory.percent, false)
                setColorStateList(R.id.storage_bar, "setProgressTintList", ColorStateList.valueOf(barColor(context, id, CompactMetric.STORAGE)))
                setColorStateList(R.id.data_bar, "setProgressTintList", ColorStateList.valueOf(barColor(context, id, CompactMetric.DATA)))
                setColorStateList(R.id.memory_bar, "setProgressTintList", ColorStateList.valueOf(barColor(context, id, CompactMetric.MEMORY)))
                intArrayOf(R.id.storage_heading, R.id.storage_percent, R.id.storage_capacity, R.id.data_heading, R.id.data_capacity, R.id.memory_heading, R.id.memory_capacity).forEach {
                    setTextColor(it, color); setInt(it, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                }
                setFloat(R.id.storage_heading, "setTextSize", 17f * WidgetAppearance.textScale(prefs, id) / 100f)
            }.also { manager.updateAppWidget(id, it) }
        }
        private data class Metric(val percent: Int, val label: String)
        private data class Stats(val storage: Metric, val data: Metric, val memory: Metric)
        private fun stats(context: Context): Stats {
            val disk = StatFs(context.filesDir.absolutePath); val diskTotal = disk.totalBytes; val diskUsed = diskTotal - disk.availableBytes
            val dataBytes = (TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()).coerceAtLeast(0); val dataPlan = 10L * GB
            val memory = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
            val memoryUsed = memory.totalMem - memory.availMem
            fun metric(used: Long, total: Long, prefix: String = "") = Metric((used * 100 / total.coerceAtLeast(1)).toInt().coerceIn(0, 100), "$prefix${format(used)}/${format(total)}")
            return Stats(metric(diskUsed, diskTotal), metric(dataBytes, dataPlan, "Since boot "), metric(memoryUsed, memory.totalMem))
        }
        private fun format(bytes: Long) = String.format(Locale.getDefault(), "%.0fGB", bytes.toDouble() / GB)
        private const val GB = 1_073_741_824L
    }
}

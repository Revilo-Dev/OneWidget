package dev.revilodev.onewidget.widgets.storage

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.model.ResolvedWidgetDesign
import dev.revilodev.onewidget.studio.storage.StorageStudioDescriptor
import dev.revilodev.onewidget.widgets.WidgetProgressBarRenderer
import java.util.Locale

data class StorageRenderPlan(
    val mode: StorageRenderMode,
    val size: StorageRenderSize,
    val compact: Boolean,
    val productionLayoutResId: Int,
    val appliedLayoutResId: Int,
    val geometry: StorageGeometryPlan?,
    val installTapActions: Boolean,
    val diagnostics: List<StorageRenderDiagnostic>
)

data class StorageRemoteViewsRenderResult(
    val remoteViews: RemoteViews,
    val plan: StorageRenderPlan,
    val tapActionsApplied: Boolean
)

object StorageContentFormatter {
    private const val GB = 1_073_741_824L

    fun amount(value: StorageMetricValue): String = if (value.usedBytes < 0) "Unavailable"
    else if (value.totalBytes <= 0) String.format(Locale.getDefault(), "%.1fGB since restart", value.usedBytes.toDouble() / GB)
    else String.format(
        Locale.getDefault(),
        "%.0fGB/%.0fGB",
        value.usedBytes.toDouble() / GB,
        value.totalBytes.toDouble() / GB
    )

    fun detail(
        title: String,
        value: StorageMetricValue,
        showTitle: Boolean,
        detailMode: StorageWidget.DetailMode
    ): String = listOfNotNull(
        title.takeIf { showTitle },
        if (value.totalBytes <= 0) amount(value) else when (detailMode) {
            StorageWidget.DetailMode.BOTH -> "${value.percent}% • ${amount(value)}"
            StorageWidget.DetailMode.PERCENT -> "${value.percent}%"
            StorageWidget.DetailMode.AMOUNT -> amount(value)
        }
    ).joinToString("  ")
}

object StorageRemoteViewsRenderer {
    fun plan(
        renderSize: StorageRenderSize,
        design: ResolvedWidgetDesign? = null,
        mode: StorageRenderMode = StorageRenderMode.PRODUCTION,
        density: Float = 1f,
        apiLevel: Int = android.os.Build.VERSION.SDK_INT
    ): StorageRenderPlan {
        val productionLayout = if (renderSize.compact) {
            R.layout.widget_storage_compact
        } else {
            R.layout.widget_storage_large
        }
        val geometry = design?.let { StorageRemoteViewsGeometry.plan(it, renderSize, density, apiLevel) }
        val diagnostics = buildList {
            if (design != null && design.widgetId != StorageStudioDescriptor.WIDGET_ID) {
                add(StorageRenderDiagnostic(null, "widget-id", "Expected storage, received ${design.widgetId}"))
            }
            geometry?.diagnostics?.let(::addAll)
        }
        return StorageRenderPlan(
            mode = mode,
            size = renderSize,
            compact = renderSize.compact,
            productionLayoutResId = productionLayout,
            appliedLayoutResId = if (design == null) productionLayout else R.layout.widget_studio_storage,
            geometry = geometry,
            installTapActions = mode == StorageRenderMode.PRODUCTION,
            diagnostics = diagnostics
        )
    }

    fun build(
        context: Context,
        configuration: StorageWidgetConfiguration,
        runtimeData: StorageRuntimeData,
        renderSize: StorageRenderSize,
        design: ResolvedWidgetDesign? = null,
        mode: StorageRenderMode = StorageRenderMode.PRODUCTION
    ): StorageRemoteViewsRenderResult {
        val plan = plan(renderSize, design, mode, context.resources.displayMetrics.density)
        val views = RemoteViews(context.packageName, plan.appliedLayoutResId)
        BlurWidget.applySurfaceColor(views, configuration.surfaceColor)
        val tapActionsApplied = if (design == null) {
            if (plan.compact) applyCompact(views, configuration, runtimeData)
            else applyLarge(context, views, configuration, runtimeData, plan.installTapActions)
        } else {
            require(design.widgetId == StorageStudioDescriptor.WIDGET_ID) {
                "Storage renderer received design for ${design.widgetId}"
            }
            applyStudio(context, views, configuration, runtimeData, plan)
        }
        views.setViewVisibility(R.id.data_bar, if (runtimeData.data.totalBytes > 0) View.VISIBLE else View.GONE)
        if (configuration.compactMetric == StorageWidget.CompactMetric.DATA) {
            views.setViewVisibility(R.id.compact_progress, if (runtimeData.data.totalBytes > 0) View.VISIBLE else View.GONE)
        }
        return StorageRemoteViewsRenderResult(views, plan, tapActionsApplied)
    }

    private fun applyCompact(
        views: RemoteViews,
        configuration: StorageWidgetConfiguration,
        runtimeData: StorageRuntimeData
    ): Boolean = with(views) {
        val metric = configuration.compactMetric
        val value = runtimeData.metric(metric)
        setTextViewText(
            R.id.widget_primary,
            StorageContentFormatter.detail(metric.label, value, configuration.showTitles, configuration.detailMode)
        )
        setTextColor(R.id.widget_primary, configuration.textColor)
        setInt(R.id.widget_primary, "setGravity", configuration.alignment.gravity)
        setFloat(R.id.widget_primary, "setTextSize", 20f * configuration.textScalePercent / 100f)
        setImageViewBitmap(
            R.id.compact_progress,
            progressBitmap(value.percent, configuration.metricColors.color(metric), configuration.gradientsEnabled)
        )
        false
    }

    private fun applyLarge(
        context: Context,
        views: RemoteViews,
        configuration: StorageWidgetConfiguration,
        runtimeData: StorageRuntimeData,
        installTapActions: Boolean
    ): Boolean = with(views) {
        applyLegacyMetric(
            this,
            R.id.storage_label,
            R.id.storage_bar,
            StorageWidget.CompactMetric.STORAGE,
            runtimeData.storage,
            configuration
        )
        applyLegacyMetric(
            this,
            R.id.memory_label,
            R.id.memory_bar,
            StorageWidget.CompactMetric.MEMORY,
            runtimeData.memory,
            configuration
        )
        applyLegacyMetric(
            this,
            R.id.data_label,
            R.id.data_bar,
            StorageWidget.CompactMetric.DATA,
            runtimeData.data,
            configuration
        )
        setViewVisibility(R.id.data_bar_row, if (configuration.hideData) View.GONE else View.VISIBLE)
        setViewVisibility(R.id.manage_storage, if (configuration.hideData) View.VISIBLE else View.GONE)
        if (configuration.hideData && installTapActions) {
            setOnClickPendingIntent(R.id.manage_storage, manageStorageIntent(context))
            true
        } else {
            false
        }
    }

    private fun applyLegacyMetric(
        views: RemoteViews,
        labelViewId: Int,
        progressViewId: Int,
        metric: StorageWidget.CompactMetric,
        value: StorageMetricValue,
        configuration: StorageWidgetConfiguration
    ) = with(views) {
        setTextViewText(
            labelViewId,
            StorageContentFormatter.detail(metric.label, value, configuration.showTitles, configuration.detailMode)
        )
        setTextColor(labelViewId, configuration.textColor)
        setInt(labelViewId, "setGravity", Gravity.CENTER)
        setFloat(labelViewId, "setTextSize", 15f * configuration.textScalePercent / 100f)
        setImageViewBitmap(
            progressViewId,
            progressBitmap(value.percent, configuration.metricColors.color(metric), configuration.gradientsEnabled)
        )
    }

    private fun applyStudio(
        context: Context,
        views: RemoteViews,
        configuration: StorageWidgetConfiguration,
        runtimeData: StorageRuntimeData,
        plan: StorageRenderPlan
    ): Boolean {
        val geometry = requireNotNull(plan.geometry)
        val geometryById = geometry.elements.associateBy { it.elementId }
        val primaryMetric = configuration.compactMetric
        applyStudioMetric(
            views,
            geometryById.getValue(StorageStudioDescriptor.PRIMARY),
            primaryMetric,
            runtimeData.metric(primaryMetric),
            configuration,
            runtimeVisible = plan.compact
        )
        applyStudioMetric(
            views,
            geometryById.getValue(StorageStudioDescriptor.STORAGE),
            StorageWidget.CompactMetric.STORAGE,
            runtimeData.storage,
            configuration,
            runtimeVisible = !plan.compact
        )
        applyStudioMetric(
            views,
            geometryById.getValue(StorageStudioDescriptor.MEMORY),
            StorageWidget.CompactMetric.MEMORY,
            runtimeData.memory,
            configuration,
            runtimeVisible = !plan.compact
        )
        val dataGeometry = geometryById.getValue(StorageStudioDescriptor.DATA)
        if (!plan.compact && configuration.hideData) {
            views.setViewVisibility(
                dataGeometry.binding.containerViewId,
                if (dataGeometry.visible) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(dataGeometry.binding.progressViewId, View.GONE)
            views.setImageViewResource(dataGeometry.binding.iconViewId, R.drawable.manage_storage)
            views.setTextViewText(dataGeometry.binding.labelViewId, "Manage storage")
            views.setTextColor(dataGeometry.binding.labelViewId, configuration.textColor)
            views.setFloat(
                dataGeometry.binding.labelViewId,
                "setTextSize",
                15f * configuration.textScalePercent / 100f
            )
            if (plan.installTapActions && dataGeometry.visible) {
                views.setOnClickPendingIntent(dataGeometry.binding.containerViewId, manageStorageIntent(context))
            }
        } else {
            applyStudioMetric(
                views,
                dataGeometry,
                StorageWidget.CompactMetric.DATA,
                runtimeData.data,
                configuration,
                runtimeVisible = !plan.compact
            )
            views.setOnClickPendingIntent(dataGeometry.binding.containerViewId, null)
        }
        StorageRemoteViewsGeometry.apply(views, geometry)
        // Runtime compact/large selection is independent from design visibility.
        geometry.elements.forEach { element ->
            val runtimeVisible = when (element.elementId) {
                StorageStudioDescriptor.PRIMARY -> plan.compact
                else -> !plan.compact
            }
            views.setViewVisibility(
                element.binding.containerViewId,
                if (element.visible && runtimeVisible) View.VISIBLE else View.GONE
            )
        }
        return plan.installTapActions && !plan.compact && configuration.hideData && dataGeometry.visible
    }

    private fun applyStudioMetric(
        views: RemoteViews,
        geometry: StorageElementGeometry,
        metric: StorageWidget.CompactMetric,
        value: StorageMetricValue,
        configuration: StorageWidgetConfiguration,
        runtimeVisible: Boolean
    ) = with(views) {
        val binding = geometry.binding
        setViewVisibility(binding.containerViewId, if (geometry.visible && runtimeVisible) View.VISIBLE else View.GONE)
        setViewVisibility(binding.progressViewId, View.VISIBLE)
        setImageViewResource(binding.iconViewId, iconFor(metric))
        setTextViewText(
            binding.labelViewId,
            StorageContentFormatter.detail(metric.label, value, configuration.showTitles, configuration.detailMode)
        )
        setTextColor(binding.labelViewId, configuration.textColor)
        setFloat(binding.labelViewId, "setTextSize", 15f * configuration.textScalePercent / 100f)
        setImageViewBitmap(
            binding.progressViewId,
            progressBitmap(
                value.percent,
                configuration.metricColors.color(metric),
                configuration.gradientsEnabled,
                StorageRemoteViewsGeometry.progressRadiusFraction(geometry)
            )
        )
    }

    private fun iconFor(metric: StorageWidget.CompactMetric): Int = when (metric) {
        StorageWidget.CompactMetric.STORAGE -> R.drawable.manage_storage
        StorageWidget.CompactMetric.DATA -> R.drawable.network_storage
        StorageWidget.CompactMetric.MEMORY -> R.drawable.device_health
    }

    private fun progressBitmap(
        percent: Int,
        tint: Int,
        gradientsEnabled: Boolean,
        radiusFraction: Float = 0.5f
    ) = if (gradientsEnabled) {
        WidgetProgressBarRenderer.render(
            percent,
            intArrayOf(
                WidgetProgressBarRenderer.darker(tint, .20f),
                tint,
                WidgetProgressBarRenderer.lighter(tint, .20f)
            ),
            radiusFraction
        )
    } else {
        WidgetProgressBarRenderer.render(
            percent,
            tint,
            animated = false,
            cornerRadiusFraction = radiusFraction
        )
    }

    private fun manageStorageIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        MANAGE_STORAGE_REQUEST_CODE,
        Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private const val MANAGE_STORAGE_REQUEST_CODE = 7003
}

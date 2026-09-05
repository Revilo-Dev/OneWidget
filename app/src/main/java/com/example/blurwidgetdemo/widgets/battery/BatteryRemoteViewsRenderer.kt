package com.example.blurwidgetdemo.widgets.battery

import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.battery.BatteryStudioDescriptor
import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign
import com.example.blurwidgetdemo.widgets.WidgetProgressBarRenderer

data class BatteryRenderPlan(
    val mode: BatteryRenderMode,
    val size: BatteryRenderSize,
    val compact: Boolean,
    val productionLayoutResId: Int,
    val appliedLayoutResId: Int,
    val geometry: BatteryGeometryPlan?,
    val installTapActions: Boolean,
    val diagnostics: List<BatteryRenderDiagnostic>
)

data class BatteryRemoteViewsRenderResult(
    val remoteViews: RemoteViews,
    val plan: BatteryRenderPlan,
    val tapActionsApplied: Boolean
)

data class BatteryResolvedContent(
    val device: BatteryDeviceRuntimeData,
    val percentageText: String,
    val statusText: String,
    val progressPercent: Int,
    val charging: Boolean
)

object BatteryContentResolver {
    fun resolve(runtimeData: BatteryRuntimeData, preferredDeviceKey: String? = null): BatteryResolvedContent {
        val device = runtimeData.devices.firstOrNull { it.key == preferredDeviceKey }
            ?: runtimeData.devices.firstOrNull()
            ?: BatteryDeviceRuntimeData(
                key = "unavailable",
                label = "Phone",
                percentage = null,
                iconResId = R.drawable.phone,
                statusText = "Unavailable"
            )
        return BatteryResolvedContent(
            device = device,
            percentageText = device.percentage?.let { "$it%" } ?: "Unavailable",
            statusText = device.statusText,
            progressPercent = device.percentage ?: 0,
            charging = device.charging
        )
    }
}

object BatteryRemoteViewsRenderer {
    fun plan(
        renderSize: BatteryRenderSize,
        design: ResolvedWidgetDesign? = null,
        mode: BatteryRenderMode = BatteryRenderMode.PRODUCTION,
        density: Float = 1f,
        apiLevel: Int = android.os.Build.VERSION.SDK_INT
    ): BatteryRenderPlan {
        val productionLayout = if (renderSize.compact) {
            R.layout.widget_battery_compact
        } else {
            R.layout.widget_battery_large
        }
        val geometry = design?.let { BatteryRemoteViewsGeometry.plan(it, renderSize, density, apiLevel) }
        val diagnostics = buildList {
            if (design != null && design.widgetId != BatteryStudioDescriptor.WIDGET_ID) {
                add(BatteryRenderDiagnostic(null, "widget-id", "Expected battery, received ${design.widgetId}"))
            }
            geometry?.diagnostics?.let(::addAll)
        }
        return BatteryRenderPlan(
            mode = mode,
            size = renderSize,
            compact = renderSize.compact,
            productionLayoutResId = productionLayout,
            appliedLayoutResId = if (design == null) productionLayout else R.layout.widget_studio_battery,
            geometry = geometry,
            installTapActions = false,
            diagnostics = diagnostics
        )
    }

    fun build(
        context: android.content.Context,
        configuration: BatteryWidgetConfiguration,
        runtimeData: BatteryRuntimeData,
        renderSize: BatteryRenderSize,
        design: ResolvedWidgetDesign? = null,
        mode: BatteryRenderMode = BatteryRenderMode.PRODUCTION
    ): BatteryRemoteViewsRenderResult {
        val plan = plan(renderSize, design, mode, context.resources.displayMetrics.density)
        val views = RemoteViews(context.packageName, plan.appliedLayoutResId)
        // Preserve the same @android:id/background surface used by production and One UI Home.
        BlurWidget.applySurfaceColor(views, configuration.surfaceColor)
        if (design == null) {
            if (plan.compact) applyCompact(views, configuration, runtimeData)
            else applyLarge(views, configuration, runtimeData, hideLastRow = plan.size.widthDp >= 300f)
        } else {
            require(design.widgetId == BatteryStudioDescriptor.WIDGET_ID) {
                "Battery renderer received design for ${design.widgetId}"
            }
            applyStudio(views, configuration, runtimeData, plan.geometry)
            plan.geometry?.let { BatteryRemoteViewsGeometry.apply(views, it) }
        }
        return BatteryRemoteViewsRenderResult(views, plan, tapActionsApplied = false)
    }

    private fun applyCompact(
        views: RemoteViews,
        configuration: BatteryWidgetConfiguration,
        runtimeData: BatteryRuntimeData
    ) = with(views) {
        val content = BatteryContentResolver.resolve(runtimeData, configuration.compactDeviceKey)
        val device = content.device
        setTextViewText(R.id.battery_percentage, device.percentage?.let { "$it%" } ?: "—")
        setImageViewResource(R.id.battery_icon, device.iconResId)
        setImageViewBitmap(R.id.battery_compact_bar, progressBitmap(device, configuration, runtimeData))
        setTextColor(R.id.battery_percentage, configuration.textColor)
        setInt(R.id.battery_percentage, "setGravity", configuration.alignment.gravity)
        setFloat(R.id.battery_percentage, "setTextSize", 30f * configuration.textScalePercent / 100f)
    }

    private fun applyLarge(
        views: RemoteViews,
        configuration: BatteryWidgetConfiguration,
        runtimeData: BatteryRuntimeData,
        hideLastRow: Boolean
    ) = with(views) {
        val rows = intArrayOf(R.id.battery_row_1, R.id.battery_row_2, R.id.battery_row_3, R.id.battery_row_4)
        val labels = intArrayOf(R.id.battery_label_1, R.id.battery_label_2, R.id.battery_label_3, R.id.battery_label_4)
        val bars = intArrayOf(R.id.battery_bar_1, R.id.battery_bar_2, R.id.battery_bar_3, R.id.battery_bar_4)
        val icons = intArrayOf(R.id.battery_icon_1, R.id.battery_icon_2, R.id.battery_icon_3, R.id.battery_icon_4)
        rows.indices.forEach { index ->
            val device = runtimeData.devices.getOrNull(index)
            setViewVisibility(rows[index], if (device == null || (hideLastRow && index == rows.lastIndex)) View.GONE else View.VISIBLE)
            if (device != null) {
                setTextViewText(labels[index], "${device.label}  ${percentageText(device)}")
                setTextColor(labels[index], configuration.textColor)
                setFloat(labels[index], "setTextSize", 15f * configuration.textScalePercent / 100f)
                setImageViewResource(icons[index], device.iconResId)
                setImageViewBitmap(bars[index], progressBitmap(device, configuration, runtimeData))
            }
        }
    }

    private fun applyStudio(
        views: RemoteViews,
        configuration: BatteryWidgetConfiguration,
        runtimeData: BatteryRuntimeData,
        geometry: BatteryGeometryPlan?
    ) = with(views) {
        val content = BatteryContentResolver.resolve(runtimeData)
        val device = content.device
        val progressGeometry = geometry?.elements?.firstOrNull {
            it.elementId == BatteryStudioDescriptor.PROGRESS
        }
        val radiusFraction = progressGeometry?.let(BatteryRemoteViewsGeometry::progressRadiusFraction) ?: 0.5f
        setTextViewText(R.id.battery_percentage, content.percentageText)
        setTextViewText(R.id.battery_status, content.statusText)
        setImageViewResource(R.id.battery_icon, device.iconResId)
        setImageViewBitmap(
            R.id.battery_compact_bar,
            progressBitmap(device, configuration, runtimeData, radiusFraction)
        )
        setTextColor(R.id.battery_percentage, configuration.textColor)
        setTextColor(R.id.battery_status, configuration.textColor)
        setFloat(R.id.battery_percentage, "setTextSize", 30f * configuration.textScalePercent / 100f)
        setFloat(R.id.battery_status, "setTextSize", 14f * configuration.textScalePercent / 100f)
    }

    private fun progressBitmap(
        device: BatteryDeviceRuntimeData,
        configuration: BatteryWidgetConfiguration,
        runtimeData: BatteryRuntimeData,
        radiusFraction: Float = 0.5f
    ) = if (configuration.gradientsEnabled) {
        WidgetProgressBarRenderer.render(
            device.percentage ?: 0,
            when (stateFor(device, runtimeData)) {
                BatteryVisualState.CHARGING -> GRADIENT_CHARGING
                BatteryVisualState.SUPER_FAST_CHARGING -> GRADIENT_SUPER_FAST
                BatteryVisualState.LOW -> GRADIENT_LOW
                else -> GRADIENT_NORMAL
            },
            radiusFraction
        )
    } else {
        WidgetProgressBarRenderer.render(
            device.percentage ?: 0,
            colourFor(stateFor(device, runtimeData), configuration.stateColors),
            animated = false,
            cornerRadiusFraction = radiusFraction
        )
    }

    private fun stateFor(device: BatteryDeviceRuntimeData, runtimeData: BatteryRuntimeData): BatteryVisualState = when {
        device.charging && (device.percentage ?: 0) >= 85 -> BatteryVisualState.SUPER_FAST_CHARGING
        device.charging -> BatteryVisualState.CHARGING
        runtimeData.powerSaving -> BatteryVisualState.POWER_SAVING
        (device.percentage ?: 100) <= 15 -> BatteryVisualState.LOW
        else -> BatteryVisualState.NORMAL
    }

    private fun colourFor(state: BatteryVisualState, colors: BatteryStateColors): Int = when (state) {
        BatteryVisualState.LOW -> colors.low
        BatteryVisualState.POWER_SAVING -> colors.powerSaving
        BatteryVisualState.CHARGING -> colors.charging
        BatteryVisualState.SUPER_FAST_CHARGING -> colors.superFastCharging
        BatteryVisualState.NORMAL -> colors.normal
    }

    private fun percentageText(device: BatteryDeviceRuntimeData): String = device.percentage?.let { "$it%" } ?: "Unavailable"

    private enum class BatteryVisualState { LOW, POWER_SAVING, NORMAL, CHARGING, SUPER_FAST_CHARGING }

    private val GRADIENT_CHARGING = intArrayOf(0xFF9BDCA9.toInt(), 0xFF31CE8A.toInt(), 0xFF2F989F.toInt())
    private val GRADIENT_SUPER_FAST = intArrayOf(0xFF633EB9.toInt(), 0xFF6A8BFA.toInt(), 0xFF3ACD8C.toInt())
    private val GRADIENT_NORMAL = intArrayOf(0xFF3BCE8E.toInt(), 0xFF87FA6A.toInt(), 0xFF3ACD8C.toInt())
    private val GRADIENT_LOW = intArrayOf(0xFFFF856D.toInt(), 0xFFFFA352.toInt(), 0xFFFF836D.toInt())
}

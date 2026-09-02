package com.example.blurwidgetdemo.widgets.battery

import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.battery.BatteryStudioDescriptor
import com.example.blurwidgetdemo.studio.editor.AnchorPositioning
import com.example.blurwidgetdemo.studio.model.DimensionMode
import com.example.blurwidgetdemo.studio.model.HorizontalAlignment
import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign
import com.example.blurwidgetdemo.studio.model.VerticalAlignment
import com.example.blurwidgetdemo.studio.model.WidgetElementSpec
import com.example.blurwidgetdemo.studio.rendering.RemoteViewsGeometryCompat

data class BatteryElementViewBinding(
    val elementId: String,
    val containerViewId: Int,
    val contentViewIds: List<Int>
)

object BatteryElementViewMap {
    val bindings = listOf(
        BatteryElementViewBinding(BatteryStudioDescriptor.PERCENTAGE, R.id.battery_percentage_wrapper, listOf(R.id.battery_percentage)),
        BatteryElementViewBinding(BatteryStudioDescriptor.STATUS, R.id.battery_status_wrapper, listOf(R.id.battery_status)),
        BatteryElementViewBinding(BatteryStudioDescriptor.ICON, R.id.battery_icon_wrapper, listOf(R.id.battery_icon)),
        BatteryElementViewBinding(BatteryStudioDescriptor.PROGRESS, R.id.battery_progress_wrapper, listOf(R.id.battery_compact_bar))
    ).associateBy { it.elementId }
}

data class BatteryRenderDiagnostic(
    val elementId: String?,
    val property: String,
    val reason: String,
    val minimumApi: Int? = null
)

data class BatteryElementGeometry(
    val elementId: String,
    val containerViewId: Int,
    val contentViewIds: List<Int>,
    val visible: Boolean,
    val widthDp: Float,
    val heightDp: Float,
    val startMarginDp: Float,
    val topMarginDp: Float,
    val padding: IntArray,
    val contentGravity: Int,
    val cornerRadiusDp: Float,
    val dynamicGeometrySupported: Boolean
)

data class BatteryGeometryPlan(
    val elements: List<BatteryElementGeometry>,
    val diagnostics: List<BatteryRenderDiagnostic>
)

object BatteryRemoteViewsGeometry {
    fun plan(
        design: ResolvedWidgetDesign,
        renderSize: BatteryRenderSize,
        density: Float,
        apiLevel: Int
    ): BatteryGeometryPlan {
        val diagnostics = mutableListOf<BatteryRenderDiagnostic>()
        val elements = design.elements.mapNotNull { element ->
            val binding = BatteryElementViewMap.bindings[element.elementId]
            if (binding == null) {
                diagnostics += BatteryRenderDiagnostic(element.elementId, "mapping", "No RemoteViews binding is registered")
                return@mapNotNull null
            }
            val wrap = estimatedWrapSize(element.elementId)
            val width = resolveDimension(element.widthMode, element.widthDp, renderSize.widthDp, wrap.first)
            val height = resolveDimension(element.heightMode, element.heightDp, renderSize.heightDp, wrap.second)
            val anchor = AnchorPositioning.anchorPoint(element.anchor, renderSize.widthDp, renderSize.heightDp)
            val centredLeft = (renderSize.widthDp - width) / 2f
            val centredTop = (renderSize.heightDp - height) / 2f
            val supported = apiLevel >= RemoteViewsGeometryCompat.MIN_DYNAMIC_GEOMETRY_API
            if (!supported) {
                listOf("width", "height", "position").forEach { property ->
                    diagnostics += BatteryRenderDiagnostic(
                        element.elementId,
                        property,
                        "$property override ignored below API 31",
                        RemoteViewsGeometryCompat.MIN_DYNAMIC_GEOMETRY_API
                    )
                }
            }
            val radiusPlan = RemoteViewsGeometryCompat.radiusPlan(element.cornerRadiusDp, apiLevel)
            radiusPlan.diagnostic?.let {
                diagnostics += BatteryRenderDiagnostic(
                    element.elementId,
                    "cornerRadius",
                    it,
                    RemoteViewsGeometryCompat.MIN_DYNAMIC_GEOMETRY_API
                )
            }
            BatteryElementGeometry(
                elementId = element.elementId,
                containerViewId = binding.containerViewId,
                contentViewIds = binding.contentViewIds,
                visible = element.visible,
                widthDp = width,
                heightDp = height,
                startMarginDp = anchor.x + element.xDp - width / 2f - centredLeft,
                topMarginDp = anchor.y + element.yDp - height / 2f - centredTop,
                padding = intArrayOf(
                    (element.paddingStartDp * density).toInt(),
                    (element.paddingTopDp * density).toInt(),
                    (element.paddingEndDp * density).toInt(),
                    (element.paddingBottomDp * density).toInt()
                ),
                contentGravity = element.contentGravity(),
                cornerRadiusDp = element.cornerRadiusDp,
                dynamicGeometrySupported = supported
            )
        }
        return BatteryGeometryPlan(elements, diagnostics)
    }

    fun apply(remoteViews: RemoteViews, plan: BatteryGeometryPlan) {
        plan.elements.forEach { element ->
            remoteViews.setViewVisibility(element.containerViewId, if (element.visible) View.VISIBLE else View.GONE)
            remoteViews.setViewPadding(
                element.containerViewId,
                element.padding[0],
                element.padding[1],
                element.padding[2],
                element.padding[3]
            )
            if (element.elementId == BatteryStudioDescriptor.PERCENTAGE ||
                element.elementId == BatteryStudioDescriptor.STATUS
            ) {
                element.contentViewIds.forEach { remoteViews.setInt(it, "setGravity", element.contentGravity) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && element.dynamicGeometrySupported) {
                RemoteViewsGeometryCompat.applySizeAndPosition(
                    remoteViews,
                    element.containerViewId,
                    element.widthDp,
                    element.heightDp,
                    element.startMarginDp,
                    element.topMarginDp
                )
            }
            if (element.elementId == BatteryStudioDescriptor.PROGRESS) {
                RemoteViewsGeometryCompat.applyOutlineRadius(
                    remoteViews,
                    element.containerViewId,
                    element.cornerRadiusDp
                )
            }
        }
    }

    fun progressRadiusFraction(element: BatteryElementGeometry): Float =
        if (element.dynamicGeometrySupported) {
            (element.cornerRadiusDp / element.heightDp.coerceAtLeast(1f)).coerceIn(0f, 0.5f)
        } else {
            0.5f
        }

    private fun resolveDimension(mode: DimensionMode, fixed: Float, canvas: Float, wrap: Float): Float = when (mode) {
        DimensionMode.WRAP -> wrap
        DimensionMode.MATCH -> canvas
        DimensionMode.FIXED -> fixed
    }.coerceAtLeast(1f)

    private fun estimatedWrapSize(elementId: String): Pair<Float, Float> = when (elementId) {
        BatteryStudioDescriptor.PERCENTAGE -> 78f to 48f
        BatteryStudioDescriptor.STATUS -> 84f to 28f
        BatteryStudioDescriptor.ICON -> 32f to 32f
        BatteryStudioDescriptor.PROGRESS -> 160f to 24f
        else -> 64f to 32f
    }

    private fun WidgetElementSpec.contentGravity(): Int {
        val horizontal = when (horizontalAlignment) {
            HorizontalAlignment.START -> Gravity.START
            HorizontalAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
            HorizontalAlignment.END -> Gravity.END
        }
        val vertical = when (verticalAlignment) {
            VerticalAlignment.TOP -> Gravity.TOP
            VerticalAlignment.CENTER -> Gravity.CENTER_VERTICAL
            VerticalAlignment.BOTTOM -> Gravity.BOTTOM
        }
        return horizontal or vertical
    }
}

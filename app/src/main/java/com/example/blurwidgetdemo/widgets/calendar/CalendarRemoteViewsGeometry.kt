package com.example.blurwidgetdemo.widgets.calendar

import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.calendar.CalendarStudioDescriptor
import com.example.blurwidgetdemo.studio.editor.AnchorPositioning
import com.example.blurwidgetdemo.studio.model.DimensionMode
import com.example.blurwidgetdemo.studio.model.HorizontalAlignment
import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign
import com.example.blurwidgetdemo.studio.model.VerticalAlignment
import com.example.blurwidgetdemo.studio.model.WidgetElementSpec
import com.example.blurwidgetdemo.studio.rendering.RemoteViewsGeometryCompat

data class CalendarElementViewBinding(
    val elementId: String,
    val containerViewId: Int,
    val contentViewId: Int
)

object CalendarElementViewMap {
    val bindings = listOf(
        CalendarElementViewBinding(
            CalendarStudioDescriptor.TITLE,
            R.id.calendar_title_wrapper,
            R.id.calendar_title
        ),
        CalendarElementViewBinding(
            CalendarStudioDescriptor.DETAILS,
            R.id.calendar_details_wrapper,
            R.id.calendar_details
        )
    ).associateBy { it.elementId }
}

data class CalendarRenderDiagnostic(
    val elementId: String?,
    val property: String,
    val reason: String,
    val minimumApi: Int? = null
)

data class CalendarElementGeometry(
    val elementId: String,
    val binding: CalendarElementViewBinding,
    val visible: Boolean,
    val widthDp: Float,
    val heightDp: Float,
    val startMarginDp: Float,
    val topMarginDp: Float,
    val padding: IntArray,
    val contentGravity: Int,
    val dynamicGeometrySupported: Boolean
)

data class CalendarGeometryPlan(
    val elements: List<CalendarElementGeometry>,
    val diagnostics: List<CalendarRenderDiagnostic>
)

object CalendarRemoteViewsGeometry {
    fun plan(
        design: ResolvedWidgetDesign,
        renderSize: CalendarRenderSize,
        density: Float,
        apiLevel: Int
    ): CalendarGeometryPlan {
        val diagnostics = mutableListOf<CalendarRenderDiagnostic>()
        val elements = design.elements.mapNotNull { element ->
            val binding = CalendarElementViewMap.bindings[element.elementId]
            if (binding == null) {
                diagnostics += CalendarRenderDiagnostic(
                    element.elementId,
                    "mapping",
                    "No RemoteViews binding is registered"
                )
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
                    diagnostics += CalendarRenderDiagnostic(
                        element.elementId,
                        property,
                        "$property override ignored below API 31",
                        RemoteViewsGeometryCompat.MIN_DYNAMIC_GEOMETRY_API
                    )
                }
            }
            CalendarElementGeometry(
                elementId = element.elementId,
                binding = binding,
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
                dynamicGeometrySupported = supported
            )
        }
        return CalendarGeometryPlan(elements, diagnostics)
    }

    fun apply(remoteViews: RemoteViews, plan: CalendarGeometryPlan) {
        plan.elements.forEach { element ->
            remoteViews.setViewVisibility(
                element.binding.containerViewId,
                if (element.visible) View.VISIBLE else View.GONE
            )
            remoteViews.setViewPadding(
                element.binding.containerViewId,
                element.padding[0],
                element.padding[1],
                element.padding[2],
                element.padding[3]
            )
            remoteViews.setInt(element.binding.contentViewId, "setGravity", element.contentGravity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && element.dynamicGeometrySupported) {
                RemoteViewsGeometryCompat.applySizeAndPosition(
                    remoteViews,
                    element.binding.containerViewId,
                    element.widthDp,
                    element.heightDp,
                    element.startMarginDp,
                    element.topMarginDp
                )
            }
        }
    }

    private fun resolveDimension(mode: DimensionMode, fixed: Float, canvas: Float, wrap: Float): Float =
        when (mode) {
            DimensionMode.WRAP -> wrap
            DimensionMode.MATCH -> canvas
            DimensionMode.FIXED -> fixed
        }.coerceAtLeast(1f)

    private fun estimatedWrapSize(elementId: String): Pair<Float, Float> = when (elementId) {
        CalendarStudioDescriptor.TITLE -> 190f to 42f
        CalendarStudioDescriptor.DETAILS -> 190f to 28f
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

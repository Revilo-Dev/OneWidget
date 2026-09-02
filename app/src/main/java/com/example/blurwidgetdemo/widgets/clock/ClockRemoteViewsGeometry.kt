package com.example.blurwidgetdemo.widgets.clock

import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.clock.ClockStudioDescriptor
import com.example.blurwidgetdemo.studio.editor.AnchorPositioning
import com.example.blurwidgetdemo.studio.model.DimensionMode
import com.example.blurwidgetdemo.studio.model.HorizontalAlignment
import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign
import com.example.blurwidgetdemo.studio.model.VerticalAlignment
import com.example.blurwidgetdemo.studio.model.WidgetElementSpec

data class ClockElementViewBinding(
    val elementId: String,
    val containerViewId: Int,
    val contentViewIds: List<Int>
)

object ClockElementViewMap {
    val bindings: Map<String, ClockElementViewBinding> = listOf(
        ClockElementViewBinding(
            ClockStudioDescriptor.TIME,
            R.id.clock_time_wrapper,
            listOf(R.id.clock_time)
        ),
        ClockElementViewBinding(
            ClockStudioDescriptor.DATE,
            R.id.clock_date_wrapper,
            listOf(R.id.clock_date_above, R.id.clock_date)
        ),
        ClockElementViewBinding(
            ClockStudioDescriptor.DAY,
            R.id.clock_day_wrapper,
            listOf(R.id.clock_day)
        ),
        ClockElementViewBinding(
            ClockStudioDescriptor.BATTERY,
            R.id.clock_battery_wrapper,
            listOf(R.id.clock_battery)
        )
    ).associateBy { it.elementId }
}

data class ClockRenderDiagnostic(
    val elementId: String?,
    val property: String,
    val reason: String,
    val minimumApi: Int? = null
)

data class ClockElementGeometry(
    val elementId: String,
    val containerViewId: Int,
    val contentViewIds: List<Int>,
    val visible: Boolean,
    val widthDp: Float,
    val heightDp: Float,
    val startMarginDp: Float,
    val topMarginDp: Float,
    val paddingStartPx: Int,
    val paddingTopPx: Int,
    val paddingEndPx: Int,
    val paddingBottomPx: Int,
    val contentGravity: Int,
    val sizeAndPositionSupported: Boolean
)

data class ClockGeometryPlan(
    val elements: List<ClockElementGeometry>,
    val diagnostics: List<ClockRenderDiagnostic>
)

object ClockRemoteViewsGeometry {
    const val MIN_GEOMETRY_API = Build.VERSION_CODES.S

    fun dpToPx(valueDp: Float, density: Float): Int = (valueDp * density).toInt()

    fun plan(
        design: ResolvedWidgetDesign,
        renderSize: ClockRenderSize,
        density: Float,
        apiLevel: Int
    ): ClockGeometryPlan {
        val diagnostics = mutableListOf<ClockRenderDiagnostic>()
        val geometry = design.elements.mapNotNull { element ->
            val binding = ClockElementViewMap.bindings[element.elementId]
            if (binding == null) {
                diagnostics += ClockRenderDiagnostic(
                    elementId = element.elementId,
                    property = "mapping",
                    reason = "No RemoteViews binding is registered for this element"
                )
                return@mapNotNull null
            }
            val estimated = estimatedWrapSize(element.elementId)
            val width = resolveDimension(
                element.widthMode,
                element.widthDp,
                renderSize.widthDp,
                estimated.first + element.paddingStartDp + element.paddingEndDp
            )
            val height = resolveDimension(
                element.heightMode,
                element.heightDp,
                renderSize.heightDp,
                estimated.second + element.paddingTopDp + element.paddingBottomDp
            )
            val anchor = AnchorPositioning.anchorPoint(
                element.anchor,
                renderSize.widthDp,
                renderSize.heightDp
            )
            // Studio wrappers use a centred XML fallback. Margins are therefore offsets from
            // that centred position, yielding the same absolute centre as the EDIT canvas.
            val centredLeft = (renderSize.widthDp - width) / 2f
            val centredTop = (renderSize.heightDp - height) / 2f
            val supported = apiLevel >= MIN_GEOMETRY_API
            if (!supported) {
                listOf(
                    "width" to "Width override ignored below API 31",
                    "height" to "Height override ignored below API 31",
                    "position" to "Anchor and x/y position ignored below API 31"
                ).forEach { (property, reason) ->
                    diagnostics += ClockRenderDiagnostic(
                        elementId = element.elementId,
                        property = property,
                        reason = reason,
                        minimumApi = MIN_GEOMETRY_API
                    )
                }
            }
            ClockElementGeometry(
                elementId = element.elementId,
                containerViewId = binding.containerViewId,
                contentViewIds = binding.contentViewIds,
                visible = element.visible,
                widthDp = width,
                heightDp = height,
                startMarginDp = anchor.x + element.xDp - width / 2f - centredLeft,
                topMarginDp = anchor.y + element.yDp - height / 2f - centredTop,
                paddingStartPx = dpToPx(element.paddingStartDp, density),
                paddingTopPx = dpToPx(element.paddingTopDp, density),
                paddingEndPx = dpToPx(element.paddingEndDp, density),
                paddingBottomPx = dpToPx(element.paddingBottomDp, density),
                contentGravity = element.contentGravity(),
                sizeAndPositionSupported = supported
            )
        }
        return ClockGeometryPlan(geometry, diagnostics)
    }

    fun apply(remoteViews: RemoteViews, plan: ClockGeometryPlan) {
        plan.elements.forEach { element ->
            remoteViews.setViewVisibility(
                element.containerViewId,
                if (element.visible) View.VISIBLE else View.GONE
            )
            remoteViews.setViewPadding(
                element.containerViewId,
                element.paddingStartPx,
                element.paddingTopPx,
                element.paddingEndPx,
                element.paddingBottomPx
            )
            element.contentViewIds.forEach { contentViewId ->
                remoteViews.setInt(contentViewId, "setGravity", element.contentGravity)
            }
            if (Build.VERSION.SDK_INT >= MIN_GEOMETRY_API && element.sizeAndPositionSupported) {
                applySizeAndPosition(remoteViews, element)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applySizeAndPosition(
        remoteViews: RemoteViews,
        element: ClockElementGeometry
    ) {
        remoteViews.setViewLayoutWidth(
            element.containerViewId,
            element.widthDp,
            TypedValue.COMPLEX_UNIT_DIP
        )
        remoteViews.setViewLayoutHeight(
            element.containerViewId,
            element.heightDp,
            TypedValue.COMPLEX_UNIT_DIP
        )
        remoteViews.setViewLayoutMargin(
            element.containerViewId,
            RemoteViews.MARGIN_START,
            element.startMarginDp,
            TypedValue.COMPLEX_UNIT_DIP
        )
        remoteViews.setViewLayoutMargin(
            element.containerViewId,
            RemoteViews.MARGIN_TOP,
            element.topMarginDp,
            TypedValue.COMPLEX_UNIT_DIP
        )
    }

    private fun resolveDimension(
        mode: DimensionMode,
        fixedDp: Float,
        canvasDp: Float,
        estimatedWrapDp: Float
    ): Float = when (mode) {
        DimensionMode.WRAP -> estimatedWrapDp
        DimensionMode.MATCH -> canvasDp
        DimensionMode.FIXED -> fixedDp
    }.coerceAtLeast(1f)

    private fun estimatedWrapSize(elementId: String): Pair<Float, Float> = when (elementId) {
        ClockStudioDescriptor.TIME -> 150f to 62f
        ClockStudioDescriptor.DATE -> 176f to 28f
        ClockStudioDescriptor.DAY -> 132f to 28f
        ClockStudioDescriptor.BATTERY -> 82f to 28f
        else -> 80f to 28f
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

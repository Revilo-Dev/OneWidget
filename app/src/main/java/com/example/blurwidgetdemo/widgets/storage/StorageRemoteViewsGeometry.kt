package dev.revilodev.onewidget.widgets.storage

import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.editor.AnchorPositioning
import dev.revilodev.onewidget.studio.model.DimensionMode
import dev.revilodev.onewidget.studio.model.HorizontalAlignment
import dev.revilodev.onewidget.studio.model.ResolvedWidgetDesign
import dev.revilodev.onewidget.studio.model.VerticalAlignment
import dev.revilodev.onewidget.studio.model.WidgetElementSpec
import dev.revilodev.onewidget.studio.rendering.RemoteViewsGeometryCompat
import dev.revilodev.onewidget.studio.storage.StorageStudioDescriptor

data class StorageElementViewBinding(
    val elementId: String,
    val containerViewId: Int,
    val progressViewId: Int,
    val labelViewId: Int,
    val iconViewId: Int
)

object StorageElementViewMap {
    val bindings = listOf(
        StorageElementViewBinding(
            StorageStudioDescriptor.PRIMARY,
            R.id.storage_primary_wrapper,
            R.id.storage_primary_bar,
            R.id.storage_primary_label,
            R.id.storage_primary_icon
        ),
        StorageElementViewBinding(
            StorageStudioDescriptor.STORAGE,
            R.id.storage_storage_wrapper,
            R.id.storage_storage_bar,
            R.id.storage_storage_label,
            R.id.storage_storage_icon
        ),
        StorageElementViewBinding(
            StorageStudioDescriptor.MEMORY,
            R.id.storage_memory_wrapper,
            R.id.storage_memory_bar,
            R.id.storage_memory_label,
            R.id.storage_memory_icon
        ),
        StorageElementViewBinding(
            StorageStudioDescriptor.DATA,
            R.id.storage_data_wrapper,
            R.id.storage_data_bar,
            R.id.storage_data_label,
            R.id.storage_data_icon
        )
    ).associateBy { it.elementId }
}

data class StorageRenderDiagnostic(
    val elementId: String?,
    val property: String,
    val reason: String,
    val minimumApi: Int? = null
)

data class StorageElementGeometry(
    val elementId: String,
    val binding: StorageElementViewBinding,
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

data class StorageGeometryPlan(
    val elements: List<StorageElementGeometry>,
    val diagnostics: List<StorageRenderDiagnostic>
)

object StorageRemoteViewsGeometry {
    fun plan(
        design: ResolvedWidgetDesign,
        renderSize: StorageRenderSize,
        density: Float,
        apiLevel: Int
    ): StorageGeometryPlan {
        val diagnostics = mutableListOf<StorageRenderDiagnostic>()
        val elements = design.elements.mapNotNull { element ->
            val binding = StorageElementViewMap.bindings[element.elementId]
            if (binding == null) {
                diagnostics += StorageRenderDiagnostic(
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
                    diagnostics += StorageRenderDiagnostic(
                        element.elementId,
                        property,
                        "$property override ignored below API 31",
                        RemoteViewsGeometryCompat.MIN_DYNAMIC_GEOMETRY_API
                    )
                }
            }
            val radiusPlan = RemoteViewsGeometryCompat.radiusPlan(element.cornerRadiusDp, apiLevel)
            radiusPlan.diagnostic?.let {
                diagnostics += StorageRenderDiagnostic(
                    element.elementId,
                    "cornerRadius",
                    it,
                    RemoteViewsGeometryCompat.MIN_DYNAMIC_GEOMETRY_API
                )
            }
            StorageElementGeometry(
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
                cornerRadiusDp = element.cornerRadiusDp,
                dynamicGeometrySupported = supported
            )
        }
        return StorageGeometryPlan(elements, diagnostics)
    }

    fun apply(remoteViews: RemoteViews, plan: StorageGeometryPlan) {
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
            remoteViews.setInt(element.binding.labelViewId, "setGravity", element.contentGravity)
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
            RemoteViewsGeometryCompat.applyOutlineRadius(
                remoteViews,
                element.binding.containerViewId,
                element.cornerRadiusDp
            )
        }
    }

    fun progressRadiusFraction(element: StorageElementGeometry): Float =
        if (element.dynamicGeometrySupported) {
            (element.cornerRadiusDp / element.heightDp.coerceAtLeast(1f)).coerceIn(0f, 0.5f)
        } else {
            0.5f
        }

    private fun resolveDimension(mode: DimensionMode, fixed: Float, canvas: Float, wrap: Float): Float =
        when (mode) {
            DimensionMode.WRAP -> wrap
            DimensionMode.MATCH -> canvas
            DimensionMode.FIXED -> fixed
        }.coerceAtLeast(1f)

    private fun estimatedWrapSize(elementId: String): Pair<Float, Float> = when (elementId) {
        StorageStudioDescriptor.PRIMARY -> 160f to 40f
        StorageStudioDescriptor.STORAGE,
        StorageStudioDescriptor.MEMORY,
        StorageStudioDescriptor.DATA -> 180f to 30f
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

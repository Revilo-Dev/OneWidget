package com.example.blurwidgetdemo.studio.rendering

import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi

data class RemoteViewsRadiusPlan(
    val radiusDp: Float,
    val applied: Boolean,
    val diagnostic: String? = null
)

/** API-gated RemoteViews geometry operations shared by Studio widget renderers. */
object RemoteViewsGeometryCompat {
    const val MIN_DYNAMIC_GEOMETRY_API = Build.VERSION_CODES.S

    fun radiusPlan(radiusDp: Float, apiLevel: Int): RemoteViewsRadiusPlan = when {
        radiusDp <= 0f -> RemoteViewsRadiusPlan(radiusDp.coerceAtLeast(0f), applied = false)
        apiLevel >= MIN_DYNAMIC_GEOMETRY_API -> RemoteViewsRadiusPlan(radiusDp, applied = true)
        else -> RemoteViewsRadiusPlan(
            radiusDp,
            applied = false,
            diagnostic = "Dynamic corner radius ignored below API 31; XML radius retained"
        )
    }

    fun applyOutlineRadius(remoteViews: RemoteViews, viewId: Int, radiusDp: Float): RemoteViewsRadiusPlan {
        val plan = radiusPlan(radiusDp, Build.VERSION.SDK_INT)
        if (plan.applied && Build.VERSION.SDK_INT >= MIN_DYNAMIC_GEOMETRY_API) {
            applyOutlineRadiusApi31(remoteViews, viewId, radiusDp)
        }
        return plan
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyOutlineRadiusApi31(remoteViews: RemoteViews, viewId: Int, radiusDp: Float) {
        remoteViews.setViewOutlinePreferredRadius(viewId, radiusDp, TypedValue.COMPLEX_UNIT_DIP)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun applySizeAndPosition(
        remoteViews: RemoteViews,
        viewId: Int,
        widthDp: Float,
        heightDp: Float,
        startMarginDp: Float,
        topMarginDp: Float
    ) {
        remoteViews.setViewLayoutWidth(viewId, widthDp, TypedValue.COMPLEX_UNIT_DIP)
        remoteViews.setViewLayoutHeight(viewId, heightDp, TypedValue.COMPLEX_UNIT_DIP)
        remoteViews.setViewLayoutMargin(
            viewId,
            RemoteViews.MARGIN_START,
            startMarginDp,
            TypedValue.COMPLEX_UNIT_DIP
        )
        remoteViews.setViewLayoutMargin(
            viewId,
            RemoteViews.MARGIN_TOP,
            topMarginDp,
            TypedValue.COMPLEX_UNIT_DIP
        )
    }
}

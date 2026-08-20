package com.example.blurwidgetdemo.widgets.extra

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/** Creates geometry that remains a circle or a true capsule after RemoteViews scaling. */
object ToggleOverlayRenderer {
    fun render(widthDp: Int, heightDp: Int, colour: Int): Bitmap {
        val width = widthDp.coerceAtLeast(1) * 4
        val height = heightDp.coerceAtLeast(1) * 4
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val overscan = 3f
            val radius = minOf(width, height) / 2f + overscan
            Canvas(bitmap).drawRoundRect(-overscan, -overscan, width + overscan, height + overscan, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour })
        }
    }
}

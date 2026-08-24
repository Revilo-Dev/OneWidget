package com.example.blurwidgetdemo.widgets.extra

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/** Creates a centred circle within the toggle's wide 1×2 glass surface. */
object ToggleOverlayRenderer {
    fun renderCircle(widthDp: Int, heightDp: Int, colour: Int): Bitmap {
        val width = widthDp.coerceAtLeast(1) * 4
        val height = heightDp.coerceAtLeast(1) * 4
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val overscan = 3f
            val radius = minOf(width, height) / 2f + overscan
            Canvas(bitmap).drawCircle(width / 2f, height / 2f, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour })
        }
    }

    fun renderFill(widthDp: Int, heightDp: Int, colour: Int): Bitmap {
        val width = widthDp.coerceAtLeast(1) * 4
        val height = heightDp.coerceAtLeast(1) * 4
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val radius = height / 2f + 3f
            Canvas(bitmap).drawRoundRect(-3f, -3f, width + 3f, height + 3f, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour })
        }
    }
}

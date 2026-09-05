package com.example.blurwidgetdemo.widgets.extra

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.PorterDuff

/** Creates transparent overlays; the compact toggle deliberately uses a square bitmap. */
object ToggleOverlayRenderer {
    fun renderCircle(widthDp: Int, heightDp: Int, colour: Int): Bitmap {
        val diameter = minOf(widthDp.coerceAtLeast(1), heightDp.coerceAtLeast(1)) * 4
        return Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour })
        }
    }

    fun renderFill(widthDp: Int, heightDp: Int, colour: Int): Bitmap {
        val width = widthDp.coerceAtLeast(1) * 4
        val height = heightDp.coerceAtLeast(1) * 4
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            // A radius at least as large as the height remains a capsule after host scaling.
            val radius = height.toFloat()
            Canvas(bitmap).apply {
                drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawRoundRect(-3f, -3f, width + 3f, height + 3f, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour })
            }
        }
    }
}

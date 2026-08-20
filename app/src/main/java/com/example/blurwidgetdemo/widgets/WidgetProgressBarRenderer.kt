package com.example.blurwidgetdemo.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import kotlin.math.roundToInt

/** Renders a colour-aware progress bar. The moving highlight position changes on each refresh. */
object WidgetProgressBarRenderer {
    private const val WIDTH = 600
    private const val HEIGHT = 60

    fun render(progress: Int, colour: Int, animated: Boolean, now: Long = System.currentTimeMillis()): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = HEIGHT / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(54, 255, 255, 255)
        canvas.drawRoundRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), radius, radius, paint)

        val fillWidth = (WIDTH * progress.coerceIn(0, 100) / 100f).coerceAtLeast(0f)
        if (fillWidth > 0f) {
            val base = colour or 0xFF000000.toInt()
            if (!animated) {
                paint.color = base
                canvas.drawRoundRect(0f, 0f, fillWidth, HEIGHT.toFloat(), radius, radius, paint)
                return bitmap
            }
            val phase = ((now / 1_000L) % 12L).toFloat() / 12f
            val dark = blend(base, Color.BLACK, 0.24f)
            val light = blend(base, Color.WHITE, 0.38f)
            // The highlight's offset is time based, so each scheduled or state refresh advances it.
            paint.shader = LinearGradient(
                -WIDTH + (WIDTH * 2f * phase), 0f,
                WIDTH * phase, 0f,
                intArrayOf(dark, base, light, base, dark),
                floatArrayOf(0f, .34f, .5f, .66f, 1f),
                Shader.TileMode.MIRROR
            )
            canvas.save()
            canvas.clipRect(0f, 0f, fillWidth, HEIGHT.toFloat())
            canvas.drawRoundRect(0f, 0f, fillWidth, HEIGHT.toFloat(), radius, radius, paint)
            canvas.restore()
            paint.shader = null
        }
        return bitmap
    }

    private fun blend(from: Int, to: Int, amount: Float): Int = Color.rgb(
        (Color.red(from) + (Color.red(to) - Color.red(from)) * amount).roundToInt(),
        (Color.green(from) + (Color.green(to) - Color.green(from)) * amount).roundToInt(),
        (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount).roundToInt()
    )
}

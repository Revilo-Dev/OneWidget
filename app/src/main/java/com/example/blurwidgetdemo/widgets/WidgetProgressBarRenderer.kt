package com.example.blurwidgetdemo.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
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
                drawFill(canvas, fillWidth, radius, paint)
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
            paint.alpha = 255
            drawFill(canvas, fillWidth, radius, paint)
            paint.shader = null
        }
        return bitmap
    }

    /** A fixed left-to-right palette for widgets whose colours communicate status. */
    fun render(progress: Int, colours: IntArray): Bitmap {
        require(colours.size >= 2) { "A gradient needs at least two colours" }
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = HEIGHT / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(54, 255, 255, 255) }
        canvas.drawRoundRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), radius, radius, paint)
        val fillWidth = WIDTH * progress.coerceIn(0, 100) / 100f
        if (fillWidth > 0f) {
            paint.shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), 0f,
                colours.map { it or 0xFF000000.toInt() }.toIntArray(),
                null,
                Shader.TileMode.CLAMP
            )
            // The track is intentionally translucent. Reset that alpha before drawing the fill;
            // otherwise Paint carries its 21% alpha into the shader and mutes every gradient.
            paint.alpha = 255
            drawFill(canvas, fillWidth, radius, paint)
            paint.shader = null
        }
        return bitmap
    }

    /** Playback progress with a bright, touch-target-like thumb at the current position. */
    fun renderMedia(progress: Int, colour: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = HEIGHT / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val track = Path().apply { addRoundRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), radius, radius, Path.Direction.CW) }
        paint.color = Color.argb(70, 255, 255, 255)
        canvas.drawRoundRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), radius, radius, paint)
        canvas.save()
        // Keep the fill and thumb glow inside the pill, including at 0% and 100%.
        canvas.clipPath(track)

        val fillWidth = WIDTH * progress.coerceIn(0, 100) / 100f
        val base = colour or 0xFF000000.toInt()
        if (fillWidth > 0f) {
            paint.shader = LinearGradient(
                0f, 0f, fillWidth.coerceAtLeast(1f), 0f,
                intArrayOf(darker(base, .16f), base, lighter(base, .22f)),
                null,
                Shader.TileMode.CLAMP
            )
            drawFill(canvas, fillWidth, radius, paint)
            paint.shader = null
        }

        // Widgets cannot receive drag coordinates, but the illuminated thumb still clearly
        // communicates the current seek position and makes the bar read like a media control.
        val thumbX = fillWidth.coerceIn(0f, WIDTH.toFloat())
        paint.shader = RadialGradient(
            thumbX, HEIGHT / 2f, HEIGHT / 2f,
            intArrayOf(Color.argb(125, Color.red(base), Color.green(base), Color.blue(base)), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(thumbX, HEIGHT / 2f, HEIGHT / 2f, paint)
        paint.shader = null
        paint.color = lighter(base, .35f)
        canvas.drawCircle(thumbX, HEIGHT / 2f, HEIGHT * .20f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(thumbX, HEIGHT / 2f, HEIGHT * .115f, paint)
        canvas.restore()
        return bitmap
    }

    fun lighter(colour: Int, amount: Float = .1f): Int = blend(colour or 0xFF000000.toInt(), Color.WHITE, amount)
    fun darker(colour: Int, amount: Float = .1f): Int = blend(colour or 0xFF000000.toInt(), Color.BLACK, amount)

    private fun blend(from: Int, to: Int, amount: Float): Int = Color.rgb(
        (Color.red(from) + (Color.red(to) - Color.red(from)) * amount).roundToInt(),
        (Color.green(from) + (Color.green(to) - Color.green(from)) * amount).roundToInt(),
        (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount).roundToInt()
    )

    /** Uses a half-width cap for short fills so both ends stay completely circular. */
    private fun drawFill(canvas: Canvas, fillWidth: Float, radius: Float, paint: Paint) {
        val fillRadius = minOf(radius, fillWidth / 2f)
        canvas.drawRoundRect(0f, 0f, fillWidth, HEIGHT.toFloat(), fillRadius, fillRadius, paint)
    }
}

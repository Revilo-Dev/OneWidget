package dev.revilodev.onewidget.widgets.clock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One UI Home may replace a TextClock's custom Typeface with its default face.
 * Rasterising the selected face in the app makes every bundled clock font render
 * identically in the launcher, configuration preview, and Studio preview.
 */
object ClockFontBitmapRenderer {
    fun render(
        context: Context,
        font: ClockFont,
        pattern: String,
        widthDp: Float,
        heightDp: Float,
        textScalePercent: Int,
        textColor: Int,
        sample: String? = null
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (widthDp * density).toInt().coerceAtLeast(1)
        val height = (heightDp * density).toInt().coerceAtLeast(1)
        val text = sample ?: runCatching {
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
        }.getOrDefault("12:45")
        val typeface = font.fontResource()?.let { ResourcesCompat.getFont(context, it) }
            ?: Typeface.DEFAULT
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = textColor
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = height * 0.70f * textScalePercent.coerceIn(50, 150) / 100f
        }
        // Keep ornate faces inside the allocated clock row rather than clipping their ascenders.
        while (paint.measureText(text) > width * 0.94f && paint.textSize > 8f) paint.textSize *= 0.94f
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val baseline = height / 2f - (paint.ascent() + paint.descent()) / 2f
            Canvas(bitmap).drawText(text, width / 2f, baseline, paint)
        }
    }
}

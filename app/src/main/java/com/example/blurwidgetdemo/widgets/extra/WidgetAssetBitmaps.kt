package dev.revilodev.onewidget.widgets.extra

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri

object WidgetAssetBitmaps {
    fun fromUri(context: Context, value: String?): Bitmap? = runCatching {
        val uri = value?.let(Uri::parse) ?: return null
        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
    }.getOrNull()

    fun emoji(value: String, sizePx: Int = 160): Bitmap {
        val safeSize = sizePx.coerceAtLeast(1)
        return Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = safeSize * 0.66f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val baseline = safeSize / 2f - (paint.ascent() + paint.descent()) / 2f
            canvas.drawText(value, safeSize / 2f, baseline, paint)
        }
    }

    fun fromDrawable(drawable: Drawable): Bitmap {
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            drawable.setBounds(0, 0, width, height)
            drawable.draw(Canvas(bitmap))
        }
    }
}

package dev.revilodev.onewidget.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.ui.SurfaceHighlight
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object WidgetSurface {
    private val images = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val decorations = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    fun update(context: Context, manager: AppWidgetManager, id: Int, content: RemoteViews) {
        val (width, height) = WidgetInitialSizeSpec.resolvedBounds(manager, id, 180, 80)
        WidgetUpdates.ensureScheduled(context)
        WidgetTextAppearance.apply(context, id, content)
        val options = manager.getAppWidgetOptions(id)
        @Suppress("DEPRECATION")
        val sizes = if (Build.VERSION.SDK_INT >= 31) options
            .getParcelableArrayList<android.util.SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            .orEmpty().filter { it.width > 0 && it.height > 0 }.distinct().take(16) else emptyList()
        val gridSpan = WidgetInitialSizeSpec.launcherGridSpan(options)
        val views = if (Build.VERSION.SDK_INT >= 31 && sizes.size > 1) {
            RemoteViews(sizes.associateWith { size -> wrap(context, id, content, size.width, size.height, pixelBudget = 500_000 / sizes.size, gridSpan = gridSpan) })
        } else wrap(context, id, content, width.toFloat(), height.toFloat(), gridSpan = gridSpan)
        manager.updateAppWidget(id, views)
    }

    fun wrap(
        context: Context,
        id: Int,
        content: RemoteViews,
        widthDp: Float,
        heightDp: Float,
        color: Int? = null,
        border: WidgetBorderStyle? = null,
        backgroundUri: String? = null,
        useStoredImage: Boolean = true,
        pixelBudget: Int = 500_000,
        gridSpan: android.util.Size? = null
    ): RemoteViews {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        val surfaceColor = color ?: if (prefs.getBoolean("background_enabled_" + id, true)) BlurWidget.tintColor(prefs, id) else Color.TRANSPARENT
        val style = (border ?: WidgetAppearance.border(prefs, id)).normalized()
        val uri = if (useStoredImage) WidgetAppearance.backgroundUri(prefs, id) else backgroundUri
        val artwork = WidgetBackgroundArtwork.drawable(gridSpan, widthDp, heightDp)
        val radiusDp = WidgetBackgroundArtwork.radius(widthDp, heightDp, gridSpan, artwork)
        val density = context.resources.displayMetrics.density
        val rawWidth = (widthDp * density).coerceAtLeast(1f)
        val rawHeight = (heightDp * density).coerceAtLeast(1f)
        val scale = minOf(1f, sqrt(pixelBudget.toFloat() / (rawWidth * rawHeight)))
        val width = (rawWidth * scale).roundToInt().coerceAtLeast(1)
        val height = (rawHeight * scale).roundToInt().coerceAtLeast(1)
        val radius = radiusDp * density * scale
        @Suppress("DEPRECATION")
        val inner = (if (Build.VERSION.SDK_INT >= 28) RemoteViews(content) else content.clone()).apply {
            setInt(android.R.id.background, "setBackgroundResource", 0)
            setBoolean(android.R.id.background, "setClipToOutline", false)
        }
        return RemoteViews(context.packageName, R.layout.widget_surface).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val background = artwork ?: if (radiusDp <= WidgetSurfaceGeometry.WIDE_TWO_ROW_RADIUS_DP) {
                    R.drawable.widget_surface_background_tight
                } else R.drawable.widget_surface_background
                setInt(android.R.id.background, "setBackgroundResource", background)
                BlurWidget.applySurfaceColor(this, surfaceColor)
                setViewOutlinePreferredRadius(android.R.id.background, radiusDp, TypedValue.COMPLEX_UNIT_DIP)
            } else setInt(android.R.id.background, "setBackgroundColor", Color.TRANSPARENT)
            removeAllViews(R.id.widget_surface_content)
            addView(R.id.widget_surface_content, inner)
            val image = uri?.let { loadImage(context, it) }
            if (image != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val key = "image:" + uri + ":" + width + ":" + height + ":" + radius + ":" + surfaceColor + ":" + artwork
                val bitmap = cached(key) {
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
                        val canvas = Canvas(output)
                        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
                        val path = Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) }
                        canvas.clipPath(path)
                        if (image != null) {
                            val factor = max(width.toFloat() / image.width, height.toFloat() / image.height)
                            val left = (width - image.width * factor) / 2f
                            val top = (height - image.height * factor) / 2f
                            canvas.drawBitmap(image, null, RectF(left, top, width - left, height - top), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        }
                        if (artwork != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            context.getDrawable(artwork)?.mutate()?.apply {
                                setTint(surfaceColor)
                                setBounds(0, 0, width, height)
                                draw(canvas)
                            }
                        } else canvas.drawColor(surfaceColor)
                    }
                }
                setViewVisibility(R.id.widget_surface_decoration, View.VISIBLE)
                setImageViewBitmap(R.id.widget_surface_decoration, bitmap)
            } else setViewVisibility(R.id.widget_surface_decoration, View.GONE)
            if (style.enabled && style.opacityPercent > 0) {
                setViewVisibility(R.id.widget_surface_border, View.VISIBLE)
                val thickness = style.thicknessPx * scale
                val key = "border:" + width + ":" + height + ":" + radius + ":" + thickness + ":" + style.opacityPercent
                setImageViewBitmap(R.id.widget_surface_border, cached(key) {
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                        SurfaceHighlight(thickness, style.opacityPercent).draw(
                            Canvas(bitmap), 0f, 0f, width.toFloat(), height.toFloat(), radius
                        )
                    }
                })
            } else setViewVisibility(R.id.widget_surface_border, View.GONE)
        }
    }

    private fun cached(key: String, create: () -> Bitmap): Bitmap = decorations.get(key)
        ?: create().also { decorations.put(key, it) }

    fun loadImage(context: Context, uri: String): Bitmap? {
        images.get(uri)?.let { return it }
        return runCatching {
            val source = Uri.parse(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null
            var sample = 1
            while (max(options.outWidth, options.outHeight) / sample > 1024) sample *= 2
            options.inJustDecodeBounds = false
            options.inSampleSize = sample
            val decoded = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
            val orientation = runCatching {
                context.contentResolver.openInputStream(source)?.use {
                    android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)
                }
            }.getOrNull() ?: 1
            val matrix = android.graphics.Matrix().apply {
                when (orientation) {
                    2 -> setScale(-1f, 1f)
                    3 -> setRotate(180f)
                    4 -> setScale(1f, -1f)
                    5 -> { setRotate(90f); postScale(-1f, 1f) }
                    6 -> setRotate(90f)
                    7 -> { setRotate(-90f); postScale(-1f, 1f) }
                    8 -> setRotate(-90f)
                }
            }
            val bitmap = if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
            images.put(uri, bitmap)
            bitmap
        }.getOrNull()
    }
}

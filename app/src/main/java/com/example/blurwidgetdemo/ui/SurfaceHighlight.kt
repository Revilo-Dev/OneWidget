package dev.revilodev.onewidget.ui

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import dev.revilodev.onewidget.R
import org.xmlpull.v1.XmlPullParser
import kotlin.math.roundToInt

// ported from surfacehighlight in the revilo dev oneui toolkit
internal class SurfaceHighlight(
    private val thickness: Float,
    opacityPercent: Int = 20
) {
    private val startColor = Color.argb((opacityPercent * 255 / 100f).roundToInt(), 255, 255, 255)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = thickness
    }
    private val edge = RectF()
    private var gradientTop = Float.NaN
    private var gradientBottom = Float.NaN

    fun draw(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, radius: Float, alpha: Int = 255) {
        if (right - left <= thickness || bottom - top <= thickness || alpha == 0) return
        if (top != gradientTop || bottom != gradientBottom) {
            paint.shader = LinearGradient(0f, top, 0f, bottom, startColor, 0x00ffffff, Shader.TileMode.CLAMP)
            gradientTop = top
            gradientBottom = bottom
        }
        val inset = thickness / 2f
        edge.set(left + inset, top + inset, right - inset, bottom - inset)
        val rounded = (minOf(radius, (right - left) / 2f, (bottom - top) / 2f) - inset).coerceAtLeast(0f)
        paint.alpha = alpha
        canvas.drawRoundRect(edge, rounded, rounded, paint)
    }

    companion object {
        const val CHROME_OPACITY = 15
        fun chromeWidth(density: Float) = (density * 0.55f).coerceAtLeast(0.75f)
    }
}

class HighlightedSurfaceDrawable() : GradientDrawable() {
    private var highlight: SurfaceHighlight? = null
    private var verticalPadding = 0

    constructor(color: Int, radius: Float, thickness: Float, opacityPercent: Int = SurfaceHighlight.CHROME_OPACITY) : this() {
        setColor(color)
        cornerRadius = radius
        highlight = SurfaceHighlight(thickness, opacityPercent)
    }

    override fun inflate(resources: Resources, parser: XmlPullParser, attrs: AttributeSet, theme: Resources.Theme?) {
        val attributes = R.styleable.HighlightedSurfaceDrawable
        val values = theme?.obtainStyledAttributes(attrs, attributes, 0, 0)
            ?: resources.obtainAttributes(attrs, attributes)
        val radius: Float
        try {
            verticalPadding = values.getDimensionPixelSize(R.styleable.HighlightedSurfaceDrawable_android_padding, 0)
            radius = values.getDimension(R.styleable.HighlightedSurfaceDrawable_android_radius, 0f)
        } finally {
            values.recycle()
        }
        super.inflate(resources, parser, attrs, theme)
        cornerRadius = radius
        highlight = SurfaceHighlight(SurfaceHighlight.chromeWidth(resources.displayMetrics.density), SurfaceHighlight.CHROME_OPACITY)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        highlight?.draw(canvas, bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(),
            bounds.bottom.toFloat(), cornerRadius, alpha)
    }

    override fun getPadding(padding: android.graphics.Rect): Boolean {
        padding.set(0, verticalPadding, 0, verticalPadding)
        return verticalPadding != 0
    }

    override fun getConstantState(): ConstantState? = null
}

internal fun android.view.View.applyCardHighlight() {
    val density = resources.displayMetrics.density
    background = HighlightedSurfaceDrawable(
        androidx.core.content.ContextCompat.getColor(context, dev.revilodev.onewidget.R.color.oneui_card_bg),
        26f * density, SurfaceHighlight.chromeWidth(density)
    )
    clipToOutline = true
}

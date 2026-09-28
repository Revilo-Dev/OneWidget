package dev.revilodev.onewidget.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView

/** Fades are drawing only: they have no touch target or accessibility node. */
class FlowScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {
    internal val gesture = FlowScrollState()
    private val paint = Paint()
    private val visibleBounds = Rect()
    private val location = IntArray(2)
    private val fadeHeight = 56f * resources.displayMetrics.density
    private val backgroundColor = context.obtainStyledAttributes(attrs,
        intArrayOf(dev.revilodev.onewidget.R.attr.flowFadeBackground)).let { values ->
        try { values.getColor(0, flowThemeColor(context, androidx.appcompat.R.attr.roundedCornerColor)) }
        finally { values.recycle() }
    }
    private val transparent = backgroundColor and 0x00ffffff
    private val topGradient = LinearGradient(0f, 0f, 0f, fadeHeight,
        backgroundColor, transparent, Shader.TileMode.CLAMP)
    private val bottomGradient = LinearGradient(0f, 0f, 0f, fadeHeight,
        transparent, backgroundColor, Shader.TileMode.CLAMP)

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) gesture.beginGesture(canScrollVertically(-1))
        return super.dispatchTouchEvent(event)
    }

    override fun onScrollChanged(left: Int, top: Int, oldLeft: Int, oldTop: Int) {
        super.onScrollChanged(left, top, oldLeft, oldTop)
        gesture.contentScrolled(canScrollVertically(-1))
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (!getGlobalVisibleRect(visibleBounds)) return
        getLocationOnScreen(location)
        val top = (visibleBounds.top - location[1]).toFloat()
        val bottom = (visibleBounds.bottom - location[1]).toFloat()
        val checkpoint = canvas.save()
        canvas.translate(scrollX.toFloat(), scrollY.toFloat())
        if (canScrollVertically(-1)) drawFade(canvas, top, topGradient)
        if (canScrollVertically(1)) drawFade(canvas, bottom - fadeHeight, bottomGradient)
        canvas.restoreToCount(checkpoint)
    }

    private fun drawFade(canvas: Canvas, y: Float, gradient: Shader) {
        val checkpoint = canvas.save()
        canvas.translate(0f, y)
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), fadeHeight, paint)
        canvas.restoreToCount(checkpoint)
    }
}

internal fun View.descendants(): Sequence<View> = sequence {
    yield(this@descendants)
    if (this@descendants is ViewGroup) {
        for (index in 0 until childCount) yieldAll(getChildAt(index).descendants())
    }
}

internal fun flowThemeColor(context: Context, attribute: Int): Int =
    context.obtainStyledAttributes(intArrayOf(attribute)).let { values ->
        try { values.getColor(0, 0) } finally { values.recycle() }
    }

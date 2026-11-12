package dev.revilodev.onewidget.ui

import android.animation.ValueAnimator
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageButton
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.FlowContentBehavior

object FlowToolbar {
    fun install(root: ViewGroup, appBar: AppBarLayout, toolbar: Toolbar) {
        val scroll = root.descendants().filterIsInstance<FlowScrollView>().firstOrNull() ?: return
        val density = root.resources.displayMetrics.density
        val toolbarHeight = (56 * density).toInt()
        fun sizeToolbar() {
            toolbar.layoutParams = toolbar.layoutParams.apply { height = toolbarHeight }
            toolbar.minimumHeight = toolbarHeight
            toolbar.setPadding(toolbar.paddingLeft, 0, toolbar.paddingRight, 0)
            appBar.seslSetCollapsedHeight(toolbarHeight.toFloat())
            if (!appBar.isEnabled) appBar.seslSetCustomHeight(toolbarHeight)
        }
        toolbar.seslSetUserTopPadding(0)
        sizeToolbar()
        // sesl resets toolbar dimensions when it attaches
        toolbar.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = sizeToolbar()
            override fun onViewDetachedFromWindow(view: View) = Unit
        })
        toolbar.elevation = 0f
        appBar.background = null
        appBar.stateListAnimator = null
        appBar.elevation = 0f
        appBar.isLiftOnScroll = false
        appBar.descendants().filterIsInstance<CollapsingToolbarLayout>().forEach {
            it.background = null
            it.contentScrim = null
            it.statusBarScrim = null
            it.minimumHeight = toolbarHeight
        }
        val coordinator = appBar.parent as CoordinatorLayout
        (appBar.layoutParams as CoordinatorLayout.LayoutParams).behavior = FlowAppBarBehavior().apply {
            setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout) = !scroll.canScrollVertically(-1)
            })
        }
        // the coordinator only applies scrolling behavior to its direct children
        var content: View = scroll
        while (content.parent !== coordinator && content.parent is View) content = content.parent as View
        if (content.parent === coordinator) {
            (content.layoutParams as CoordinatorLayout.LayoutParams).behavior = FlowContentBehavior(toolbarHeight)
            content.background = null
            scroll.setPadding(scroll.paddingLeft, scroll.paddingTop + toolbarHeight,
                scroll.paddingRight, scroll.paddingBottom)
            scroll.clipToPadding = false
        }
        appBar.bringToFront()
        val surfaces = ControlSurfaces(toolbar)
        toolbar.background = surfaces
        var collapsed = false
        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { bar, offset ->
            collapsed = bar.totalScrollRange == 0 || -offset >= bar.totalScrollRange
        })
        val update = ViewTreeObserver.OnPreDrawListener {
            surfaces.active = FlowScrollState.showControlSurfaces(
                collapsed || appBar.totalScrollRange == 0, scroll.canScrollVertically(-1))
            toolbar.descendants().filterIsInstance<ActionMenuView>().forEach { menu ->
                val padding = (4 * density).toInt()
                if (menu.paddingLeft != padding || menu.paddingRight != padding) {
                    menu.setPadding(padding, menu.paddingTop, padding, menu.paddingBottom)
                }
            }
            true
        }
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) { view.viewTreeObserver.addOnPreDrawListener(update) }
            override fun onViewDetachedFromWindow(view: View) { view.viewTreeObserver.removeOnPreDrawListener(update) }
        })
        if (root.isAttachedToWindow) root.viewTreeObserver.addOnPreDrawListener(update)
    }

    private class ControlSurfaces(private val toolbar: Toolbar) : Drawable() {
        private val density = toolbar.resources.displayMetrics.density
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val dark = toolbar.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            color = if (dark) (flowThemeColor(toolbar.context, android.R.attr.colorBackground) and 0x00ffffff) or (224 shl 24)
                else Color.WHITE
        }
        private val fillAlpha = paint.alpha
        private val highlight = SurfaceHighlight(SurfaceHighlight.chromeWidth(density), SurfaceHighlight.CHROME_OPACITY)
        private val rect = RectF()
        private var surfaceAlpha = 0f
        private var fade: ValueAnimator? = null
        var active = false
            set(value) {
                if (field == value) return
                field = value
                fade?.cancel()
                fade = ValueAnimator.ofFloat(surfaceAlpha, if (value) 1f else 0f).apply {
                    duration = 180
                    addUpdateListener {
                        surfaceAlpha = it.animatedValue as Float
                        invalidateSelf()
                    }
                    start()
                }
            }

        override fun draw(canvas: Canvas) {
            if (surfaceAlpha == 0f) return
            paint.alpha = (fillAlpha * surfaceAlpha).toInt()
            for (index in 0 until toolbar.childCount) {
                val child = toolbar.getChildAt(index)
                if (child.visibility != View.VISIBLE) continue
                if (child is ActionMenuView && child.childCount > 0) {
                    rect.set(child.left.toFloat(), child.top.toFloat(), child.right.toFloat(), child.bottom.toFloat())
                    rect.offset(if (child.layoutDirection == View.LAYOUT_DIRECTION_RTL) 6 * density else -6 * density, 0f)
                    drawSurface(canvas, 26 * density)
                } else if (child is ImageButton && child.drawable === toolbar.navigationIcon) {
                    val centerX = (child.left + child.right + child.paddingLeft - child.paddingRight) / 2f +
                        (if (child.layoutDirection == View.LAYOUT_DIRECTION_RTL) -4f else 4f) * density
                    val centerY = (child.top + child.bottom) / 2f
                    val radius = minOf(child.width, child.height) / 2f - 4 * density
                    rect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
                    drawSurface(canvas, radius)
                }
            }
        }
        private fun drawSurface(canvas: Canvas, radius: Float) {
            canvas.drawRoundRect(rect, radius, radius, paint)
            highlight.draw(canvas, rect.left, rect.top, rect.right, rect.bottom, radius, (255 * surfaceAlpha).toInt())
        }

        override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
        @Deprecated("Deprecated in Android")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}

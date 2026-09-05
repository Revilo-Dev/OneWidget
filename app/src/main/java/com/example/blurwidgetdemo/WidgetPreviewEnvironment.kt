package com.example.blurwidgetdemo

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

/** A neutral, theme-aware stage behind transparent RemoteViews previews. */
object WidgetPreviewEnvironment {
    /**
     * Kept as a real one-argument overload so code loaded from an earlier
     * widget process can continue to call it while an app update is applied.
     */
    fun apply(view: View) = apply(view, fullyRounded = false)

    fun apply(view: View, fullyRounded: Boolean) {
        view.background = GradientDrawable().apply {
            setColor(0x66000000)
            setStroke((view.context.resources.displayMetrics.density * .25f).toInt().coerceAtLeast(1), 0x40FFFFFF.toInt())
            cornerRadius = dp(view.context, if (fullyRounded) 1_000 else 28).toFloat()
        }
    }

    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
}

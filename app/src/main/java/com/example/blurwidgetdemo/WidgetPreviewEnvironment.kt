package com.example.blurwidgetdemo

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

/** A neutral, theme-aware stage behind transparent RemoteViews previews. */
object WidgetPreviewEnvironment {
    fun apply(view: View) {
        val dark = (view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        view.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            if (dark) intArrayOf(0xFF263145.toInt(), 0xFF111722.toInt())
            else intArrayOf(0xFFE8EFFA.toInt(), 0xFFC9D6E8.toInt())
        ).apply { cornerRadius = dp(view.context, 18).toFloat() }
    }

    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
}

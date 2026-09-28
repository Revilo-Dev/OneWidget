package dev.revilodev.onewidget.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R

/** Shared by the picker, configuration previews and exact three-column providers. */
object WidgetCorners {
    private val threeColumnProvider = Regex("3x[12]Widget$")

    fun isThreeColumn(provider: String?) = provider?.let(threeColumnProvider::containsMatchIn) == true

    fun apply(context: Context, manager: AppWidgetManager, id: Int, views: RemoteViews) {
        apply(context, manager.getAppWidgetInfo(id)?.provider?.className, views)
    }

    fun apply(context: Context, provider: String?, views: RemoteViews) {
        if (!isThreeColumn(provider)) return
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        val borders = WidgetAppearance.bordersEnabled(prefs) && provider?.contains("MediaPlayer") != true
        views.setInt(android.R.id.background, "setBackgroundResource", resource(borders))
    }

    fun resource(borders: Boolean) = if (borders) R.drawable.widget_background_three_column
        else R.drawable.widget_background_three_column_no_border
}

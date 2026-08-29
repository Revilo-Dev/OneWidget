package com.example.blurwidgetdemo.widgets.extra

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetAppearance

abstract class NamedActionWidget : AppWidgetProvider() {
    abstract val title: String
    abstract val icon: Int
    abstract fun action(context: Context, id: Int): Intent
    protected open fun displayText(context: Context, id: Int) = title
    protected open fun textColor(prefs: SharedPreferences, id: Int) = WidgetAppearance.textColor(prefs, id)
    protected open fun startLabelPadding(context: Context) = 0
    protected open fun iconBitmap(context: Context, id: Int): Bitmap? = null
    protected open val layoutResource = R.layout.widget_action
    protected open fun layoutResource(context: Context, manager: AppWidgetManager, id: Int) = layoutResource
    protected open fun applyContent(views: RemoteViews, context: Context, id: Int) {
        iconBitmap(context, id)?.let { views.setImageViewBitmap(R.id.action_icon, it) } ?: views.setImageViewResource(R.id.action_icon, icon)
        views.setTextViewText(R.id.action_label, displayText(context, id))
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { render(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) = render(context, manager, id)
    private fun render(context: Context, manager: AppWidgetManager, id: Int) {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        val iconOnly = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 80) <= 100 && manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
        RemoteViews(context.packageName, layoutResource(context, manager, id)).apply {
            BlurWidget.applySurfaceTint(this, prefs, id)
            applyContent(this, context, id)
            setViewVisibility(R.id.action_label, if (iconOnly) View.GONE else View.VISIBLE)
            setTextColor(R.id.action_label, textColor(prefs, id))
            setInt(R.id.action_label, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
            val padding = if (WidgetAppearance.alignment(prefs, id) == WidgetAppearance.Alignment.START) startLabelPadding(context) else 0
            setViewPadding(R.id.action_label, padding, 0, 0, 0)
            setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, id, action(context, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }.also { manager.updateAppWidget(id, it) }
    }
}

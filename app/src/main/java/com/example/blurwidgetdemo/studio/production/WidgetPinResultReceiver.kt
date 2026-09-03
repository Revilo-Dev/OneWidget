package com.example.blurwidgetdemo.studio.production

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.studio.registry.DefaultWidgetStudioRegistry
import com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
import com.example.blurwidgetdemo.widgets.WidgetInitialSizeSpec

/** Initializes production mode at the launcher-confirmed creation boundary. */
class WidgetPinResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val widgetId = intent.getStringExtra(EXTRA_STUDIO_WIDGET_ID)
        val providerClassName = intent.getStringExtra(EXTRA_PROVIDER_CLASS_NAME)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID ||
            widgetId == null ||
            DefaultWidgetStudioRegistry.create().descriptor(widgetId) == null
        ) {
            return
        }
        UniversalProductionWidgetRenderer.initializeConfiguredWidget(context, widgetId, appWidgetId)
        val requestedColumns = intent.getIntExtra(WidgetInitialSizeSpec.EXTRA_COLUMNS, 0)
        val requestedRows = intent.getIntExtra(WidgetInitialSizeSpec.EXTRA_ROWS, 0)
        if (requestedColumns > 0 && requestedRows > 0) {
            context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("requested_size_$appWidgetId", "${requestedColumns}x${requestedRows}")
                .apply()
        }
        if (providerClassName?.startsWith(context.packageName) == true) {
            context.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(ComponentName(context.packageName, providerClassName))
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            )
        }
    }

    companion object {
        const val EXTRA_STUDIO_WIDGET_ID = "studio_widget_id"
        const val EXTRA_PROVIDER_CLASS_NAME = "provider_class_name"
    }
}

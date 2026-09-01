package com.example.blurwidgetdemo.widgets.clock

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.clock.ClockStudioDescriptor
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.production.ProductionWidgetDesignRepository
import com.example.blurwidgetdemo.studio.production.ProductionWidgetDesignResolver
import com.example.blurwidgetdemo.studio.production.WidgetProductionDesignModeRepository

class DigitalClockWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) =
        ids.forEach { updateWidget(context, manager, it, "PROVIDER_UPDATE") }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        updateWidget(context, manager, id, "RESIZE")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, DigitalClockWidget::class.java))
                ids.forEach { updateWidget(context, manager, it, intent.action ?: "BROADCAST") }
            }
        }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val preferences = ClockWidgetPreferences.preferences(context)
        val designModes = WidgetProductionDesignModeRepository(context)
        ids.forEach {
            ClockWidgetPreferences.delete(preferences, it)
            designModes.delete(ClockStudioDescriptor.WIDGET_ID, it)
        }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int, reason: String = "UPDATE") {
            val options = manager.getAppWidgetOptions(id)
            val renderSize = ClockWidgetSizeResolver.fromOptions(options)
            val settings = ClockWidgetConfigurationRepository.load(context, id)
            val mode = WidgetProductionDesignModeRepository(context).mode(
                ClockStudioDescriptor.WIDGET_ID,
                id
            )
            val promoted = when (
                val loaded = ProductionWidgetDesignRepository(context)
                    .loadActive(ClockStudioDescriptor.WIDGET_ID)
            ) {
                is StudioRepositoryResult.Success -> loaded.value
                is StudioRepositoryResult.Failure -> null
            }
            // A missing promotion or legacy instance deliberately retains design = null.
            val productionDesign = ProductionWidgetDesignResolver.resolve(
                mode = mode,
                promotedDesign = promoted,
                widgetId = ClockStudioDescriptor.WIDGET_ID,
                widthDp = renderSize.widthDp,
                heightDp = renderSize.heightDp
            )
            val result = ClockRemoteViewsRenderer.build(
                context = context,
                configuration = settings,
                renderSize = renderSize,
                design = productionDesign,
                mode = ClockRenderMode.PRODUCTION
            )
            val debug = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
            val layoutName = runCatching {
                context.resources.getResourceEntryName(result.plan.appliedLayoutResId)
            }.getOrDefault("unknown")
            if (debug) {
                Log.d(
                    "ClockFontDebug",
                    "provider id=$id reason=$reason font=${settings.font.preferenceValue} " +
                        "size=${result.plan.category} layout=${result.plan.appliedLayoutResId}/$layoutName " +
                        "path=ClockRemoteViewsRenderer"
                )
            }
            manager.updateAppWidget(id, result.remoteViews)
            if (debug) Log.d("ClockFontDebug", "updateAppWidget dispatched id=$id layout=$layoutName visibleTime=${R.id.clock_time}")
        }
    }
}

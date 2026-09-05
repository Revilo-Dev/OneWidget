package com.example.blurwidgetdemo.widgets.calendar

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.example.blurwidgetdemo.studio.calendar.CalendarStudioDescriptor
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.production.ProductionWidgetDesignRepository
import com.example.blurwidgetdemo.studio.production.ProductionWidgetDesignResolver
import com.example.blurwidgetdemo.studio.production.WidgetProductionDesignModeRepository

open class CalendarWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) =
        ids.forEach { updateWidget(context, manager, it) }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        options: android.os.Bundle
    ) = updateWidget(context, manager, id)

    override fun onDeleted(context: Context, ids: IntArray) {
        val designModes = WidgetProductionDesignModeRepository(context)
        ids.forEach { designModes.delete(CalendarStudioDescriptor.WIDGET_ID, it) }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val renderSize = CalendarWidgetSizeResolver.fromOptions(manager.getAppWidgetOptions(id))
            val mode = WidgetProductionDesignModeRepository(context).mode(
                CalendarStudioDescriptor.WIDGET_ID,
                id
            )
            val promoted = when (
                val loaded = ProductionWidgetDesignRepository(context)
                    .loadActive(CalendarStudioDescriptor.WIDGET_ID)
            ) {
                is StudioRepositoryResult.Success -> loaded.value
                is StudioRepositoryResult.Failure -> null
            }
            val productionDesign = ProductionWidgetDesignResolver.resolve(
                mode = mode,
                promotedDesign = promoted,
                widgetId = CalendarStudioDescriptor.WIDGET_ID,
                widthDp = renderSize.widthDp,
                heightDp = renderSize.heightDp
            )
            val rendered = CalendarRemoteViewsRenderer.build(
                context = context,
                configuration = CalendarWidgetConfigurationRepository.load(context, id),
                content = CalendarRuntimeDataSource.load(context),
                renderSize = renderSize,
                design = productionDesign,
                mode = CalendarRenderMode.PRODUCTION
            )
            manager.updateAppWidget(id, rendered.remoteViews)
        }

        fun runtimeContent(context: Context): CalendarRuntimeContent =
            CalendarRuntimeDataSource.load(context)
    }
}

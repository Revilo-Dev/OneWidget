package dev.revilodev.onewidget.studio.calendar

import android.content.Context
import android.widget.RemoteViews
import dev.revilodev.onewidget.studio.model.ResolvedWidgetDesign
import dev.revilodev.onewidget.studio.rendering.StudioPreviewRenderResult
import dev.revilodev.onewidget.studio.rendering.StudioRemoteViewsRenderer
import dev.revilodev.onewidget.studio.rendering.WidgetRenderSize
import dev.revilodev.onewidget.widgets.calendar.CalendarRemoteViewsRenderer
import dev.revilodev.onewidget.widgets.calendar.CalendarRenderMode
import dev.revilodev.onewidget.widgets.calendar.CalendarRenderSize
import dev.revilodev.onewidget.widgets.calendar.CalendarSamples
import dev.revilodev.onewidget.widgets.calendar.CalendarWidgetConfigurationRepository

object CalendarStudioRemoteViewsRenderer : StudioRemoteViewsRenderer {
    override fun buildForDeveloperWidget(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        appWidgetId: Int
    ): RemoteViews = build(context, design, renderSize, CalendarRenderMode.DEVELOPER_WIDGET).remoteViews

    override fun buildForStudioPreview(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): StudioPreviewRenderResult {
        val result = build(context, design, renderSize, CalendarRenderMode.STUDIO_ACTUAL_PREVIEW)
        return StudioPreviewRenderResult(
            remoteViews = result.remoteViews,
            diagnostics = buildList {
                add("compact=${result.plan.compact}")
                add("productionLayout=${result.plan.productionLayoutResId}")
                add("appliedLayout=${result.plan.appliedLayoutResId}")
                add("geometry=${result.plan.geometry?.elements?.size ?: 0}")
                result.plan.diagnostics.forEach {
                    add("${it.elementId ?: "renderer"}.${it.property}: ${it.reason}")
                }
            }
        )
    }

    override fun buildForSnapshot(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): RemoteViews = build(context, design, renderSize, CalendarRenderMode.SNAPSHOT).remoteViews

    private fun build(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        mode: CalendarRenderMode
    ) = CalendarRemoteViewsRenderer.build(
        context = context,
        configuration = CalendarWidgetConfigurationRepository.studioSample(),
        content = CalendarSamples.event(),
        renderSize = CalendarRenderSize(renderSize.widthDp, renderSize.heightDp),
        design = design,
        mode = mode
    )
}

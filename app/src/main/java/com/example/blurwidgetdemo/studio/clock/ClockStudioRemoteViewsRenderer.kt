package dev.revilodev.onewidget.studio.clock

import android.content.Context
import android.widget.RemoteViews
import dev.revilodev.onewidget.studio.model.ResolvedWidgetDesign
import dev.revilodev.onewidget.studio.rendering.StudioRemoteViewsRenderer
import dev.revilodev.onewidget.studio.rendering.StudioPreviewRenderResult
import dev.revilodev.onewidget.studio.rendering.WidgetRenderSize
import dev.revilodev.onewidget.widgets.clock.ClockRemoteViewsRenderer
import dev.revilodev.onewidget.widgets.clock.ClockRenderMode
import dev.revilodev.onewidget.widgets.clock.ClockRenderSize
import dev.revilodev.onewidget.widgets.clock.ClockWidgetConfigurationRepository

object ClockStudioRemoteViewsRenderer : StudioRemoteViewsRenderer {
    override fun buildForStudioPreview(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): StudioPreviewRenderResult {
        val result = ClockRemoteViewsRenderer.build(
            context = context,
            configuration = ClockWidgetConfigurationRepository.studioSample(),
            renderSize = ClockRenderSize(renderSize.widthDp, renderSize.heightDp),
            design = design,
            mode = ClockRenderMode.STUDIO_ACTUAL_PREVIEW
        )
        return StudioPreviewRenderResult(
            remoteViews = result.remoteViews,
            diagnostics = buildList {
                add("category=${result.plan.category}")
                add("productionLayout=${result.plan.productionLayoutResId}")
                add("appliedLayout=${result.plan.appliedLayoutResId}")
                add("geometry=${result.plan.geometry?.elements?.size ?: 0}")
                result.plan.diagnostics.forEach { add("${it.elementId ?: "renderer"}.${it.property}: ${it.reason}") }
            }
        )
    }

    override fun buildForDeveloperWidget(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        appWidgetId: Int
    ): RemoteViews = ClockRemoteViewsRenderer.build(
        context = context,
        configuration = ClockWidgetConfigurationRepository.studioSample(),
        renderSize = ClockRenderSize(renderSize.widthDp, renderSize.heightDp),
        design = design,
        mode = ClockRenderMode.DEVELOPER_WIDGET
    ).remoteViews

    override fun buildForSnapshot(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): RemoteViews = ClockRemoteViewsRenderer.build(
        context = context,
        configuration = ClockWidgetConfigurationRepository.studioSample(),
        renderSize = ClockRenderSize(renderSize.widthDp, renderSize.heightDp),
        design = design,
        mode = ClockRenderMode.SNAPSHOT
    ).remoteViews
}

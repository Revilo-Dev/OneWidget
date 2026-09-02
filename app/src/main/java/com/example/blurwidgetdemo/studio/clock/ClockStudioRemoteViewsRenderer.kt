package com.example.blurwidgetdemo.studio.clock

import android.content.Context
import android.widget.RemoteViews
import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign
import com.example.blurwidgetdemo.studio.rendering.StudioRemoteViewsRenderer
import com.example.blurwidgetdemo.studio.rendering.StudioPreviewRenderResult
import com.example.blurwidgetdemo.studio.rendering.WidgetRenderSize
import com.example.blurwidgetdemo.widgets.clock.ClockRemoteViewsRenderer
import com.example.blurwidgetdemo.widgets.clock.ClockRenderMode
import com.example.blurwidgetdemo.widgets.clock.ClockRenderSize
import com.example.blurwidgetdemo.widgets.clock.ClockWidgetConfigurationRepository

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

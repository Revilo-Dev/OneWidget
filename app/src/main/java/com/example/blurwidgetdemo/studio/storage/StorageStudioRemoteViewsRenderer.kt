package com.example.blurwidgetdemo.studio.storage

import android.content.Context
import android.widget.RemoteViews
import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign
import com.example.blurwidgetdemo.studio.rendering.StudioPreviewRenderResult
import com.example.blurwidgetdemo.studio.rendering.StudioRemoteViewsRenderer
import com.example.blurwidgetdemo.studio.rendering.WidgetRenderSize
import com.example.blurwidgetdemo.widgets.storage.StorageRemoteViewsRenderer
import com.example.blurwidgetdemo.widgets.storage.StorageRenderMode
import com.example.blurwidgetdemo.widgets.storage.StorageRenderSize
import com.example.blurwidgetdemo.widgets.storage.StorageSamples
import com.example.blurwidgetdemo.widgets.storage.StorageWidgetConfigurationRepository

object StorageStudioRemoteViewsRenderer : StudioRemoteViewsRenderer {
    override fun buildForDeveloperWidget(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        appWidgetId: Int
    ): RemoteViews = build(context, design, renderSize, StorageRenderMode.DEVELOPER_WIDGET).remoteViews

    override fun buildForStudioPreview(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): StudioPreviewRenderResult {
        val result = build(context, design, renderSize, StorageRenderMode.STUDIO_ACTUAL_PREVIEW)
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
    ): RemoteViews = build(context, design, renderSize, StorageRenderMode.SNAPSHOT).remoteViews

    private fun build(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        mode: StorageRenderMode
    ) = StorageRemoteViewsRenderer.build(
        context = context,
        configuration = StorageWidgetConfigurationRepository.studioSample(),
        runtimeData = StorageSamples.runtime(),
        renderSize = StorageRenderSize(renderSize.widthDp, renderSize.heightDp),
        design = design,
        mode = mode
    )
}

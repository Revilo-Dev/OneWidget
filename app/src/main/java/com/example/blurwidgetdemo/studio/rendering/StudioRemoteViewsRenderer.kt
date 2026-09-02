package com.example.blurwidgetdemo.studio.rendering

import android.content.Context
import android.widget.RemoteViews
import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign

data class WidgetRenderSize(
    val widthDp: Float,
    val heightDp: Float
) {
    init {
        require(widthDp.isFinite() && widthDp > 0f) { "Render width must be positive and finite" }
        require(heightDp.isFinite() && heightDp > 0f) { "Render height must be positive and finite" }
    }
}

data class StudioPreviewRenderResult(
    val remoteViews: RemoteViews,
    val diagnostics: List<String> = emptyList()
)

interface StudioRemoteViewsRenderer {
    val snapshotRendererVersion: Int
        get() = 1

    fun buildForDeveloperWidget(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        appWidgetId: Int
    ): RemoteViews

    fun buildForStudioPreview(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): StudioPreviewRenderResult = StudioPreviewRenderResult(
        buildForDeveloperWidget(context, design, renderSize, appWidgetId = -1)
    )

    /** Same layout/geometry route as the developer widget, with stable snapshot sample data. */
    fun buildForSnapshot(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): RemoteViews = buildForDeveloperWidget(
        context = context,
        design = design,
        renderSize = renderSize,
        appWidgetId = -1
    )
}

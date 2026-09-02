package com.example.blurwidgetdemo.studio.battery

import android.content.Context
import android.widget.RemoteViews
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign
import com.example.blurwidgetdemo.studio.rendering.StudioPreviewRenderResult
import com.example.blurwidgetdemo.studio.rendering.StudioRemoteViewsRenderer
import com.example.blurwidgetdemo.studio.rendering.WidgetRenderSize
import com.example.blurwidgetdemo.widgets.battery.BatteryRemoteViewsRenderer
import com.example.blurwidgetdemo.widgets.battery.BatteryRenderMode
import com.example.blurwidgetdemo.widgets.battery.BatteryRenderSize
import com.example.blurwidgetdemo.widgets.battery.BatterySamples
import com.example.blurwidgetdemo.widgets.battery.BatteryWidgetConfigurationRepository

object BatteryStudioRemoteViewsRenderer : StudioRemoteViewsRenderer {
    override fun buildForDeveloperWidget(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        appWidgetId: Int
    ): RemoteViews = build(context, design, renderSize, BatteryRenderMode.DEVELOPER_WIDGET).remoteViews

    override fun buildForStudioPreview(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): StudioPreviewRenderResult {
        val result = build(context, design, renderSize, BatteryRenderMode.STUDIO_ACTUAL_PREVIEW)
        return StudioPreviewRenderResult(
            remoteViews = result.remoteViews,
            diagnostics = buildList {
                add("compact=${result.plan.compact}")
                add("productionLayout=${result.plan.productionLayoutResId}")
                add("appliedLayout=${result.plan.appliedLayoutResId}")
                add("geometry=${result.plan.geometry?.elements?.size ?: 0}")
                result.plan.diagnostics.forEach { add("${it.elementId ?: "renderer"}.${it.property}: ${it.reason}") }
            }
        )
    }

    override fun buildForSnapshot(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): RemoteViews = build(context, design, renderSize, BatteryRenderMode.SNAPSHOT).remoteViews

    private fun build(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        mode: BatteryRenderMode
    ) = BatteryRemoteViewsRenderer.build(
        context = context,
        configuration = BatteryWidgetConfigurationRepository.studioSample(),
        runtimeData = BatterySamples.runtime(R.drawable.phone),
        renderSize = BatteryRenderSize(renderSize.widthDp, renderSize.heightDp),
        design = design,
        mode = mode
    )
}

package dev.revilodev.onewidget.studio.battery

import android.content.Context
import android.widget.RemoteViews
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.model.ResolvedWidgetDesign
import dev.revilodev.onewidget.studio.rendering.StudioPreviewRenderResult
import dev.revilodev.onewidget.studio.rendering.StudioRemoteViewsRenderer
import dev.revilodev.onewidget.studio.rendering.WidgetRenderSize
import dev.revilodev.onewidget.widgets.battery.BatteryRemoteViewsRenderer
import dev.revilodev.onewidget.widgets.battery.BatteryRenderMode
import dev.revilodev.onewidget.widgets.battery.BatteryRenderSize
import dev.revilodev.onewidget.widgets.battery.BatterySamples
import dev.revilodev.onewidget.widgets.battery.BatteryWidgetConfigurationRepository

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

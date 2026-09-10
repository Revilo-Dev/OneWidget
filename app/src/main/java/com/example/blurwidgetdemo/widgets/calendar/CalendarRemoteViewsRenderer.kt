package dev.revilodev.onewidget.widgets.calendar

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.calendar.CalendarStudioDescriptor
import dev.revilodev.onewidget.studio.model.ResolvedWidgetDesign

data class CalendarRenderPlan(
    val mode: CalendarRenderMode,
    val size: CalendarRenderSize,
    val compact: Boolean,
    val productionLayoutResId: Int,
    val appliedLayoutResId: Int,
    val geometry: CalendarGeometryPlan?,
    val installTapActions: Boolean,
    val diagnostics: List<CalendarRenderDiagnostic>
)

data class CalendarRemoteViewsRenderResult(
    val remoteViews: RemoteViews,
    val plan: CalendarRenderPlan,
    val tapActionsApplied: Boolean
)

object CalendarRemoteViewsRenderer {
    fun plan(
        renderSize: CalendarRenderSize,
        design: ResolvedWidgetDesign? = null,
        mode: CalendarRenderMode = CalendarRenderMode.PRODUCTION,
        density: Float = 1f,
        apiLevel: Int = android.os.Build.VERSION.SDK_INT
    ): CalendarRenderPlan {
        val productionLayout = if (renderSize.compact) {
            R.layout.widget_calendar_compact
        } else {
            R.layout.widget_calendar_large
        }
        val geometry = design?.let { CalendarRemoteViewsGeometry.plan(it, renderSize, density, apiLevel) }
        val diagnostics = buildList {
            if (design != null && design.widgetId != CalendarStudioDescriptor.WIDGET_ID) {
                add(CalendarRenderDiagnostic(null, "widget-id", "Expected calendar, received ${design.widgetId}"))
            }
            geometry?.diagnostics?.let(::addAll)
        }
        return CalendarRenderPlan(
            mode = mode,
            size = renderSize,
            compact = renderSize.compact,
            productionLayoutResId = productionLayout,
            appliedLayoutResId = if (design == null) productionLayout else R.layout.widget_studio_calendar,
            geometry = geometry,
            installTapActions = mode == CalendarRenderMode.PRODUCTION,
            diagnostics = diagnostics
        )
    }

    fun build(
        context: Context,
        configuration: CalendarWidgetConfiguration,
        content: CalendarRuntimeContent,
        renderSize: CalendarRenderSize,
        design: ResolvedWidgetDesign? = null,
        mode: CalendarRenderMode = CalendarRenderMode.PRODUCTION
    ): CalendarRemoteViewsRenderResult {
        val plan = plan(renderSize, design, mode, context.resources.displayMetrics.density)
        val views = RemoteViews(context.packageName, plan.appliedLayoutResId)
        BlurWidget.applySurfaceColor(views, configuration.surfaceColor)
        if (design == null) {
            applyLegacy(views, configuration, content, plan.compact)
        } else {
            require(design.widgetId == CalendarStudioDescriptor.WIDGET_ID) {
                "Calendar renderer received design for ${design.widgetId}"
            }
            applyStudio(views, configuration, content, requireNotNull(plan.geometry), plan.compact)
        }
        if (plan.installTapActions) {
            views.setOnClickPendingIntent(android.R.id.background, calendarIntent(context))
        } else {
            views.setOnClickPendingIntent(android.R.id.background, null)
        }
        return CalendarRemoteViewsRenderResult(
            remoteViews = views,
            plan = plan,
            tapActionsApplied = plan.installTapActions
        )
    }

    private fun applyLegacy(
        views: RemoteViews,
        configuration: CalendarWidgetConfiguration,
        content: CalendarRuntimeContent,
        compact: Boolean
    ) = with(views) {
        setTextViewText(R.id.widget_primary, content.title)
        setTextViewText(R.id.widget_secondary, content.details)
        setViewVisibility(R.id.widget_secondary, View.VISIBLE)
        setTextColor(R.id.widget_primary, configuration.textColor)
        setTextColor(R.id.widget_secondary, if (compact) 0xFFA8A8A8.toInt() else configuration.textColor)
        setInt(R.id.widget_primary, "setGravity", configuration.alignment.gravity)
        setInt(R.id.widget_secondary, "setGravity", configuration.alignment.gravity)
        setFloat(R.id.widget_primary, "setTextSize", (if (compact) 18f else 23f) * configuration.textScalePercent / 100f)
    }

    private fun applyStudio(
        views: RemoteViews,
        configuration: CalendarWidgetConfiguration,
        content: CalendarRuntimeContent,
        geometry: CalendarGeometryPlan,
        compact: Boolean
    ) = with(views) {
        setTextViewText(R.id.calendar_title, content.title)
        setTextViewText(R.id.calendar_details, content.details)
        setTextColor(R.id.calendar_title, configuration.textColor)
        setTextColor(R.id.calendar_details, configuration.textColor)
        setFloat(R.id.calendar_title, "setTextSize", 23f * configuration.textScalePercent / 100f)
        setFloat(R.id.calendar_details, "setTextSize", 14f * configuration.textScalePercent / 100f)
        CalendarRemoteViewsGeometry.apply(views, geometry)
        geometry.elements.forEach { element ->
            val runtimeVisible = element.elementId != CalendarStudioDescriptor.DETAILS || !compact
            setViewVisibility(
                element.binding.containerViewId,
                if (element.visible && runtimeVisible) View.VISIBLE else View.GONE
            )
        }
    }

    private fun calendarIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        CALENDAR_REQUEST_CODE,
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private const val CALENDAR_REQUEST_CODE = 100
}

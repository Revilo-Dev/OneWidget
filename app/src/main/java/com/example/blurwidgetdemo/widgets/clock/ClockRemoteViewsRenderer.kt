package com.example.blurwidgetdemo.widgets.clock

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.clock.ClockStudioDescriptor
import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign

data class ClockRenderPlan(
    val mode: ClockRenderMode,
    val size: ClockRenderSize,
    val category: ClockWidgetLayout.Category,
    /** Existing production layout selected for this size and font. */
    val productionLayoutResId: Int,
    /** Layout actually inflated for this call. */
    val appliedLayoutResId: Int,
    val timePatterns: ClockTimePatterns,
    val geometry: ClockGeometryPlan?,
    val installTapActions: Boolean,
    val tapActionViewIds: Set<Int>,
    val snapshotSampleData: ClockSnapshotSampleData?,
    val diagnostics: List<ClockRenderDiagnostic>
)

data class ClockRemoteViewsRenderResult(
    val remoteViews: RemoteViews,
    val plan: ClockRenderPlan,
    val tapActionsApplied: Boolean
)

object ClockRemoteViewsRenderer {
    fun plan(
        configuration: ClockWidgetConfiguration,
        renderSize: ClockRenderSize,
        design: ResolvedWidgetDesign? = null,
        mode: ClockRenderMode = ClockRenderMode.PRODUCTION,
        density: Float = 1f,
        apiLevel: Int = android.os.Build.VERSION.SDK_INT
    ): ClockRenderPlan {
        val category = renderSize.category
        val productionLayout = ClockLayoutResolver.resolve(category, configuration.font)
        val geometry = design?.let {
            ClockRemoteViewsGeometry.plan(it, renderSize, density, apiLevel)
        }
        val diagnostics = buildList {
            if (design != null && design.widgetId != ClockStudioDescriptor.WIDGET_ID) {
                add(
                    ClockRenderDiagnostic(
                        elementId = null,
                        property = "widget-id",
                        reason = "Expected ${ClockStudioDescriptor.WIDGET_ID}, received ${design.widgetId}"
                    )
                )
            }
            geometry?.diagnostics?.let(::addAll)
        }
        return ClockRenderPlan(
            mode = mode,
            size = renderSize,
            category = category,
            productionLayoutResId = productionLayout,
            appliedLayoutResId = if (design == null) {
                productionLayout
            } else {
                ClockStudioLayoutResolver.resolve(configuration.font)
            },
            timePatterns = ClockTimeFormatResolver.resolve(
                configuration.timeFormat,
                configuration.showAmPm,
                configuration.showSeconds
            ),
            geometry = geometry,
            installTapActions = mode == ClockRenderMode.PRODUCTION,
            tapActionViewIds = if (mode == ClockRenderMode.PRODUCTION) {
                PRODUCTION_TAP_TARGETS
            } else {
                emptySet()
            },
            snapshotSampleData = ClockSnapshotSamples.GALLERY.takeIf {
                mode == ClockRenderMode.SNAPSHOT
            },
            diagnostics = diagnostics
        )
    }

    fun build(
        context: Context,
        configuration: ClockWidgetConfiguration,
        renderSize: ClockRenderSize,
        design: ResolvedWidgetDesign? = null,
        mode: ClockRenderMode = ClockRenderMode.PRODUCTION
    ): ClockRemoteViewsRenderResult {
        val plan = plan(
            configuration = configuration,
            renderSize = renderSize,
            design = design,
            mode = mode,
            density = context.resources.displayMetrics.density
        )
        val views = RemoteViews(context.packageName, plan.appliedLayoutResId)
        // Preserve this exact root surface: One UI Home uses it for native wallpaper blur.
        BlurWidget.applySurfaceColor(
            views,
            if (configuration.backgroundEnabled) configuration.tintColor() else Color.TRANSPARENT
        )
        applySharedContent(context, views, configuration, plan)
        if (design == null) {
            applyProductionLayout(views, configuration, plan.category)
        } else {
            require(design.widgetId == ClockStudioDescriptor.WIDGET_ID) {
                "Digital Clock renderer received design for ${design.widgetId}"
            }
            plan.geometry?.let { ClockRemoteViewsGeometry.apply(views, it) }
            applyStudioVisibility(views, configuration, design)
        }
        if (plan.installTapActions) {
            applyTapActions(context, views, configuration.tapAction, plan.tapActionViewIds)
        }
        return ClockRemoteViewsRenderResult(
            remoteViews = views,
            plan = plan,
            tapActionsApplied = plan.installTapActions
        )
    }

    private fun applySharedContent(
        context: Context,
        views: RemoteViews,
        configuration: ClockWidgetConfiguration,
        plan: ClockRenderPlan
    ) = with(views) {
        val snapshot = plan.snapshotSampleData
        val time12 = snapshot?.time?.let(ClockSnapshotSamples::literalPattern)
            ?: plan.timePatterns.format12Hour
        val time24 = snapshot?.time?.let(ClockSnapshotSamples::literalPattern)
            ?: plan.timePatterns.format24Hour
        setCharSequence(R.id.clock_time, "setFormat12Hour", time12)
        setCharSequence(R.id.clock_time, "setFormat24Hour", time24)
        snapshot?.let { sample ->
            val datePattern = ClockSnapshotSamples.literalPattern(sample.date)
            val dayPattern = ClockSnapshotSamples.literalPattern(sample.day)
            setCharSequence(R.id.clock_date_above, "setFormat12Hour", datePattern)
            setCharSequence(R.id.clock_date_above, "setFormat24Hour", datePattern)
            setCharSequence(R.id.clock_date, "setFormat12Hour", datePattern)
            setCharSequence(R.id.clock_date, "setFormat24Hour", datePattern)
            setCharSequence(R.id.clock_day, "setFormat12Hour", dayPattern)
            setCharSequence(R.id.clock_day, "setFormat24Hour", dayPattern)
        }
        setTextColor(R.id.clock_time, configuration.textColor)
        setTextColor(R.id.clock_date_above, configuration.textColor)
        setTextColor(R.id.clock_date, configuration.textColor)
        setTextColor(R.id.clock_day, configuration.textColor)
        setTextColor(R.id.clock_battery_percent, configuration.textColor)

        if (configuration.showPhoneBattery) {
            val percentage = snapshot?.batteryPercent ?: context.getSystemService(BatteryManager::class.java)
                ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 0..100 } ?: 0
            setTextViewText(R.id.clock_battery_percent, "$percentage%")
        }
        val scale = configuration.textScalePercent / 100f
        setTextViewTextSize(
            R.id.clock_time,
            android.util.TypedValue.COMPLEX_UNIT_SP,
            baseTimeTextSize(plan.category) * scale
        )
        setTextViewTextSize(
            R.id.clock_date_above,
            android.util.TypedValue.COMPLEX_UNIT_SP,
            baseDateTextSize(plan.category) * scale
        )
        setTextViewTextSize(
            R.id.clock_date,
            android.util.TypedValue.COMPLEX_UNIT_SP,
            baseDateTextSize(plan.category) * scale
        )
        setTextViewTextSize(
            R.id.clock_day,
            android.util.TypedValue.COMPLEX_UNIT_SP,
            baseDateTextSize(plan.category) * scale
        )
        setTextViewTextSize(
            R.id.clock_battery_percent,
            android.util.TypedValue.COMPLEX_UNIT_SP,
            14f * scale
        )
        setViewVisibility(R.id.clock_debug_marker, View.GONE)
    }

    private fun applyProductionLayout(
        views: RemoteViews,
        configuration: ClockWidgetConfiguration,
        category: ClockWidgetLayout.Category
    ) = with(views) {
        setViewVisibility(
            R.id.clock_date_above,
            if (configuration.showDate && configuration.dateAboveTime) View.VISIBLE else View.GONE
        )
        setViewVisibility(
            R.id.clock_date,
            if (configuration.showDate && !configuration.dateAboveTime) View.VISIBLE else View.GONE
        )
        setViewVisibility(
            R.id.clock_day,
            if (
                configuration.showDate &&
                configuration.showDayOfWeek &&
                category == ClockWidgetLayout.Category.LARGE
            ) View.VISIBLE else View.GONE
        )
        setViewVisibility(
            R.id.clock_battery,
            if (configuration.showPhoneBattery) View.VISIBLE else View.GONE
        )
        val horizontalGravity = if (
            !configuration.showDate || category == ClockWidgetLayout.Category.COMPACT
        ) {
            Gravity.CENTER_HORIZONTAL
        } else {
            configuration.alignment.gravity()
        }
        setInt(R.id.clock_content, "setGravity", Gravity.CENTER_VERTICAL or horizontalGravity)
        setInt(R.id.clock_time, "setGravity", horizontalGravity)
        setInt(R.id.clock_date_above, "setGravity", horizontalGravity)
        setInt(R.id.clock_date, "setGravity", horizontalGravity)
        setInt(R.id.clock_day, "setGravity", horizontalGravity)
    }

    private fun applyStudioVisibility(
        views: RemoteViews,
        configuration: ClockWidgetConfiguration,
        design: ResolvedWidgetDesign
    ) {
        fun visible(elementId: String): Boolean =
            design.elements.firstOrNull { it.elementId == elementId }?.visible == true

        val timeVisible = visible(ClockStudioDescriptor.TIME)
        val dateVisible = visible(ClockStudioDescriptor.DATE) && configuration.showDate
        val dayVisible = visible(ClockStudioDescriptor.DAY) &&
            configuration.showDate && configuration.showDayOfWeek
        val batteryVisible = visible(ClockStudioDescriptor.BATTERY) &&
            configuration.showPhoneBattery
        views.setViewVisibility(
            R.id.clock_time_wrapper,
            if (timeVisible) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.clock_date_wrapper,
            if (dateVisible) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.clock_date_above,
            if (dateVisible && configuration.dateAboveTime) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.clock_date,
            if (dateVisible && !configuration.dateAboveTime) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.clock_day_wrapper,
            if (dayVisible) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(R.id.clock_day, if (dayVisible) View.VISIBLE else View.GONE)
        views.setViewVisibility(
            R.id.clock_battery_wrapper,
            if (batteryVisible) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.clock_battery,
            if (batteryVisible) View.VISIBLE else View.GONE
        )
    }

    private fun applyTapActions(
        context: Context,
        views: RemoteViews,
        action: ClockTapAction,
        targetViewIds: Set<Int>
    ) {
        val tapIntent = if (action == ClockTapAction.CLOCK) {
            clockPendingIntent(context)
        } else {
            calendarPendingIntent(context)
        }
        targetViewIds.forEach { views.setOnClickPendingIntent(it, tapIntent) }
    }

    private fun ClockTextAlignment.gravity(): Int = when (this) {
        ClockTextAlignment.START -> Gravity.START
        ClockTextAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
        ClockTextAlignment.END -> Gravity.END
    }

    private fun baseTimeTextSize(category: ClockWidgetLayout.Category): Float = when (category) {
        ClockWidgetLayout.Category.COMPACT -> 28f
        ClockWidgetLayout.Category.MEDIUM -> 42f
        ClockWidgetLayout.Category.LARGE -> 52f
    }

    private fun baseDateTextSize(category: ClockWidgetLayout.Category): Float = when (category) {
        ClockWidgetLayout.Category.COMPACT -> 14f
        ClockWidgetLayout.Category.MEDIUM -> 15f
        ClockWidgetLayout.Category.LARGE -> 16f
    }

    private fun clockPendingIntent(context: Context): PendingIntent {
        val samsungClock = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(SAMSUNG_CLOCK_PACKAGE, SAMSUNG_CLOCK_ACTIVITY))
        return pendingActivity(context, samsungClock, Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }

    private fun calendarPendingIntent(context: Context): PendingIntent = pendingActivity(
        context,
        Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time")),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
    )

    private fun pendingActivity(
        context: Context,
        preferred: Intent,
        fallback: Intent
    ): PendingIntent {
        val resolved = if (preferred.resolveActivity(context.packageManager) != null) {
            preferred
        } else {
            fallback
        }
        return PendingIntent.getActivity(
            context,
            0,
            resolved.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val SAMSUNG_CLOCK_PACKAGE = "com.sec.android.app.clockpackage"
    private const val SAMSUNG_CLOCK_ACTIVITY =
        "com.sec.android.app.clockpackage.ClockPackage"

    private val PRODUCTION_TAP_TARGETS = setOf(
        android.R.id.background,
        R.id.clock_content,
        R.id.clock_time,
        R.id.clock_date
    )
}

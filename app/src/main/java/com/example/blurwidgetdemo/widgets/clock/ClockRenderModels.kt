package com.example.blurwidgetdemo.widgets.clock

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle

/** Immutable input shared by the production provider and Widget Studio ACTUAL preview. */
typealias ClockWidgetConfiguration = ClockWidgetSettings

object ClockWidgetConfigurationRepository {
    fun load(context: Context, appWidgetId: Int): ClockWidgetConfiguration =
        ClockWidgetPreferences.load(ClockWidgetPreferences.preferences(context), appWidgetId)

    /** Isolated preview values. No production widget preference is read or written. */
    fun studioSample(): ClockWidgetConfiguration = ClockWidgetSettings(
        showDate = true,
        showDayOfWeek = true,
        showPhoneBattery = true
    )
}

data class ClockRenderSize(
    val widthDp: Float,
    val heightDp: Float
) {
    val category: ClockWidgetLayout.Category
        get() = ClockWidgetLayout.category(widthDp.toInt(), heightDp.toInt())
}

object ClockWidgetSizeResolver {
    const val DEFAULT_MIN_WIDTH_DP = 110
    const val DEFAULT_MIN_HEIGHT_DP = 40

    fun fromOptions(options: Bundle): ClockRenderSize = ClockRenderSize(
        widthDp = options.getInt(
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
            DEFAULT_MIN_WIDTH_DP
        ).toFloat(),
        heightDp = options.getInt(
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
            DEFAULT_MIN_HEIGHT_DP
        ).toFloat()
    )
}

enum class ClockRenderMode {
    PRODUCTION,
    CONFIG_PREVIEW,
    STUDIO_ACTUAL_PREVIEW,
    DEVELOPER_WIDGET,
    SNAPSHOT
}

data class ClockSnapshotSampleData(
    val time: String,
    val date: String,
    val day: String,
    val batteryPercent: Int
)

object ClockSnapshotSamples {
    val GALLERY = ClockSnapshotSampleData(
        time = "10:08",
        date = "24 August",
        day = "Monday",
        batteryPercent = 72
    )

    fun literalPattern(value: String): String = "'${value.replace("'", "''")}'"
}

data class ClockTimePatterns(
    val format12Hour: String,
    val format24Hour: String
)

object ClockTimeFormatResolver {
    fun resolve(
        format: ClockTimeFormat,
        showAmPm: Boolean,
        showSeconds: Boolean
    ): ClockTimePatterns {
        val seconds = if (showSeconds) ":ss" else ""
        return when (format) {
            ClockTimeFormat.SYSTEM -> ClockTimePatterns("h:mm${seconds} a", "HH:mm$seconds")
            ClockTimeFormat.TWELVE_HOUR -> {
                val pattern = if (showAmPm) "h:mm${seconds} a" else "h:mm$seconds"
                ClockTimePatterns(pattern, pattern)
            }
            ClockTimeFormat.TWENTY_FOUR_HOUR -> ClockTimePatterns(
                "HH:mm$seconds",
                "HH:mm$seconds"
            )
        }
    }
}

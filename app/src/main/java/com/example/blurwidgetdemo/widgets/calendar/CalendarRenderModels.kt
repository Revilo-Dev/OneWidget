package dev.revilodev.onewidget.widgets.calendar

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.widgets.WidgetAppearance
import java.text.DateFormat
import java.util.Date

enum class CalendarRenderMode {
    PRODUCTION,
    CONFIG_PREVIEW,
    STUDIO_ACTUAL_PREVIEW,
    DEVELOPER_WIDGET,
    SNAPSHOT
}

data class CalendarRenderSize(val widthDp: Float, val heightDp: Float) {
    val compact: Boolean
        get() = heightDp <= 100f
}

enum class CalendarContentState {
    EVENT,
    EMPTY,
    PERMISSION_REQUIRED,
    UNAVAILABLE,
    ERROR
}

data class CalendarRuntimeContent(
    val state: CalendarContentState,
    val title: String,
    val details: String
)

data class CalendarWidgetConfiguration(
    val surfaceColor: Int,
    val textColor: Int,
    val textScalePercent: Int,
    val alignment: WidgetAppearance.Alignment
)

object CalendarContentResolver {
    fun event(title: String?, startTimeMillis: Long, formatter: (Long) -> String): CalendarRuntimeContent =
        CalendarRuntimeContent(
            state = CalendarContentState.EVENT,
            title = title?.takeUnless { it.isBlank() } ?: "Untitled event",
            details = "Starts ${formatter(startTimeMillis)}"
        )

    fun empty() = CalendarRuntimeContent(
        CalendarContentState.EMPTY,
        "No upcoming events",
        "Tap to open Calendar"
    )

    fun permissionRequired() = CalendarRuntimeContent(
        CalendarContentState.PERMISSION_REQUIRED,
        "Calendar permission needed",
        "Grant access in widget configuration"
    )

    fun unavailable() = CalendarRuntimeContent(
        CalendarContentState.UNAVAILABLE,
        "Calendar unavailable",
        "Tap to open Calendar"
    )

    fun error() = CalendarRuntimeContent(
        CalendarContentState.ERROR,
        "Calendar temporarily unavailable",
        "Tap to try Calendar"
    )
}

object CalendarRuntimeDataSource {
    private const val LOOKAHEAD_MILLIS = 7L * 86_400_000L

    fun load(context: Context): CalendarRuntimeContent {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return CalendarContentResolver.permissionRequired()
        }
        return try {
            val now = System.currentTimeMillis()
            context.contentResolver.query(
                CalendarContract.Instances.CONTENT_URI.buildUpon()
                    .appendPath(now.toString())
                    .appendPath((now + LOOKAHEAD_MILLIS).toString())
                    .build(),
                arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN),
                null,
                null,
                "begin ASC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    CalendarContentResolver.event(cursor.getString(0), cursor.getLong(1)) { start ->
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(start))
                    }
                } else {
                    CalendarContentResolver.empty()
                }
            } ?: CalendarContentResolver.unavailable()
        } catch (_: SecurityException) {
            CalendarContentResolver.permissionRequired()
        } catch (_: Exception) {
            CalendarContentResolver.error()
        }
    }
}

object CalendarSamples {
    fun event() = CalendarRuntimeContent(
        state = CalendarContentState.EVENT,
        title = "Team catch-up",
        details = "Starts 10:00 AM"
    )
}

object CalendarWidgetConfigurationRepository {
    fun load(context: Context, appWidgetId: Int): CalendarWidgetConfiguration {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        return CalendarWidgetConfiguration(
            surfaceColor = BlurWidget.tintColor(prefs, appWidgetId),
            textColor = WidgetAppearance.textColor(prefs, appWidgetId),
            textScalePercent = WidgetAppearance.textScale(prefs, appWidgetId),
            alignment = WidgetAppearance.alignment(prefs, appWidgetId)
        )
    }

    fun studioSample() = CalendarWidgetConfiguration(
        surfaceColor = 0x66FFFFFF,
        textColor = 0xFFFFFFFF.toInt(),
        textScalePercent = 100,
        alignment = WidgetAppearance.Alignment.CENTRE
    )
}

object CalendarWidgetSizeResolver {
    fun fromOptions(options: Bundle): CalendarRenderSize = CalendarRenderSize(
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180).toFloat(),
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80).toFloat()
    )
}

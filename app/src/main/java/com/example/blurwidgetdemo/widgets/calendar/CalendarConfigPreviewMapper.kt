package dev.revilodev.onewidget.widgets.calendar

import dev.revilodev.onewidget.widgets.WidgetAppearance

/** Maps unsaved Calendar controls without writing production preferences. */
object CalendarConfigPreviewMapper {
    fun map(
        base: CalendarWidgetConfiguration,
        surfaceColor: Int,
        textColor: Int,
        textScalePercent: Int,
        alignment: WidgetAppearance.Alignment
    ): CalendarWidgetConfiguration = base.copy(
        surfaceColor = surfaceColor,
        textColor = textColor,
        textScalePercent = textScalePercent.coerceIn(50, 150),
        alignment = alignment
    )
}

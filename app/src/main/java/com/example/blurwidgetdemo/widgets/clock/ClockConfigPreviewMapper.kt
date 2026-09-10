package dev.revilodev.onewidget.widgets.clock

/** Maps unsaved configuration controls to renderer input without touching widget preferences. */
object ClockConfigPreviewMapper {
    fun map(
        settings: ClockWidgetConfiguration,
        hue: Float,
        saturation: Float,
        value: Float,
        alpha: Int,
        textScalePercent: Int,
        textColor: Int
    ): ClockWidgetConfiguration = settings.copy(
        hue = hue,
        saturation = saturation,
        value = value,
        alpha = alpha.coerceIn(1, 254),
        textScalePercent = textScalePercent.coerceIn(50, 150),
        textColor = textColor
    )
}

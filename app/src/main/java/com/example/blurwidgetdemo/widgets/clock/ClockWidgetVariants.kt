package dev.revilodev.onewidget.widgets.clock

import android.appwidget.AppWidgetProvider

/**
 * Android's pin API cannot select a size for a single provider. Each gallery clock size
 * therefore has a small provider alias with its own target-cell metadata.
 */
class DigitalClock2x1Widget : DigitalClockWidget()
class DigitalClock3x2Widget : DigitalClockWidget()
class DigitalClock4x2Widget : DigitalClockWidget()
class DigitalClock4x4Widget : DigitalClockWidget()

object ClockWidgetVariants {
    val providerClasses: List<Class<out AppWidgetProvider>> = listOf(
        DigitalClockWidget::class.java.name,
        DigitalClock2x1Widget::class.java.name,
        DigitalClock3x2Widget::class.java.name,
        DigitalClock4x2Widget::class.java.name,
        DigitalClock4x4Widget::class.java.name
    ).map { Class.forName(it).asSubclass(AppWidgetProvider::class.java) }

    private val providerNames = providerClasses.map { it.name }.toSet()

    fun isClockProvider(className: String?): Boolean = className in providerNames

    fun providerFor(columns: Int, rows: Int): Class<out AppWidgetProvider> = when (columns to rows) {
        2 to 1 -> DigitalClock2x1Widget::class.java
        3 to 2 -> DigitalClock3x2Widget::class.java
        4 to 2 -> DigitalClock4x2Widget::class.java
        4 to 4 -> DigitalClock4x4Widget::class.java
        else -> DigitalClockWidget::class.java
    }
}

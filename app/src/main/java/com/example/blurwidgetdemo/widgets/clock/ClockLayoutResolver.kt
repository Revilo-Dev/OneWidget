package dev.revilodev.onewidget.widgets.clock

import dev.revilodev.onewidget.R

/**
 * RemoteViews cannot receive a Typeface object. Each result is therefore a complete XML
 * layout with android:fontFamily already set while the launcher inflates the widget.
 */
object ClockLayoutResolver {
    fun resolve(sizeCategory: ClockWidgetLayout.Category, fontChoice: ClockFont): Int = layouts(
        R.layout.widget_digital_clock_compact,
        R.layout.widget_digital_clock_medium,
        R.layout.widget_digital_clock_large,
        sizeCategory
    )

    private fun layouts(compact: Int, medium: Int, large: Int, category: ClockWidgetLayout.Category): Int = when (category) {
        ClockWidgetLayout.Category.COMPACT -> compact
        ClockWidgetLayout.Category.MEDIUM -> medium
        ClockWidgetLayout.Category.LARGE -> large
    }
}

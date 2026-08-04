package com.example.blurwidgetdemo.widgets.clock

import com.example.blurwidgetdemo.R

/**
 * RemoteViews cannot receive a Typeface object. Each result is therefore a complete XML
 * layout with android:fontFamily already set while the launcher inflates the widget.
 */
object ClockLayoutResolver {
    fun resolve(sizeCategory: ClockWidgetLayout.Category, fontChoice: ClockFont): Int = when (fontChoice) {
        ClockFont.SYSTEM -> layouts(R.layout.widget_digital_clock_compact, R.layout.widget_digital_clock_medium, R.layout.widget_digital_clock_large, sizeCategory)
        ClockFont.SAMSUNG_DEFAULT -> layouts(R.layout.widget_digital_clock_compact_samsung_default, R.layout.widget_digital_clock_medium_samsung_default, R.layout.widget_digital_clock_large_samsung_default, sizeCategory)
        ClockFont.SAMSUNG_DEFAULT_BOLD -> layouts(R.layout.widget_digital_clock_compact_samsung_default_bold, R.layout.widget_digital_clock_medium_samsung_default_bold, R.layout.widget_digital_clock_large_samsung_default_bold, sizeCategory)
        ClockFont.SAMSUNG_DEFAULT_THIN -> layouts(R.layout.widget_digital_clock_compact_samsung_default_thin, R.layout.widget_digital_clock_medium_samsung_default_thin, R.layout.widget_digital_clock_large_samsung_default_thin, sizeCategory)
        ClockFont.SAMSUNG_MONO -> layouts(R.layout.widget_digital_clock_compact_samsung_mono, R.layout.widget_digital_clock_medium_samsung_mono, R.layout.widget_digital_clock_large_samsung_mono, sizeCategory)
        ClockFont.CLOCK_BOLD_SERIF -> layouts(R.layout.widget_digital_clock_compact_clock_bold_serif, R.layout.widget_digital_clock_medium_clock_bold_serif, R.layout.widget_digital_clock_large_clock_bold_serif, sizeCategory)
        ClockFont.CLOCK_STRIPE -> layouts(R.layout.widget_digital_clock_compact_clock_stripe, R.layout.widget_digital_clock_medium_clock_stripe, R.layout.widget_digital_clock_large_clock_stripe, sizeCategory)
        ClockFont.CLOCK_STAMP -> layouts(R.layout.widget_digital_clock_compact_clock_stamp, R.layout.widget_digital_clock_medium_clock_stamp, R.layout.widget_digital_clock_large_clock_stamp, sizeCategory)
        ClockFont.ALATSI -> layouts(R.layout.widget_digital_clock_compact_alatsi, R.layout.widget_digital_clock_medium_alatsi, R.layout.widget_digital_clock_large_alatsi, sizeCategory)
        ClockFont.CAPRIOLA -> layouts(R.layout.widget_digital_clock_compact_capriola, R.layout.widget_digital_clock_medium_capriola, R.layout.widget_digital_clock_large_capriola, sizeCategory)
        ClockFont.FREDERICKA -> layouts(R.layout.widget_digital_clock_compact_fredericka, R.layout.widget_digital_clock_medium_fredericka, R.layout.widget_digital_clock_large_fredericka, sizeCategory)
        ClockFont.LATO -> layouts(R.layout.widget_digital_clock_compact_lato, R.layout.widget_digital_clock_medium_lato, R.layout.widget_digital_clock_large_lato, sizeCategory)
        ClockFont.STARDOS_STENCIL -> layouts(R.layout.widget_digital_clock_compact_stardos_stencil, R.layout.widget_digital_clock_medium_stardos_stencil, R.layout.widget_digital_clock_large_stardos_stencil, sizeCategory)
        ClockFont.MODAK -> layouts(R.layout.widget_digital_clock_compact_modak, R.layout.widget_digital_clock_medium_modak, R.layout.widget_digital_clock_large_modak, sizeCategory)
    }

    private fun layouts(compact: Int, medium: Int, large: Int, category: ClockWidgetLayout.Category): Int = when (category) {
        ClockWidgetLayout.Category.COMPACT -> compact
        ClockWidgetLayout.Category.MEDIUM -> medium
        ClockWidgetLayout.Category.LARGE -> large
    }
}

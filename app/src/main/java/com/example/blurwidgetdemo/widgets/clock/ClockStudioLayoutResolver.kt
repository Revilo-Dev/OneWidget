package dev.revilodev.onewidget.widgets.clock

import dev.revilodev.onewidget.R

/** Font-specific, full-canvas layouts used only when a Studio design is supplied. */
object ClockStudioLayoutResolver {
    fun resolve(font: ClockFont): Int = when (font) {
        ClockFont.SYSTEM -> R.layout.widget_studio_clock_system
        ClockFont.SAMSUNG_DEFAULT -> R.layout.widget_studio_clock_samsung_default
        ClockFont.SAMSUNG_DEFAULT_BOLD -> R.layout.widget_studio_clock_samsung_default_bold
        ClockFont.SAMSUNG_DEFAULT_THIN -> R.layout.widget_studio_clock_samsung_default_thin
        ClockFont.SAMSUNG_MONO -> R.layout.widget_studio_clock_samsung_mono
        ClockFont.CLOCK_BOLD_SERIF -> R.layout.widget_studio_clock_clock_bold_serif
        ClockFont.CLOCK_STRIPE -> R.layout.widget_studio_clock_clock_stripe
        ClockFont.CLOCK_STAMP -> R.layout.widget_studio_clock_clock_stamp
        ClockFont.ALATSI -> R.layout.widget_studio_clock_alatsi
        ClockFont.CAPRIOLA -> R.layout.widget_studio_clock_capriola
        ClockFont.FREDERICKA -> R.layout.widget_studio_clock_fredericka
        ClockFont.LATO -> R.layout.widget_studio_clock_lato
        ClockFont.STARDOS_STENCIL -> R.layout.widget_studio_clock_stardos_stencil
        ClockFont.MODAK -> R.layout.widget_studio_clock_modak
    }
}

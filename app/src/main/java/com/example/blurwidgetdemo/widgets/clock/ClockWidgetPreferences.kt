package com.example.blurwidgetdemo.widgets.clock

import android.content.SharedPreferences
import com.example.blurwidgetdemo.BlurWidget

enum class ClockTimeFormat { SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR }
enum class ClockTextAlignment { START, CENTER, END }
enum class ClockTapAction { CLOCK, CALENDAR }
enum class ClockFont { SYSTEM, SAMSUNG_DEFAULT, SAMSUNG_DEFAULT_BOLD, SAMSUNG_DEFAULT_THIN, SAMSUNG_MONO, CLOCK_BOLD_SERIF, CLOCK_STRIPE, CLOCK_STAMP, ALATSI, CAPRIOLA, FREDERICKA, LATO, STARDOS_STENCIL, MODAK }

data class ClockWidgetSettings(
    val hue: Float = BlurWidget.DEFAULT_TINT_HUE,
    val saturation: Float = BlurWidget.DEFAULT_TINT_SATURATION,
    val value: Float = BlurWidget.DEFAULT_TINT_VALUE,
    val alpha: Int = BlurWidget.DEFAULT_TINT_ALPHA,
    val showDate: Boolean = true,
    val showDayOfWeek: Boolean = true,
    val dateAboveTime: Boolean = false,
    val backgroundEnabled: Boolean = true,
    val timeFormat: ClockTimeFormat = ClockTimeFormat.SYSTEM,
    val alignment: ClockTextAlignment = ClockTextAlignment.CENTER,
    val tapAction: ClockTapAction = ClockTapAction.CLOCK,
    val font: ClockFont = ClockFont.SYSTEM
) {
    fun tintColor(): Int = BlurWidget.tintColor(hue, saturation, value, alpha)
}

object ClockWidgetPreferences {
    private const val NAME = "digital_clock_widget_prefs"
    private fun key(name: String, id: Int) = "${name}_$id"

    fun load(preferences: SharedPreferences, id: Int): ClockWidgetSettings = ClockWidgetSettings(
        hue = preferences.getFloat(key("hue", id), BlurWidget.DEFAULT_TINT_HUE),
        saturation = preferences.getFloat(key("saturation", id), BlurWidget.DEFAULT_TINT_SATURATION),
        value = preferences.getFloat(key("value", id), BlurWidget.DEFAULT_TINT_VALUE),
        alpha = preferences.getInt(key("alpha", id), BlurWidget.DEFAULT_TINT_ALPHA).coerceIn(1, 254),
        showDate = preferences.getBoolean(key("show_date", id), true),
        showDayOfWeek = preferences.getBoolean(key("show_day", id), true),
        dateAboveTime = preferences.getBoolean(key("date_above", id), false),
        backgroundEnabled = preferences.getBoolean(key("background_enabled", id), true),
        timeFormat = preferences.enumValue(key("time_format", id), ClockTimeFormat.SYSTEM),
        alignment = preferences.enumValue(key("alignment", id), ClockTextAlignment.CENTER),
        tapAction = preferences.enumValue(key("tap_action", id), ClockTapAction.CLOCK),
        font = preferences.enumValue(key("font", id), ClockFont.SYSTEM)
    )

    fun save(preferences: SharedPreferences, id: Int, settings: ClockWidgetSettings) {
        preferences.edit()
            .putFloat(key("hue", id), settings.hue).putFloat(key("saturation", id), settings.saturation)
            .putFloat(key("value", id), settings.value).putInt(key("alpha", id), settings.alpha.coerceIn(1, 254))
            .putBoolean(key("show_date", id), settings.showDate).putBoolean(key("show_day", id), settings.showDayOfWeek)
            .putBoolean(key("date_above", id), settings.dateAboveTime).putBoolean(key("background_enabled", id), settings.backgroundEnabled)
            .putString(key("time_format", id), settings.timeFormat.name).putString(key("alignment", id), settings.alignment.name)
            .putString(key("tap_action", id), settings.tapAction.name).putString(key("font", id), settings.font.name)
            .apply()
    }

    fun delete(preferences: SharedPreferences, id: Int) {
        preferences.edit().apply {
            listOf("hue", "saturation", "value", "alpha", "show_date", "show_day", "date_above", "background_enabled", "time_format", "alignment", "tap_action", "font")
                .forEach { remove(key(it, id)) }
        }.apply()
    }

    fun preferences(context: android.content.Context) = context.getSharedPreferences(NAME, android.content.Context.MODE_PRIVATE)

    private inline fun <reified T : Enum<T>> SharedPreferences.enumValue(key: String, default: T): T =
        getString(key, default.name)?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default
}

package com.example.blurwidgetdemo.widgets.clock

import android.content.SharedPreferences
import android.graphics.Color
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.BlurWidget

enum class ClockTimeFormat { SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR }
enum class ClockTextAlignment { START, CENTER, END }
enum class ClockTapAction { CLOCK, CALENDAR }
enum class ClockFont(val preferenceValue: String) {
    SYSTEM("system"), SAMSUNG_DEFAULT("samsung_default"), SAMSUNG_DEFAULT_BOLD("samsung_default_bold"),
    SAMSUNG_DEFAULT_THIN("samsung_default_thin"), SAMSUNG_MONO("samsung_mono"), CLOCK_BOLD_SERIF("clock_bold_serif"),
    CLOCK_STRIPE("clock_stripe"), CLOCK_STAMP("clock_stamp"), ALATSI("alatsi"), CAPRIOLA("capriola"),
    FREDERICKA("fredericka"), LATO("lato"), STARDOS_STENCIL("stardos_stencil"), MODAK("modak");

    companion object {
        fun fromPreference(value: String?): ClockFont = entries.firstOrNull { it.preferenceValue == value }
            ?: entries.firstOrNull { it.name == value } // Migrate existing enum-name preferences.
            ?: SYSTEM
    }
}

/** Font metadata shared by the widget layouts and the live configuration preview. */
fun ClockFont.fontResource(): Int? = when (this) {
    ClockFont.SYSTEM -> null
    ClockFont.SAMSUNG_DEFAULT, ClockFont.SAMSUNG_DEFAULT_BOLD, ClockFont.SAMSUNG_DEFAULT_THIN -> R.font.samsung_number_default_vf
    ClockFont.SAMSUNG_MONO -> R.font.samsung_number_mono_vf
    ClockFont.CLOCK_BOLD_SERIF -> R.font.clock_bold_serif
    ClockFont.CLOCK_STRIPE -> R.font.clock_stripe
    ClockFont.CLOCK_STAMP -> R.font.clock_stamp
    ClockFont.ALATSI -> R.font.alatsi
    ClockFont.CAPRIOLA -> R.font.capriola
    ClockFont.FREDERICKA -> R.font.fredericka_the_great
    ClockFont.LATO -> R.font.lato
    ClockFont.STARDOS_STENCIL -> R.font.stardos_stencil
    ClockFont.MODAK -> R.font.modak_regular
}

fun ClockFont.variationSettings(): String? = when (this) {
    ClockFont.SAMSUNG_DEFAULT_BOLD -> "'wght' 700"
    ClockFont.SAMSUNG_DEFAULT_THIN -> "'wght' 300"
    else -> null
}

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
    val showAmPm: Boolean = true,
    val showSeconds: Boolean = false,
    val alignment: ClockTextAlignment = ClockTextAlignment.CENTER,
    val tapAction: ClockTapAction = ClockTapAction.CLOCK,
    val font: ClockFont = ClockFont.SYSTEM,
    val textColor: Int = Color.WHITE
) {
    fun tintColor(): Int = BlurWidget.tintColor(hue, saturation, value, alpha)
}

object ClockWidgetPreferences {
    private const val NAME = BlurWidget.WIDGET_PREFS
    private fun key(name: String, id: Int) = "${name}_$id"

    fun load(preferences: SharedPreferences, id: Int): ClockWidgetSettings = ClockWidgetSettings(
        hue = preferences.getFloat(key("tint_hue", id), BlurWidget.DEFAULT_TINT_HUE),
        saturation = preferences.getFloat(key("tint_saturation", id), BlurWidget.DEFAULT_TINT_SATURATION),
        value = preferences.getFloat(key("tint_value", id), BlurWidget.DEFAULT_TINT_VALUE),
        alpha = preferences.getInt(key("tint_alpha", id), BlurWidget.DEFAULT_TINT_ALPHA).coerceIn(1, 254),
        showDate = preferences.getBoolean(key("show_date", id), true),
        showDayOfWeek = preferences.getBoolean(key("show_day", id), true),
        dateAboveTime = preferences.getBoolean(key("date_above", id), false),
        backgroundEnabled = preferences.getBoolean(key("background_enabled", id), true),
        timeFormat = preferences.enumValue(key("time_format", id), ClockTimeFormat.SYSTEM),
        showAmPm = preferences.getBoolean(key("show_am_pm", id), true),
        showSeconds = preferences.getBoolean(key("show_seconds", id), false),
        alignment = preferences.enumValue(key("alignment", id), ClockTextAlignment.CENTER),
        tapAction = preferences.enumValue(key("tap_action", id), ClockTapAction.CLOCK),
        font = ClockFont.fromPreference(preferences.getString(key("font", id), null)),
        textColor = preferences.getInt(key("text_color", id), Color.WHITE)
    )

    fun save(preferences: SharedPreferences, id: Int, settings: ClockWidgetSettings) {
        preferences.edit()
            .putFloat(key("tint_hue", id), settings.hue).putFloat(key("tint_saturation", id), settings.saturation)
            .putFloat(key("tint_value", id), settings.value).putInt(key("tint_alpha", id), settings.alpha.coerceIn(1, 254))
            .putBoolean(key("show_date", id), settings.showDate).putBoolean(key("show_day", id), settings.showDayOfWeek)
            .putBoolean(key("date_above", id), settings.dateAboveTime).putBoolean(key("background_enabled", id), settings.backgroundEnabled)
            .putString(key("time_format", id), settings.timeFormat.name).putBoolean(key("show_am_pm", id), settings.showAmPm).putBoolean(key("show_seconds", id), settings.showSeconds).putString(key("alignment", id), settings.alignment.name)
            .putString(key("tap_action", id), settings.tapAction.name).putString(key("font", id), settings.font.preferenceValue).putInt(key("text_color", id), settings.textColor)
            .apply()
    }

    fun delete(preferences: SharedPreferences, id: Int) {
        preferences.edit().apply {
            listOf("tint_hue", "tint_saturation", "tint_value", "tint_alpha", "show_date", "show_day", "date_above", "background_enabled", "time_format", "show_am_pm", "show_seconds", "alignment", "tap_action", "font", "text_color")
                .forEach { remove(key(it, id)) }
        }.apply()
    }

    fun preferences(context: android.content.Context) = context.getSharedPreferences(NAME, android.content.Context.MODE_PRIVATE)

    private inline fun <reified T : Enum<T>> SharedPreferences.enumValue(key: String, default: T): T =
        getString(key, default.name)?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default
}

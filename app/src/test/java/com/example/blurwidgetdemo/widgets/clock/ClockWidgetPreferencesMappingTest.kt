package com.example.blurwidgetdemo.widgets.clock

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockWidgetPreferencesMappingTest {
    @Test fun `empty preferences preserve all defaults`() {
        val settings = ClockWidgetPreferences.load(MapPreferences(emptyMap()), 1)
        // Preserve the existing loader's null-font migration behaviour exactly.
        val defaults = ClockWidgetSettings(font = ClockFont.SYSTEM)
        assertEquals(defaults, settings)
    }

    @Test fun `all existing per-widget preferences map exactly`() {
        val id = 42
        val settings = ClockWidgetPreferences.load(
            MapPreferences(
                mapOf(
                    "tint_hue_$id" to 210f,
                    "tint_saturation_$id" to 0.7f,
                    "tint_value_$id" to 0.4f,
                    "tint_alpha_$id" to 177,
                    "show_date_$id" to false,
                    "show_day_$id" to false,
                    "date_above_$id" to true,
                    "background_enabled_$id" to false,
                    "time_format_$id" to ClockTimeFormat.TWELVE_HOUR.name,
                    "show_am_pm_$id" to false,
                    "show_seconds_$id" to true,
                    "alignment_$id" to ClockTextAlignment.END.name,
                    "tap_action_$id" to ClockTapAction.CALENDAR.name,
                    "font_$id" to ClockFont.LATO.preferenceValue,
                    "text_color_$id" to 0xFF123456.toInt(),
                    "text_scale_$id" to 135,
                    "show_phone_battery_$id" to true
                )
            ),
            id
        )
        assertEquals(210f, settings.hue)
        assertEquals(0.7f, settings.saturation)
        assertEquals(0.4f, settings.value)
        assertEquals(177, settings.alpha)
        assertFalse(settings.showDate)
        assertFalse(settings.showDayOfWeek)
        assertTrue(settings.dateAboveTime)
        assertFalse(settings.backgroundEnabled)
        assertEquals(ClockTimeFormat.TWELVE_HOUR, settings.timeFormat)
        assertFalse(settings.showAmPm)
        assertTrue(settings.showSeconds)
        assertEquals(ClockTextAlignment.END, settings.alignment)
        assertEquals(ClockTapAction.CALENDAR, settings.tapAction)
        assertEquals(ClockFont.LATO, settings.font)
        assertEquals(0xFF123456.toInt(), settings.textColor)
        assertEquals(135, settings.textScalePercent)
        assertTrue(settings.showPhoneBattery)
    }

    @Test fun `unknown enums and out of range values fall back safely`() {
        val id = 7
        val settings = ClockWidgetPreferences.load(
            MapPreferences(
                mapOf(
                    "tint_alpha_$id" to 999,
                    "time_format_$id" to "REMOVED_TIME_FORMAT",
                    "alignment_$id" to "REMOVED_ALIGNMENT",
                    "tap_action_$id" to "REMOVED_ACTION",
                    "font_$id" to "removed_font",
                    "text_scale_$id" to -20
                )
            ),
            id
        )
        assertEquals(254, settings.alpha)
        assertEquals(ClockTimeFormat.SYSTEM, settings.timeFormat)
        assertEquals(ClockTextAlignment.CENTER, settings.alignment)
        assertEquals(ClockTapAction.CLOCK, settings.tapAction)
        assertEquals(ClockFont.SYSTEM, settings.font)
        assertEquals(50, settings.textScalePercent)
    }

    private class MapPreferences(
        private val values: Map<String, Any?>
    ) : SharedPreferences {
        override fun getAll(): Map<String, *> = values
        override fun getString(key: String?, defValue: String?): String? =
            values[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            values[key] as? MutableSet<String> ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = throw UnsupportedOperationException()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
    }
}

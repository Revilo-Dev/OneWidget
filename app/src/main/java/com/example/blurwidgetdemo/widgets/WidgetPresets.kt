package dev.revilodev.onewidget.widgets

import android.content.SharedPreferences
import dev.revilodev.onewidget.BlurWidget
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class WidgetPreset(
    val id: String,
    val name: String,
    val hue: Float = 0f,
    val saturation: Float = 0f,
    val value: Float = 1f,
    val alpha: Int = 102,
    val border: WidgetBorderStyle = WidgetBorderStyle()
) {
    fun color() = BlurWidget.tintColor(hue, saturation, value, alpha)
}

object WidgetPresets {
    private const val KEY = "appearance_presets_v1"

    fun load(prefs: SharedPreferences): List<WidgetPreset> = runCatching {
        val json = prefs.getString(KEY, null) ?: return defaults()
        val array = JSONArray(json)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            WidgetPreset(
                id = item.getString("id"),
                name = item.getString("name").trim().take(40).ifEmpty { "Preset" },
                hue = item.optDouble("hue", 0.0).toFloat().coerceIn(0f, 360f),
                saturation = item.optDouble("saturation", 0.0).toFloat().coerceIn(0f, 1f),
                value = item.optDouble("value", 1.0).toFloat().coerceIn(0f, 1f),
                alpha = item.optInt("alpha", 102).coerceIn(1, 254),
                border = WidgetBorderStyle(item.optBoolean("border", true),
                    item.optInt("thickness", 4), item.optInt("opacity", 20)).normalized()
            )
        }.distinctBy { it.id }.take(24)
    }.getOrElse { defaults() }

    fun save(prefs: SharedPreferences, presets: List<WidgetPreset>) {
        val array = JSONArray()
        presets.take(24).forEach { preset ->
            val border = preset.border.normalized()
            array.put(JSONObject().put("id", preset.id).put("name", preset.name.trim().take(40).ifEmpty { "Preset" })
                .put("hue", preset.hue).put("saturation", preset.saturation).put("value", preset.value)
                .put("alpha", preset.alpha.coerceIn(1, 254)).put("border", border.enabled)
                .put("thickness", border.thicknessPx).put("opacity", border.opacityPercent))
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun selected(prefs: SharedPreferences, id: Int): WidgetPreset? {
        val selected = prefs.getString("appearance_preset_" + id, null) ?: return null
        return load(prefs).firstOrNull { it.id == selected }
    }

    fun newPreset() = WidgetPreset(UUID.randomUUID().toString(), "New preset")

    private fun defaults() = listOf(
        WidgetPreset("glass", "Glass", alpha = 38),
        WidgetPreset("light", "Light", alpha = 102),
        WidgetPreset("medium", "Medium", alpha = 178),
        WidgetPreset("solid", "Solid", alpha = 240)
    )
}

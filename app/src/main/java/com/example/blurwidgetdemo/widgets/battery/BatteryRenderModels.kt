package com.example.blurwidgetdemo.widgets.battery

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.widgets.WidgetAppearance

enum class BatteryRenderMode {
    PRODUCTION,
    CONFIG_PREVIEW,
    STUDIO_ACTUAL_PREVIEW,
    DEVELOPER_WIDGET,
    SNAPSHOT
}

data class BatteryRenderSize(val widthDp: Float, val heightDp: Float) {
    val compact: Boolean
        get() = heightDp <= 100f || widthDp < 250f
}

data class BatteryStateColors(
    val low: Int,
    val powerSaving: Int,
    val normal: Int,
    val charging: Int,
    val superFastCharging: Int = normal
)

data class BatteryWidgetConfiguration(
    val surfaceColor: Int,
    val textColor: Int,
    val textScalePercent: Int,
    val alignment: WidgetAppearance.Alignment,
    val gradientsEnabled: Boolean,
    val compactDeviceKey: String,
    val stateColors: BatteryStateColors
)

data class BatteryDeviceRuntimeData(
    val key: String,
    val label: String,
    val percentage: Int?,
    val iconResId: Int,
    val charging: Boolean = false,
    val statusText: String = if (charging) "Charging" else "Battery"
)

data class BatteryRuntimeData(
    val devices: List<BatteryDeviceRuntimeData>,
    val powerSaving: Boolean = false
)

object BatterySamples {
    fun runtime(phoneIconResId: Int) = BatteryRuntimeData(
        devices = listOf(
            BatteryDeviceRuntimeData(
                key = BatteryWidget.PHONE,
                label = "Phone",
                percentage = 72,
                iconResId = phoneIconResId,
                charging = true,
                statusText = "Charging"
            )
        )
    )
}

object BatteryWidgetConfigurationRepository {
    fun load(context: Context, appWidgetId: Int): BatteryWidgetConfiguration {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        return BatteryWidgetConfiguration(
            surfaceColor = BlurWidget.tintColor(prefs, appWidgetId),
            textColor = WidgetAppearance.textColor(prefs, appWidgetId),
            textScalePercent = WidgetAppearance.textScale(prefs, appWidgetId),
            alignment = WidgetAppearance.alignment(prefs, appWidgetId),
            gradientsEnabled = WidgetAppearance.gradientsEnabled(prefs),
            compactDeviceKey = prefs.getString("battery_compact_device_$appWidgetId", BatteryWidget.PHONE)
                ?: BatteryWidget.PHONE,
            stateColors = BatteryStateColors(
                low = BatteryWidget.colour(context, appWidgetId, "low"),
                powerSaving = BatteryWidget.colour(context, appWidgetId, "power_saving"),
                normal = BatteryWidget.colour(context, appWidgetId, "normal"),
                charging = BatteryWidget.colour(context, appWidgetId, "charging"),
                superFastCharging = BatteryWidget.colour(context, appWidgetId, "super_fast_charging")
            )
        )
    }

    fun studioSample(): BatteryWidgetConfiguration = BatteryWidgetConfiguration(
        surfaceColor = 0x66FFFFFF,
        textColor = 0xFFFFFFFF.toInt(),
        textScalePercent = 100,
        alignment = WidgetAppearance.Alignment.CENTRE,
        gradientsEnabled = true,
        compactDeviceKey = BatteryWidget.PHONE,
        stateColors = BatteryStateColors(
            low = 0xFFF44336.toInt(),
            powerSaving = 0xFFFF9800.toInt(),
            normal = 0xFF4CAF50.toInt(),
            charging = 0xFF009688.toInt()
        )
    )
}

object BatteryWidgetSizeResolver {
    fun fromOptions(options: Bundle): BatteryRenderSize = BatteryRenderSize(
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180).toFloat(),
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80).toFloat()
    )
}

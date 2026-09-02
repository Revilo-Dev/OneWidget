package com.example.blurwidgetdemo.widgets.battery

import com.example.blurwidgetdemo.widgets.WidgetAppearance

/** Maps unsaved Battery configuration controls without writing production preferences. */
object BatteryConfigPreviewMapper {
    fun map(
        base: BatteryWidgetConfiguration,
        surfaceColor: Int,
        textColor: Int,
        textScalePercent: Int,
        alignment: WidgetAppearance.Alignment,
        compactDeviceKey: String,
        colors: Map<String, Int>
    ): BatteryWidgetConfiguration = base.copy(
        surfaceColor = surfaceColor,
        textColor = textColor,
        textScalePercent = textScalePercent.coerceIn(50, 150),
        alignment = alignment,
        compactDeviceKey = compactDeviceKey,
        stateColors = base.stateColors.copy(
            low = colors["low"] ?: base.stateColors.low,
            powerSaving = colors["power_saving"] ?: base.stateColors.powerSaving,
            normal = colors["normal"] ?: base.stateColors.normal,
            charging = colors["charging"] ?: base.stateColors.charging
        )
    )
}

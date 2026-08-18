package com.example.blurwidgetdemo.widgets.battery

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetAppearance

class BatteryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { updateWidget(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateWidget(context, manager, id)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_BATTERY_CHANGED || intent.action == Intent.ACTION_POWER_CONNECTED || intent.action == Intent.ACTION_POWER_DISCONNECTED || intent.action == android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED || intent.action == android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED) {
            val manager = AppWidgetManager.getInstance(context)
            onUpdate(context, manager, manager.getAppWidgetIds(ComponentName(context, BatteryWidget::class.java)))
        }
    }

    companion object {
        data class Device(val key: String, val label: String, val level: Int?, val icon: Int, val charging: Boolean = false)
        private const val PHONE = "phone"
        private const val RED = 0xFFF44336.toInt()
        private const val ORANGE = 0xFFFF9800.toInt()
        private const val GREEN = 0xFF4CAF50.toInt()
        private const val TEAL = 0xFF009688.toInt()

        fun availableDevices(context: Context): List<Device> = listOf(phoneDevice(context)) + bluetoothDevices(context)
        fun colour(context: Context, id: Int, state: String): Int {
            val fallback = when (state) { "low" -> RED; "power_saving" -> ORANGE; "charging" -> TEAL; else -> GREEN }
            return context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getInt("battery_colour_${state}_$id", fallback)
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val options = manager.getAppWidgetOptions(id)
            val compact = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100 || options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180) < 250
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val devices = availableDevices(context)
            if (compact) {
                val selected = devices.firstOrNull { it.key == prefs.getString("battery_compact_device_$id", PHONE) } ?: devices.first()
                RemoteViews(context.packageName, R.layout.widget_battery_compact).apply {
                    setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(prefs, id))
                    setTextViewText(R.id.battery_percentage, selected.level?.let { "$it%" } ?: "—")
                    setImageViewResource(R.id.battery_icon, iconFor(selected))
                    setProgressBar(R.id.battery_compact_bar, 100, selected.level ?: 0, false)
                    setProgressTint(R.id.battery_compact_bar, colour(context, id, stateFor(context, selected)))
                    setTextColor(R.id.battery_percentage, WidgetAppearance.textColor(prefs, id))
                    setInt(R.id.battery_percentage, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                    setFloat(R.id.battery_percentage, "setTextSize", 30f * WidgetAppearance.textScale(prefs, id) / 100f)
                }.also { manager.updateAppWidget(id, it) }
            } else RemoteViews(context.packageName, R.layout.widget_battery_large).apply {
                setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(prefs, id))
                val rows = arrayOf(R.id.battery_row_1, R.id.battery_row_2, R.id.battery_row_3, R.id.battery_row_4)
                val labels = arrayOf(R.id.battery_label_1, R.id.battery_label_2, R.id.battery_label_3, R.id.battery_label_4)
                val bars = arrayOf(R.id.battery_bar_1, R.id.battery_bar_2, R.id.battery_bar_3, R.id.battery_bar_4)
                val icons = arrayOf(R.id.battery_icon_1, R.id.battery_icon_2, R.id.battery_icon_3, R.id.battery_icon_4)
                rows.indices.forEach { index ->
                    val device = devices.getOrNull(index)
                    setViewVisibility(rows[index], if (device == null) View.GONE else View.VISIBLE)
                    if (device != null) {
                        setTextViewText(labels[index], "${device.label}  ${device.level?.let { "$it%" } ?: "Unavailable"}")
                        setTextColor(labels[index], WidgetAppearance.textColor(prefs, id))
                        setFloat(labels[index], "setTextSize", 15f * WidgetAppearance.textScale(prefs, id) / 100f)
                        setImageViewResource(icons[index], iconFor(device))
                        setProgressBar(bars[index], 100, device.level ?: 0, false)
                        setProgressTint(bars[index], colour(context, id, stateFor(context, device)))
                    }
                }
            }.also { manager.updateAppWidget(id, it) }
        }

        private fun phoneDevice(context: Context): Device {
            val level = context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
            val status = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            return Device(PHONE, "Phone", level, R.drawable.phone, status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
        }

        @SuppressLint("MissingPermission")
        private fun bluetoothDevices(context: Context): List<Device> {
            if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return emptyList()
            val pairedDevices: Set<android.bluetooth.BluetoothDevice> = BluetoothAdapter.getDefaultAdapter()?.bondedDevices ?: emptySet()
            return pairedDevices.mapNotNull { device ->
                bluetoothBatteryLevel(device)?.let { level -> Device("bt:${device.address}", device.name ?: "Bluetooth device", level, deviceIcon(device.name)) }
            }.take(3)
        }

        private fun bluetoothBatteryLevel(device: android.bluetooth.BluetoothDevice): Int? = runCatching {
            (device.javaClass.methods.firstOrNull { it.name == "getBatteryLevel" && it.parameterCount == 0 }
                ?.invoke(device) as? Number)?.toInt()?.takeIf { it in 0..100 }
        }.getOrNull()

        private fun RemoteViews.setProgressTint(viewId: Int, colour: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setColorStateList(viewId, "setProgressTintList", ColorStateList.valueOf(colour))
            }
        }

        private fun deviceIcon(name: String?): Int = when {
            name.orEmpty().contains("bud", true) -> R.drawable.buds
            name.orEmpty().contains("watch", true) -> R.drawable.galaxy_watch
            name.orEmpty().contains("pen", true) -> R.drawable.pen_mode
            else -> R.drawable.devices
        }
        private fun iconFor(device: Device): Int = if (device.charging) if ((device.level ?: 0) >= 85) R.drawable.superfast_charging else R.drawable.charging else device.icon
        private fun stateFor(context: Context, device: Device): String = when {
            device.charging -> "charging"
            context.getSystemService(PowerManager::class.java).isPowerSaveMode -> "power_saving"
            (device.level ?: 100) <= 15 -> "low"
            else -> "normal"
        }
    }
}

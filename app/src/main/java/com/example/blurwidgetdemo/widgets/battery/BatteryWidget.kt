package com.example.blurwidgetdemo.widgets.battery

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.battery.BatteryStudioDescriptor
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.production.ProductionWidgetDesignRepository
import com.example.blurwidgetdemo.studio.production.ProductionWidgetDesignResolver
import com.example.blurwidgetdemo.studio.production.WidgetProductionDesignModeRepository
import com.example.blurwidgetdemo.widgets.variants.ExactWidgetVariants

open class BatteryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
        scheduleRefresh(context)
    }
    override fun onEnabled(context: Context) = scheduleRefresh(context)
    override fun onDisabled(context: Context) = context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    override fun onDeleted(context: Context, ids: IntArray) {
        val designModes = WidgetProductionDesignModeRepository(context)
        ids.forEach { designModes.delete(BatteryStudioDescriptor.WIDGET_ID, it) }
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateWidget(context, manager, id)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH || intent.action == Intent.ACTION_BATTERY_CHANGED || intent.action == Intent.ACTION_POWER_CONNECTED || intent.action == Intent.ACTION_POWER_DISCONNECTED || intent.action == android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED || intent.action == android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = ExactWidgetVariants.providersFor(BatteryWidget::class.java)
                .flatMap { provider -> manager.getAppWidgetIds(ComponentName(context, provider)).toList() }
                .distinct()
                .toIntArray()
            onUpdate(context, manager, ids)
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.example.blurwidgetdemo.action.REFRESH_BATTERY_WIDGET"
        data class Device(val key: String, val label: String, val level: Int?, val icon: Int, val charging: Boolean = false)
        const val PHONE = "phone"
        private const val RED = 0xFFF44336.toInt()
        private const val ORANGE = 0xFFFF9800.toInt()
        private const val GREEN = 0xFF4CAF50.toInt()
        private const val TEAL = 0xFF009688.toInt()

        fun availableDevices(context: Context): List<Device> = listOf(phoneDevice(context)) + bluetoothDevices(context)
        fun runtimeData(context: Context): BatteryRuntimeData = BatteryRuntimeData(
            devices = availableDevices(context).map { device ->
                BatteryDeviceRuntimeData(
                    key = device.key,
                    label = device.label,
                    percentage = device.level,
                    iconResId = iconFor(device),
                    charging = device.charging,
                    statusText = if (device.charging) "Charging" else "Battery"
                )
            },
            powerSaving = context.getSystemService(PowerManager::class.java).isPowerSaveMode
        )
        fun colour(context: Context, id: Int, state: String): Int {
            val fallback = when (state) { "low" -> RED; "power_saving" -> ORANGE; "charging" -> TEAL; else -> GREEN }
            return context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getInt("battery_colour_${state}_$id", fallback)
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val options = manager.getAppWidgetOptions(id)
            val renderSize = BatteryWidgetSizeResolver.fromOptions(options)
            val mode = WidgetProductionDesignModeRepository(context).mode(
                BatteryStudioDescriptor.WIDGET_ID,
                id
            )
            val promoted = when (
                val loaded = ProductionWidgetDesignRepository(context)
                    .loadActive(BatteryStudioDescriptor.WIDGET_ID)
            ) {
                is StudioRepositoryResult.Success -> loaded.value
                is StudioRepositoryResult.Failure -> null
            }
            val productionDesign = ProductionWidgetDesignResolver.resolve(
                mode = mode,
                promotedDesign = promoted,
                widgetId = BatteryStudioDescriptor.WIDGET_ID,
                widthDp = renderSize.widthDp,
                heightDp = renderSize.heightDp
            )
            val rendered = BatteryRemoteViewsRenderer.build(
                context = context,
                configuration = BatteryWidgetConfigurationRepository.load(context, id),
                runtimeData = runtimeData(context),
                renderSize = renderSize,
                design = productionDesign,
                mode = BatteryRenderMode.PRODUCTION
            )
            manager.updateAppWidget(id, rendered.remoteViews)
        }

        private fun refreshIntent(context: Context) = PendingIntent.getBroadcast(context, 7001,
            Intent(context, BatteryWidget::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        private fun scheduleRefresh(context: Context) {
            context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + 15_000L, refreshIntent(context))
        }

        private fun phoneDevice(context: Context): Device {
            val level = context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
            val status = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            return Device(PHONE, "Phone", level, R.drawable.phone, status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
        }

        @SuppressLint("MissingPermission")
        private fun bluetoothDevices(context: Context): List<Device> {
            if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return emptyList()
            val pairedDevices: Set<android.bluetooth.BluetoothDevice> =
                context.getSystemService(BluetoothManager::class.java).adapter?.bondedDevices ?: emptySet()
            val connectedAddresses = connectedBluetoothAddresses(context)
            return pairedDevices.mapNotNull { device ->
                if (device.address !in connectedAddresses) return@mapNotNull null
                bluetoothBatteryLevel(device)?.let { level -> Device("bt:${device.address}", device.name ?: "Bluetooth device", level, deviceIcon(device.name)) }
            }.take(3)
        }

        /** Android publishes battery levels only for some profiles; show those only while connected. */
        @SuppressLint("MissingPermission")
        private fun connectedBluetoothAddresses(context: Context): Set<String> = runCatching {
            val manager = context.getSystemService(BluetoothManager::class.java)
            listOf(
                BluetoothProfile.A2DP,
                BluetoothProfile.HEADSET,
                BluetoothProfile.GATT,
                if (Build.VERSION.SDK_INT >= 29) BluetoothProfile.HEARING_AID else null,
                if (Build.VERSION.SDK_INT >= 33) BluetoothProfile.LE_AUDIO else null
            ).filterNotNull().flatMap { profile -> manager.getConnectedDevices(profile) }
                .mapTo(mutableSetOf()) { it.address }
        }.getOrDefault(emptySet())

        private fun bluetoothBatteryLevel(device: android.bluetooth.BluetoothDevice): Int? = runCatching {
            (device.javaClass.methods.firstOrNull { it.name == "getBatteryLevel" && it.parameterCount == 0 }
                ?.invoke(device) as? Number)?.toInt()?.takeIf { it in 0..100 }
        }.getOrNull()

        private fun deviceIcon(name: String?): Int = when {
            name.orEmpty().contains("bud", true) -> R.drawable.buds
            name.orEmpty().contains("watch", true) -> R.drawable.galaxy_watch
            name.orEmpty().contains("pen", true) -> R.drawable.pen_mode
            else -> R.drawable.devices
        }
        private fun iconFor(device: Device): Int = if (device.charging) if ((device.level ?: 0) >= 85) R.drawable.superfast_charging else R.drawable.charging else device.icon
    }
}

package com.example.blurwidgetdemo.widgets.extra

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R

abstract class ToggleWidget : AppWidgetProvider() {
    abstract val icon: Int
    abstract fun action(context: Context): Intent
    protected open fun isEnabled(context: Context) = false
    open fun toggle(context: Context) = context.startActivity(action(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { id ->
        RemoteViews(context.packageName, R.layout.widget_toggle).apply {
            setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE), id))
            val active = isEnabled(context)
            setViewVisibility(R.id.toggle_circle, if (active) View.VISIBLE else View.GONE)
            val options = manager.getAppWidgetOptions(id)
            setImageViewBitmap(R.id.toggle_circle, ToggleOverlayRenderer.render(
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 40),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40),
                context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getInt("toggle_accent_$id", 0xFF387AFF.toInt())
            ))
            setImageViewResource(R.id.toggle_icon, icon)
            setOnClickPendingIntent(android.R.id.background, PendingIntent.getBroadcast(context, id, Intent(context, this@ToggleWidget::class.java).setAction(ACTION_TOGGLE), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }.also { manager.updateAppWidget(id, it) }
    }
    override fun onReceive(context: Context, intent: Intent) { if (intent.action == ACTION_TOGGLE) { toggle(context); val manager = AppWidgetManager.getInstance(context); onUpdate(context, manager, manager.getAppWidgetIds(ComponentName(context, javaClass))); return }; super.onReceive(context, intent) }
    companion object { const val ACTION_TOGGLE = "com.example.blurwidgetdemo.action.TOGGLE" }
}
class DataToggleWidget : ToggleWidget() { override val icon = R.drawable.network_storage; override fun action(context: Context) = Intent(Settings.ACTION_WIRELESS_SETTINGS) }
class DarkModeToggleWidget : ToggleWidget() { override val icon = R.drawable.dark; override fun action(context: Context) = Intent(Settings.ACTION_DISPLAY_SETTINGS); override fun isEnabled(context: Context) = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES }
class WifiToggleWidget : ToggleWidget() { override val icon = R.drawable.wifi_2; override fun action(context: Context) = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY); override fun isEnabled(context: Context) = context.applicationContext.getSystemService(android.net.wifi.WifiManager::class.java).isWifiEnabled }
class DoNotDisturbToggleWidget : ToggleWidget() { override val icon = R.drawable.do_not_disturb; override fun action(context: Context) = Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS); override fun isEnabled(context: Context) = runCatching { context.getSystemService(android.app.NotificationManager::class.java).currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL }.getOrDefault(false); override fun toggle(context: Context) { val manager = context.getSystemService(android.app.NotificationManager::class.java); if (!manager.isNotificationPolicyAccessGranted) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return }; manager.setInterruptionFilter(if (isEnabled(context)) android.app.NotificationManager.INTERRUPTION_FILTER_ALL else android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY) } }
class BluetoothToggleWidget : ToggleWidget() { override val icon = R.drawable.devices; override fun action(context: Context) = Intent(Settings.ACTION_BLUETOOTH_SETTINGS); @android.annotation.SuppressLint("MissingPermission") override fun isEnabled(context: Context) = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false) }
class LocationToggleWidget : ToggleWidget() { override val icon = R.drawable.location; override fun action(context: Context) = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS); override fun isEnabled(context: Context): Boolean { val manager = context.getSystemService(LocationManager::class.java); return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) manager.isLocationEnabled else manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } }
class HotspotToggleWidget : ToggleWidget() { override val icon = R.drawable.wifi_2; override fun action(context: Context) = Intent(Settings.ACTION_WIRELESS_SETTINGS) }
class CameraToggleWidget : ToggleWidget() { override val icon = R.drawable.image; override fun action(context: Context) = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA) }
class TorchToggleWidget : ToggleWidget() { override val icon = R.drawable.flashlight; override fun action(context: Context) = Intent(Settings.ACTION_SETTINGS); override fun isEnabled(context: Context) = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getBoolean("torch_enabled", false); @android.annotation.SuppressLint("MissingPermission") override fun toggle(context: Context) { runCatching { val camera = context.getSystemService(android.hardware.camera2.CameraManager::class.java); val id = camera.cameraIdList.firstOrNull { camera.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } ?: return; val enabled = !isEnabled(context); camera.setTorchMode(id, enabled); context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit().putBoolean("torch_enabled", enabled).apply() }.onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } }

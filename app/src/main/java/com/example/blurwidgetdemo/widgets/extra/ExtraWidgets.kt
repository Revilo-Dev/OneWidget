package com.example.blurwidgetdemo.widgets.extra

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.bluetooth.BluetoothAdapter
import android.app.NotificationManager
import android.location.LocationManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetAppearance
import java.text.DateFormat
import java.util.Date

abstract class NamedActionWidget : AppWidgetProvider() {
    abstract val title: String
    abstract val icon: Int
    abstract fun action(context: Context, id: Int): Intent
    protected open fun displayText(context: Context, id: Int) = title
    protected open fun iconBitmap(context: Context, id: Int): Bitmap? = null
    protected open val isToggle = false
    protected open fun isEnabled(context: Context): Boolean = false
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { render(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) = render(context, manager, id)
    private fun render(context: Context, manager: AppWidgetManager, id: Int) {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        if (isToggle) {
            RemoteViews(context.packageName, R.layout.widget_toggle).apply {
                val active = isEnabled(context)
                val accent = prefs.getInt("toggle_accent_$id", 0xFF387AFF.toInt())
                setViewVisibility(R.id.toggle_circle, if (active) View.VISIBLE else View.GONE)
                setInt(R.id.toggle_circle, "setColorFilter", accent)
                setImageViewResource(R.id.toggle_icon, icon)
                val tapIntent = if (this@NamedActionWidget is ToggleWidget) Intent(context, this@NamedActionWidget::class.java).setAction(ToggleWidget.ACTION_TOGGLE) else action(context, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val tap = if (this@NamedActionWidget is ToggleWidget) PendingIntent.getBroadcast(context, id, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) else PendingIntent.getActivity(context, id, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                setOnClickPendingIntent(android.R.id.background, tap)
            }.also { manager.updateAppWidget(id, it) }
            return
        }
        val options = manager.getAppWidgetOptions(id)
        val iconOnly = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 80) <= 100 && options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
        RemoteViews(context.packageName, R.layout.widget_action).apply {
            setInt(android.R.id.background, "setBackgroundColor", BlurWidget.tintColor(prefs, id))
            iconBitmap(context, id)?.let { setImageViewBitmap(R.id.action_icon, it) } ?: setImageViewResource(R.id.action_icon, icon)
            setTextViewText(R.id.action_label, displayText(context, id))
            setViewVisibility(R.id.action_label, if (iconOnly) View.GONE else View.VISIBLE)
            setTextColor(R.id.action_label, WidgetAppearance.textColor(prefs, id))
            setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, id, action(context, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }.also { manager.updateAppWidget(id, it) }
    }
}

class GoogleSearchWidget : NamedActionWidget() { override val title = "Google Search"; override val icon = R.drawable.google; override fun action(context: Context, id: Int) = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", "") }
abstract class ToggleWidget : NamedActionWidget() {
    override val isToggle = true
    open fun toggle(context: Context) = context.startActivity(action(context, 0).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) { toggle(context); val manager = AppWidgetManager.getInstance(context); onUpdate(context, manager, manager.getAppWidgetIds(ComponentName(context, javaClass))); return }
        super.onReceive(context, intent)
    }
    companion object { const val ACTION_TOGGLE = "com.example.blurwidgetdemo.action.TOGGLE" }
}
class DataToggleWidget : ToggleWidget() { override val title = "Mobile data"; override val icon = R.drawable.network_storage; override fun action(context: Context, id: Int) = Intent(Settings.ACTION_WIRELESS_SETTINGS) }
class DarkModeToggleWidget : ToggleWidget() { override val title = "Dark mode"; override val icon = R.drawable.dark; override fun action(context: Context, id: Int) = Intent(Settings.ACTION_DISPLAY_SETTINGS); override fun isEnabled(context: Context) = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES }
class WifiToggleWidget : ToggleWidget() { override val title = "Wi-Fi"; override val icon = R.drawable.wifi_2; override fun action(context: Context, id: Int) = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY); override fun isEnabled(context: Context) = context.applicationContext.getSystemService(android.net.wifi.WifiManager::class.java).isWifiEnabled }
class DoNotDisturbToggleWidget : ToggleWidget() { override val title = "Do not disturb"; override val icon = R.drawable.do_not_disturb; override fun action(context: Context, id: Int) = Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS); override fun isEnabled(context: Context) = runCatching { context.getSystemService(NotificationManager::class.java).currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL }.getOrDefault(false); override fun toggle(context: Context) { val manager = context.getSystemService(NotificationManager::class.java); if (!manager.isNotificationPolicyAccessGranted) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return }; manager.setInterruptionFilter(if (isEnabled(context)) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY) } }
class BluetoothToggleWidget : ToggleWidget() { override val title = "Bluetooth"; override val icon = R.drawable.devices; override fun action(context: Context, id: Int) = Intent(Settings.ACTION_BLUETOOTH_SETTINGS); @android.annotation.SuppressLint("MissingPermission") override fun isEnabled(context: Context) = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false) }
class LocationToggleWidget : ToggleWidget() { override val title = "Location"; override val icon = R.drawable.location; override fun action(context: Context, id: Int) = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS); override fun isEnabled(context: Context): Boolean { val manager = context.getSystemService(LocationManager::class.java); return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) manager.isLocationEnabled else manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } }
class HotspotToggleWidget : ToggleWidget() { override val title = "Mobile hotspot"; override val icon = R.drawable.wifi_2; override fun action(context: Context, id: Int) = Intent(Settings.ACTION_WIRELESS_SETTINGS) }
class CameraToggleWidget : ToggleWidget() { override val title = "Camera"; override val icon = R.drawable.image; override fun action(context: Context, id: Int) = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA) }
class TorchToggleWidget : ToggleWidget() { override val title = "Torch"; override val icon = R.drawable.flashlight; override fun action(context: Context, id: Int) = Intent(Settings.ACTION_SETTINGS); override fun isEnabled(context: Context) = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getBoolean("torch_enabled", false); @android.annotation.SuppressLint("MissingPermission") override fun toggle(context: Context) { runCatching { val camera = context.getSystemService(android.hardware.camera2.CameraManager::class.java); val id = camera.cameraIdList.firstOrNull { camera.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } ?: return; val enabled = !isEnabled(context); camera.setTorchMode(id, enabled); context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit().putBoolean("torch_enabled", enabled).apply() }.onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } }
class StepCounterWidget : NamedActionWidget() {
    override val title = "Steps"; override val icon = R.drawable.running
    override fun action(context: Context, id: Int) = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_FITNESS)
    override fun displayText(context: Context, id: Int): String {
        val steps = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getLong("step_counter_$id", -1)
        return if (steps >= 0) "$steps steps" else "Steps"
    }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        if (context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) return
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val result = goAsync()
        sensorManager.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val steps = event.values.firstOrNull()?.toLong() ?: return
                context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit().apply { ids.forEach { putLong("step_counter_$it", steps) } }.apply()
                sensorManager.unregisterListener(this)
                this@StepCounterWidget.refreshViews(context, manager, ids)
                result.finish()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    private fun refreshViews(context: Context, manager: AppWidgetManager, ids: IntArray) = super.onUpdate(context, manager, ids)
}
class TextWidget : NamedActionWidget() { override val title = "Text"; override val icon = R.drawable.text_check_on; override fun action(context: Context, id: Int) = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME); override fun displayText(context: Context, id: Int) = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getString("text_widget_$id", "Text") ?: "Text" }
class AlarmWidget : NamedActionWidget() { override val title = "Alarm"; override val icon = R.drawable.time; override fun action(context: Context, id: Int) = Intent(AlarmClock.ACTION_SHOW_ALARMS); override fun displayText(context: Context, id: Int): String { val next = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime; return if (next == null) "No alarm set" else "Alarm ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(next))}" } }
class MediaPlayerWidget : NamedActionWidget() { override val title = "Media player"; override val icon = R.drawable.music_alt; override fun action(context: Context, id: Int) = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC) }
class OpenAppWidget : NamedActionWidget() {
    override val title = "Open app"; override val icon = R.drawable.phone
    override fun action(context: Context, id: Int): Intent { val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE); val pkg = prefs.getString("open_app_package_$id", null); val cls = prefs.getString("open_app_class_$id", null); return if (pkg != null && cls != null) Intent().setComponent(ComponentName(pkg, cls)) else Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER) }
    override fun displayText(context: Context, id: Int) = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getString("open_app_label_$id", title) ?: title
    override fun iconBitmap(context: Context, id: Int): Bitmap? = runCatching { val pkg = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getString("open_app_package_$id", null) ?: return null; drawableBitmap(context.packageManager.getApplicationIcon(pkg)) }.getOrNull()
}
private fun drawableBitmap(drawable: Drawable): Bitmap { val width = drawable.intrinsicWidth.coerceAtLeast(1); val height = drawable.intrinsicHeight.coerceAtLeast(1); return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { drawable.setBounds(0, 0, width, height); drawable.draw(Canvas(it)) } }

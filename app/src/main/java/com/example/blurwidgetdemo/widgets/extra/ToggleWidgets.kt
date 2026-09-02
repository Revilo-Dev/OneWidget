package com.example.blurwidgetdemo.widgets.extra

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.universal.UniversalElementContent
import com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetActions
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetContent
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds

enum class ToggleKind(val label: String, val icon: Int) {
    DATA("Mobile data", R.drawable.network_storage), DARK_MODE("Dark mode", R.drawable.dark), WIFI("Wi-Fi", R.drawable.wifi_2),
    DO_NOT_DISTURB("Do not disturb", R.drawable.do_not_disturb), BLUETOOTH("Bluetooth", R.drawable.devices), LOCATION("Location", R.drawable.location),
    HOTSPOT("Mobile hotspot", R.drawable.wifi_2), CAMERA("Camera", R.drawable.image), TORCH("Torch", R.drawable.flashlight);
    companion object { fun from(value: String?) = entries.firstOrNull { it.name == value } ?: WIFI }
}

/** One configurable toggle widget. Resize it between the 1×1 and 1×2 variants. */
class ToggleWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { render(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) = render(context, manager, id)
    override fun onDeleted(context: Context, ids: IntArray) =
        UniversalProductionWidgetRenderer.deleteModes(context, UniversalWidgetIds.TOGGLE, ids)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) { toggle(context, id); render(context, AppWidgetManager.getInstance(context), id) }
        }
    }

    private fun render(context: Context, manager: AppWidgetManager, id: Int) {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        val options = manager.getAppWidgetOptions(id)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 40)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40)
        val kind = ToggleKind.from(prefs.getString("toggle_kind_$id", options.getString(OPTION_TOGGLE_KIND)))
        val layout = if (width <= 100 && height <= 100) R.layout.widget_toggle_square else R.layout.widget_toggle
        val enabled = isEnabled(context, id, kind)
        val accent = prefs.getInt("toggle_accent_$id", 0xFF387AFF.toInt())
        val overlay = if (width <= 100 && height <= 100) {
            ToggleOverlayRenderer.renderCircle(width, height, accent)
        } else {
            ToggleOverlayRenderer.renderFill(width, height, accent)
        }
        val rootAction = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, ToggleWidget::class.java).setAction(ACTION_TOGGLE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val promoted = UniversalProductionWidgetRenderer.buildIfPromoted(
            context,
            manager,
            id,
            UniversalWidgetIds.TOGGLE,
            UniversalWidgetContent(
                mapOf(
                    "universal.state" to UniversalElementContent(bitmap = overlay, visible = enabled),
                    "universal.icon" to UniversalElementContent(imageResId = kind.icon)
                )
            ),
            UniversalWidgetActions(root = rootAction)
        )
        val views = promoted ?: RemoteViews(context.packageName, layout).apply {
            BlurWidget.applySurfaceTint(this, prefs, id)
            setViewVisibility(R.id.toggle_circle, if (enabled) View.VISIBLE else View.GONE)
            setImageViewBitmap(R.id.toggle_circle, overlay)
            setImageViewResource(R.id.toggle_icon, kind.icon)
            setOnClickPendingIntent(android.R.id.background, rootAction)
        }
        manager.updateAppWidget(id, views)
    }

    private fun isEnabled(context: Context, id: Int, kind: ToggleKind): Boolean = when (kind) {
        ToggleKind.DARK_MODE -> (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        ToggleKind.WIFI -> context.applicationContext.getSystemService(android.net.wifi.WifiManager::class.java).isWifiEnabled
        ToggleKind.DO_NOT_DISTURB -> runCatching { context.getSystemService(android.app.NotificationManager::class.java).currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL }.getOrDefault(false)
        ToggleKind.BLUETOOTH -> runCatching {
            context.getSystemService(BluetoothManager::class.java).adapter?.isEnabled == true
        }.getOrDefault(false)
        ToggleKind.LOCATION -> context.getSystemService(LocationManager::class.java).let { if (android.os.Build.VERSION.SDK_INT >= 28) it.isLocationEnabled else it.isProviderEnabled(LocationManager.GPS_PROVIDER) || it.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }
        ToggleKind.TORCH -> context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getBoolean("torch_enabled_$id", false)
        else -> false
    }

    private fun toggle(context: Context, id: Int) {
        val kind = ToggleKind.from(
            context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
                .getString("toggle_kind_$id", null)
        )
        val target = when (kind) {
            ToggleKind.DARK_MODE -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            ToggleKind.WIFI -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            ToggleKind.BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            ToggleKind.LOCATION -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            ToggleKind.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            ToggleKind.DO_NOT_DISTURB -> Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
            ToggleKind.HOTSPOT, ToggleKind.DATA -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
            ToggleKind.TORCH -> Intent(Settings.ACTION_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(target) }
            .recoverCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
    }
    companion object {
        const val ACTION_TOGGLE = "com.example.blurwidgetdemo.action.TOGGLE"
        const val OPTION_TOGGLE_KIND = "com.example.blurwidgetdemo.option.TOGGLE_KIND"
    }
}

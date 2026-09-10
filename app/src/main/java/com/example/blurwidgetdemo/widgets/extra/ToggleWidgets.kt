package dev.revilodev.onewidget.widgets.extra

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.universal.UniversalElementContent
import dev.revilodev.onewidget.studio.universal.UniversalProductionWidgetRenderer
import dev.revilodev.onewidget.studio.universal.UniversalWidgetActions
import dev.revilodev.onewidget.studio.universal.UniversalWidgetContent
import dev.revilodev.onewidget.studio.universal.UniversalWidgetIds

enum class ToggleKind(val label: String, val icon: Int) {
    DATA("Mobile data", R.drawable.network_storage), DARK_MODE("Dark mode", R.drawable.dark), WIFI("Wi-Fi", R.drawable.wifi_2),
    DO_NOT_DISTURB("Do not disturb", R.drawable.do_not_disturb), BLUETOOTH("Bluetooth", R.drawable.bluetooth), LOCATION("Location", R.drawable.location),
    HOTSPOT("Mobile hotspot", R.drawable.wifi_2), CAMERA("Camera", R.drawable.camera_new), TORCH("Torch", R.drawable.flashlight);
    companion object { fun from(value: String?) = entries.firstOrNull { it.name == value } ?: WIFI }
}

/** One configurable toggle widget. Resize it horizontally between the 1×1 and 2×1 variants. */
open class ToggleWidget : AppWidgetProvider() {
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
        // One UI can report its physical 1x1 cell as wider than 100dp on dense home grids.
        // The exact provider identity is authoritative when deciding circle versus pill.
        val exactOneByOne = manager.getAppWidgetInfo(id)?.provider?.className
            ?.endsWith(".widgets.variants.Toggle1x1Widget") == true
        val singleCell = exactOneByOne || (width <= 100 && height <= 100)
        val kind = ToggleKind.from(prefs.getString("toggle_kind_$id", options.getString(OPTION_TOGGLE_KIND)))
        val layout = if (singleCell) R.layout.widget_toggle_square else R.layout.widget_toggle
        val enabled = isEnabled(context, id, kind)
        val displayIcon = if (kind == ToggleKind.BLUETOOTH && !enabled) R.drawable.bluetooth_off else kind.icon
        val accent = prefs.getInt("toggle_accent_$id", 0xFF387AFF.toInt())
        val overlay = if (singleCell) {
            ToggleOverlayRenderer.renderCircle(width, height, if (enabled) accent else 0x667F8798)
        } else {
            ToggleOverlayRenderer.renderFill(width, height, accent)
        }
        val rootAction = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, this::class.java).setAction(ACTION_TOGGLE)
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
                    "universal.icon" to UniversalElementContent(imageResId = displayIcon)
                )
            ),
            UniversalWidgetActions(root = rootAction)
        )
        val views = promoted ?: RemoteViews(context.packageName, layout).apply {
            if (!singleCell) BlurWidget.applySurfaceTint(
                this,
                prefs,
                id,
                if (dev.revilodev.onewidget.widgets.WidgetAppearance.bordersEnabled(prefs)) {
                    R.drawable.widget_clock_background_fully_rounded
                } else {
                    R.drawable.widget_clock_background_fully_rounded_no_border
                }
            )
            setViewVisibility(R.id.toggle_circle, View.VISIBLE)
            setImageViewBitmap(R.id.toggle_circle, overlay)
            setImageViewResource(R.id.toggle_icon, displayIcon)
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
        if (kind == ToggleKind.TORCH && toggleTorch(context, id)) return
        if (kind == ToggleKind.DO_NOT_DISTURB && toggleDoNotDisturb(context)) return
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
            ToggleKind.DO_NOT_DISTURB -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            ToggleKind.DATA -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            }
            ToggleKind.HOTSPOT -> Intent("android.settings.TETHER_SETTINGS")
            ToggleKind.TORCH -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(target) }
            .recoverCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
    }

    private fun toggleDoNotDisturb(context: Context): Boolean {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        if (!manager.isNotificationPolicyAccessGranted) return false
        return runCatching {
            val enabled = manager.currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
            manager.setInterruptionFilter(
                if (enabled) android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                else android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
            )
            true
        }.getOrDefault(false)
    }

    private fun toggleTorch(context: Context, id: Int): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return true
        }
        return runCatching {
            val manager = context.getSystemService(CameraManager::class.java)
            val cameraId = manager.cameraIdList.firstOrNull { candidate ->
                manager.getCameraCharacteristics(candidate)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val enabled = !prefs.getBoolean("torch_enabled_$id", false)
            manager.setTorchMode(cameraId, enabled)
            prefs.edit().putBoolean("torch_enabled_$id", enabled).apply()
            true
        }.getOrDefault(false)
    }
    companion object {
        const val ACTION_TOGGLE = "dev.revilodev.onewidget.action.TOGGLE"
        const val OPTION_TOGGLE_KIND = "dev.revilodev.onewidget.option.TOGGLE_KIND"
    }
}

/** Legacy provider identity retained for existing V2 widgets. */
class ToggleWidgetV2 : ToggleWidget()

/** Fresh provider identity makes One UI reload the corrected tiny/small size contract. */
class ToggleWidgetV3 : ToggleWidget()

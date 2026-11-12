package dev.revilodev.onewidget

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.CalendarContract
import dev.revilodev.onewidget.widgets.WidgetUpdates

class OneWidgetApplication : Application() {
    private val handler = Handler(Looper.getMainLooper())
    private var batteryState = ""
    private val refreshBattery = Runnable { WidgetUpdates.requestMatching(this) { it.contains("Battery") || it.contains("DigitalClock") } }
    private val refreshCalendar = Runnable { WidgetUpdates.requestMatching(this) { it.contains("Calendar") || it.contains("NextEvent") } }

    override fun onCreate() {
        super.onCreate()
        cancelLegacyAlarms()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> WidgetUpdates.requestAll(context, dynamicOnly = true)
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val state = intent.getIntExtra("level", -1).toString() + ":" + intent.getIntExtra("status", -1)
                        if (state == batteryState) return
                        batteryState = state
                        if (getSystemService(PowerManager::class.java).isInteractive) {
                            handler.removeCallbacks(refreshBattery)
                            handler.postDelayed(refreshBattery, 500)
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(receiver, filter)
        if (checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handler.removeCallbacks(refreshCalendar)
                handler.postDelayed(refreshCalendar, 500)
            }
        })
    }

    private fun cancelLegacyAlarms() {
        val alarm = getSystemService(AlarmManager::class.java)
        listOf(
            Triple("widgets.battery.BatteryWidget", "REFRESH_BATTERY_WIDGET", 7001),
            Triple("widgets.storage.StorageWidget", "REFRESH_STORAGE_WIDGET", 7002),
            Triple("widgets.clock.DigitalClockWidget", "CLOCK_TYPEFACE_REFRESH", 9472)
        ).forEach { (provider, action, request) ->
            val intent = Intent().setClassName(this, packageName + "." + provider).setAction(packageName + ".action." + action)
            PendingIntent.getBroadcast(this, request, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                alarm.cancel(it)
                it.cancel()
            }
        }
    }
}

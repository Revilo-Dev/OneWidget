package dev.revilodev.onewidget.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock

object WidgetUpdates {
    private const val INTERVAL = 5 * 60_000L
    private var scheduled = false

    fun requestAll(context: Context, dynamicOnly: Boolean = false) {
        requestMatching(context) { !dynamicOnly || isDynamic(it) }
    }

    fun requestMatching(context: Context, matches: (String) -> Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        manager.installedProviders.filter { it.provider.packageName == context.packageName &&
            it.widgetCategory and android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }.forEach { provider ->
            if (!matches(provider.provider.className)) return@forEach
            val ids = manager.getAppWidgetIds(provider.provider)
            if (ids.isNotEmpty()) context.sendBroadcast(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setComponent(provider.provider).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
        }
        ensureScheduled(context)
    }

    private fun isDynamic(name: String) = listOf("Battery", "Clock", "Storage", "Calendar", "NextEvent", "Weather", "StepCounter", "Alarm", "MediaPlayer", "Hydration", "Counter", "Todo").any(name::contains)

    fun ensureScheduled(context: Context, force: Boolean = false) {
        if (scheduled && !force) return
        val manager = AppWidgetManager.getInstance(context)
        val active = manager.installedProviders.any { it.provider.packageName == context.packageName &&
            isDynamic(it.provider.className) && manager.getAppWidgetIds(it.provider).isNotEmpty() }
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(context, 7300, Intent(context, WidgetUpdateReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (active) alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + INTERVAL, INTERVAL, pending)
        else alarm.cancel(pending)
        scheduled = active
    }
}

class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetUpdates.ensureScheduled(context, force = true)
        if (intent.action == null && !context.getSystemService(PowerManager::class.java).isInteractive) return
        WidgetUpdates.requestAll(context, dynamicOnly = true)
        WidgetUpdates.ensureScheduled(context, force = true)
    }
}

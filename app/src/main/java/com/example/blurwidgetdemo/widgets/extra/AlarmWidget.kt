package com.example.blurwidgetdemo.widgets.extra

import android.app.PendingIntent
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.provider.AlarmClock
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.universal.UniversalElementContent
import com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetActions
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetContent
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Widget-local daily alarm controls. Android does not expose other apps' alarms for toggling. */
class AlarmWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { updateWidget(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateWidget(context, manager, id)

    override fun onDeleted(context: Context, ids: IntArray) =
        UniversalProductionWidgetRenderer.deleteModes(context, UniversalWidgetIds.ALARM, ids)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        when (intent.action) {
            ACTION_TOGGLE -> prefs.edit()
                .putBoolean("alarm_enabled_$id", !prefs.getBoolean("alarm_enabled_$id", true))
                .remove("alarm_snoozed_$id")
                .apply()
            ACTION_SNOOZE -> prefs.edit().putString("alarm_snoozed_$id", today()).apply()
            else -> return
        }
        updateWidget(context, AppWidgetManager.getInstance(context), id)
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.blurwidgetdemo.action.TOGGLE_ALARM"
        const val ACTION_SNOOZE = "com.example.blurwidgetdemo.action.SNOOZE_ALARM"

        fun saveSelection(context: Context, id: Int, hour: Int, minute: Int) {
            context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit()
                .putInt("alarm_hour_$id", hour)
                .putInt("alarm_minute_$id", minute)
                .putBoolean("alarm_enabled_$id", true)
                .remove("alarm_snoozed_$id")
                .apply()
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val large = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) > 100
            val hour = prefs.getInt("alarm_hour_$id", -1)
            val hasAlarm = hour >= 0
            val enabled = prefs.getBoolean("alarm_enabled_$id", true)
            val snoozed = prefs.getString("alarm_snoozed_$id", null) == today()
            val active = hasAlarm && enabled && !snoozed
            val layout = if (large) R.layout.widget_alarm_large else R.layout.widget_alarm_compact
            val time = if (hasAlarm) formatTime(hour, prefs.getInt("alarm_minute_$id", 0)) else "Pick an alarm"
            val status = when {
                !hasAlarm -> "Tap to choose a daily alarm"
                snoozed -> "Snoozed for today"
                active -> "Daily • On"
                else -> "Daily • Off"
            }
            val toggleAction = broadcast(context, id, ACTION_TOGGLE, 0)
            val snoozeAction = broadcast(context, id, ACTION_SNOOZE, 1)
            val rootAction = clockPendingIntent(context, id)
            val promoted = UniversalProductionWidgetRenderer.buildIfPromoted(
                context,
                manager,
                id,
                UniversalWidgetIds.ALARM,
                UniversalWidgetContent(
                    mapOf(
                        "universal.toggle" to UniversalElementContent(
                            imageResId = R.drawable.category_clock,
                            imageAlpha = if (active) 255 else 92
                        ),
                        "universal.time" to UniversalElementContent(text = time),
                        "universal.status" to UniversalElementContent(text = status),
                        "universal.snooze" to UniversalElementContent(
                            text = "Snooze today",
                            visible = hasAlarm && active
                        )
                    )
                ),
                UniversalWidgetActions(
                    root = rootAction,
                    elements = mapOf(
                        "universal.toggle" to toggleAction,
                        "universal.snooze" to snoozeAction
                    )
                )
            )
            val views = promoted ?: RemoteViews(context.packageName, layout).apply {
                BlurWidget.applySurfaceTint(this, prefs, id)
                setImageViewResource(R.id.alarm_toggle, R.drawable.category_clock)
                setTextViewText(R.id.alarm_time, time)
                setTextViewText(R.id.alarm_status, status)
                setViewVisibility(R.id.alarm_snooze, if (large && hasAlarm && active) View.VISIBLE else View.GONE)
                setInt(R.id.alarm_toggle, "setImageAlpha", if (active) 255 else 92)
                setOnClickPendingIntent(R.id.alarm_toggle, toggleAction)
                setOnClickPendingIntent(R.id.alarm_snooze, snoozeAction)
                setOnClickPendingIntent(android.R.id.background, rootAction)
            }
            manager.updateAppWidget(id, views)
        }

        private fun broadcast(context: Context, id: Int, action: String, offset: Int) = PendingIntent.getBroadcast(
            context, id * 10 + offset,
            Intent(context, AlarmWidget::class.java).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun formatTime(hour: Int, minute: Int): String = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(time)
        }

        private fun today() = SimpleDateFormat("yyyyMMdd", Locale.US).format(java.util.Date())

        private fun clockPendingIntent(context: Context, id: Int): PendingIntent {
            val samsungClock = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(SAMSUNG_CLOCK_PACKAGE, SAMSUNG_CLOCK_ACTIVITY))
            val intent = if (samsungClock.resolveActivity(context.packageManager) != null) samsungClock else Intent(AlarmClock.ACTION_SHOW_ALARMS)
            return PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private const val SAMSUNG_CLOCK_PACKAGE = "com.sec.android.app.clockpackage"
        private const val SAMSUNG_CLOCK_ACTIVITY = "com.sec.android.app.clockpackage.ClockPackage"
    }
}

/** Shown both during widget placement and when a user taps the alarm card to change its time. */
class AlarmWidgetConfigActivity : AppCompatActivity() {
    private var id = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val now = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            AlarmWidget.saveSelection(this, id, hour, minute)
            com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
                .initializeConfiguredWidget(this, com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds.ALARM, id)
            AlarmWidget.updateWidget(this, AppWidgetManager.getInstance(this), id)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
            finish()
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).apply {
            setTitle("Choose daily alarm")
            setOnCancelListener { finish() }
            show()
        }
    }
}

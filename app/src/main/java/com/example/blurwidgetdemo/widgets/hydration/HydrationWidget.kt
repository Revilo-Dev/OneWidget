package com.example.blurwidgetdemo.widgets.hydration

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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

/** Daily water counter with a per-widget scheduled clear time (midnight by default). */
class HydrationWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { updateWidget(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateWidget(context, manager, id)

    override fun onDeleted(context: Context, ids: IntArray) =
        UniversalProductionWidgetRenderer.deleteModes(context, UniversalWidgetIds.HYDRATION, ids)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val change = when (intent.action) {
            ACTION_ADD_WATER -> 1
            ACTION_REMOVE_WATER -> -1
            ACTION_RESET -> 0
            else -> return
        }
        if (change != 0) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt("hydration_$id", (currentCount(prefs, id) + change).coerceIn(0, DAILY_GOAL))
                .putString("hydration_slot_$id", resetSlot(prefs.getInt("hydration_hour_$id", 0), prefs.getInt("hydration_minute_$id", 0)))
                .apply()
        }
        updateWidget(context, AppWidgetManager.getInstance(context), id)
    }

    companion object {
        private const val ACTION_ADD_WATER = "com.example.blurwidgetdemo.action.ADD_WATER"
        private const val ACTION_REMOVE_WATER = "com.example.blurwidgetdemo.action.REMOVE_WATER"
        private const val ACTION_RESET = "com.example.blurwidgetdemo.action.RESET_HYDRATION"
        private const val DAILY_GOAL = 8

        fun saveResetTime(context: Context, id: Int, hour: Int, minute: Int) {
            context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit()
                .putInt("hydration_hour_$id", hour).putInt("hydration_minute_$id", minute)
                .putString("hydration_slot_$id", resetSlot(hour, minute)).putInt("hydration_$id", 0).apply()
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val count = currentCount(prefs, id)
            val compact = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
            val layout = if (compact) R.layout.widget_hydration_compact else R.layout.widget_hydration_large
            val minusAction = action(context, id, ACTION_REMOVE_WATER, 0)
            val plusAction = action(context, id, ACTION_ADD_WATER, 1)
            val promoted = UniversalProductionWidgetRenderer.buildIfPromoted(
                context,
                manager,
                id,
                UniversalWidgetIds.HYDRATION,
                UniversalWidgetContent(
                    mapOf(
                        "universal.icon" to UniversalElementContent(imageResId = R.drawable.ic_water_glass),
                        "universal.count" to UniversalElementContent(text = count.toString()),
                        "universal.goal" to UniversalElementContent(text = "of $DAILY_GOAL glasses"),
                        "universal.minus" to UniversalElementContent(imageResId = R.drawable.minus),
                        "universal.plus" to UniversalElementContent(imageResId = R.drawable.plus)
                    )
                ),
                UniversalWidgetActions(
                    elements = mapOf(
                        "universal.minus" to minusAction,
                        "universal.plus" to plusAction
                    )
                )
            )
            val views = promoted ?: RemoteViews(context.packageName, layout).apply {
                BlurWidget.applySurfaceTint(this, prefs, id)
                setImageViewResource(R.id.hydration_icon, R.drawable.ic_water_glass)
                setImageViewResource(R.id.hydration_minus, R.drawable.minus)
                setImageViewResource(R.id.hydration_plus, R.drawable.plus)
                setTextViewText(R.id.hydration_count, count.toString())
                setTextViewText(R.id.hydration_goal, "of $DAILY_GOAL glasses")
                setViewVisibility(R.id.hydration_goal, if (compact) View.GONE else View.VISIBLE)
                setOnClickPendingIntent(R.id.hydration_minus, minusAction)
                setOnClickPendingIntent(R.id.hydration_plus, plusAction)
            }
            manager.updateAppWidget(id, views)
            scheduleReset(context, id)
        }

        private fun currentCount(prefs: android.content.SharedPreferences, id: Int): Int {
            val hour = prefs.getInt("hydration_hour_$id", 0)
            val minute = prefs.getInt("hydration_minute_$id", 0)
            val slot = resetSlot(hour, minute)
            return if (prefs.getString("hydration_slot_$id", null) == slot) prefs.getInt("hydration_$id", 0)
            else {
                prefs.edit().putString("hydration_slot_$id", slot).putInt("hydration_$id", 0).apply()
                0
            }
        }

        private fun scheduleReset(context: Context, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val reset = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, prefs.getInt("hydration_hour_$id", 0)); set(Calendar.MINUTE, prefs.getInt("hydration_minute_$id", 0))
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reset.timeInMillis, action(context, id, ACTION_RESET, 2))
        }

        private fun action(context: Context, id: Int, action: String, offset: Int) = PendingIntent.getBroadcast(
            context, id * 10 + offset,
            Intent(context, HydrationWidget::class.java).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun resetSlot(hour: Int, minute: Int): String {
            val now = Calendar.getInstance()
            val reset = now.clone() as Calendar
            reset.set(Calendar.HOUR_OF_DAY, hour); reset.set(Calendar.MINUTE, minute); reset.set(Calendar.SECOND, 0); reset.set(Calendar.MILLISECOND, 0)
            if (now.before(reset)) reset.add(Calendar.DAY_OF_YEAR, -1)
            return SimpleDateFormat("yyyyMMdd", Locale.US).format(reset.time)
        }
    }
}

/** The Hydration reconfigure screen: choose the daily clear time, midnight by default. */
class HydrationConfigActivity : AppCompatActivity() {
    private var id = AppWidgetManager.INVALID_APPWIDGET_ID
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val prefs = getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE)
        TimePickerDialog(this, { _, hour, minute ->
            HydrationWidget.saveResetTime(this, id, hour, minute)
            com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
                .initializeConfiguredWidget(this, com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds.HYDRATION, id)
            HydrationWidget.updateWidget(this, AppWidgetManager.getInstance(this), id)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
            finish()
        }, prefs.getInt("hydration_hour_$id", 0), prefs.getInt("hydration_minute_$id", 0), false).apply {
            setTitle("Choose daily clear time")
            setOnCancelListener { finish() }
            show()
        }
    }
}

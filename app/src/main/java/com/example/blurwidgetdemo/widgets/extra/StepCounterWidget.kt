package com.example.blurwidgetdemo.widgets.extra

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StepCounterWidget : NamedActionWidget() {
    override val title = "Steps"
    override val icon = R.drawable.running
    override fun action(context: Context, id: Int) = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_FITNESS)
    override fun layoutResource(context: Context, manager: AppWidgetManager, id: Int): Int {
        val options = manager.getAppWidgetOptions(id)
        return if (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40) > 100) R.layout.widget_steps_large
        else R.layout.widget_steps_compact
    }
    override fun displayText(context: Context, id: Int): String {
        val steps = stepCount(context, id)
        return NumberFormat.getIntegerInstance().format(steps)
    }
    override fun applyContent(views: RemoteViews, context: Context, id: Int) {
        val steps = stepCount(context, id)
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(id)
        if (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40) > 100) {
            views.setImageViewResource(R.id.steps_icon, icon)
            views.setTextViewText(R.id.steps_count, NumberFormat.getIntegerInstance().format(steps))
            views.setTextViewText(R.id.steps_goal, "/ $DAILY_GOAL steps")
        } else {
            views.setImageViewResource(R.id.steps_icon, icon)
            views.setTextViewText(R.id.steps_count, NumberFormat.getIntegerInstance().format(steps))
        }
    }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        if (context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) return
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val result = goAsync()
        sensorManager.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rawCounter = event.values.firstOrNull()?.toLong() ?: return
                saveTodayCounts(context, ids, rawCounter)
                sensorManager.unregisterListener(this)
                this@StepCounterWidget.refreshRendered(context, manager, ids)
                result.finish()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        super.onAppWidgetOptionsChanged(context, manager, id, options)
    }

    private fun stepCount(context: Context, id: Int) = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        .getLong("step_counter_$id", 0).coerceAtLeast(0)
    private fun refreshRendered(context: Context, manager: AppWidgetManager, ids: IntArray) = super.onUpdate(context, manager, ids)

    /** TYPE_STEP_COUNTER is cumulative since boot, so keep a per-widget baseline for each day. */
    private fun saveTodayCounts(context: Context, ids: IntArray, rawCounter: Long) {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        val today = dayKey()
        val bootedToday = System.currentTimeMillis() - SystemClock.elapsedRealtime() >= startOfToday()
        prefs.edit().apply {
            ids.forEach { id ->
                val existingDay = prefs.getString("step_day_$id", null)
                var baseline = prefs.getLong("step_baseline_$id", rawCounter)
                if (existingDay != today || rawCounter < baseline) {
                    // If Android restarted today, its raw count already is today's total. For an
                    // older boot we can only reliably count from the first reading of the day.
                    baseline = if (bootedToday) 0L else rawCounter
                    putString("step_day_$id", today)
                    putLong("step_baseline_$id", baseline)
                }
                putLong("step_counter_$id", (rawCounter - baseline).coerceAtLeast(0L))
            }
        }.apply()
    }

    private fun dayKey() = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    private fun startOfToday() = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object { const val DAILY_GOAL = 10_000 }
}

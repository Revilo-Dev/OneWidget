package com.example.blurwidgetdemo.widgets.extra

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R

class StepCounterWidget : NamedActionWidget() {
    override val title = "Steps"
    override val icon = R.drawable.running
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
                this@StepCounterWidget.refreshRendered(context, manager, ids)
                result.finish()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    private fun refreshRendered(context: Context, manager: AppWidgetManager, ids: IntArray) = super.onUpdate(context, manager, ids)
}

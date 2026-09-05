package com.example.blurwidgetdemo.widgets.hydration

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.RemoteViews
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.WidgetConfigActivity
import com.example.blurwidgetdemo.studio.universal.UniversalElementContent
import com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetActions
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetContent
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Daily water counter with a per-widget scheduled clear time (midnight by default). */
open class HydrationWidget : AppWidgetProvider() {
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
                .putInt("hydration_$id", (currentCount(prefs, id) + change).coerceAtLeast(0))
                .putString("hydration_slot_$id", resetSlot(prefs.getInt("hydration_hour_$id", 0), prefs.getInt("hydration_minute_$id", 0)))
                .apply()
        }
        updateWidget(context, AppWidgetManager.getInstance(context), id)
    }

    companion object {
        private const val ACTION_ADD_WATER = "com.example.blurwidgetdemo.action.ADD_WATER"
        private const val ACTION_REMOVE_WATER = "com.example.blurwidgetdemo.action.REMOVE_WATER"
        private const val ACTION_RESET = "com.example.blurwidgetdemo.action.RESET_HYDRATION"
        private const val DEFAULT_DAILY_GOAL = 8

        fun saveResetTime(context: Context, id: Int, hour: Int, minute: Int) {
            context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit()
                .putInt("hydration_hour_$id", hour).putInt("hydration_minute_$id", minute)
                .putString("hydration_slot_$id", resetSlot(hour, minute)).putInt("hydration_$id", 0).apply()
        }

        fun saveGoal(context: Context, id: Int, goal: Int) {
            context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit()
                .putInt("hydration_goal_$id", goal.coerceAtLeast(1))
                .putBoolean("hydration_show_goal_$id", true)
                .apply()
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val count = currentCount(prefs, id)
            val compact = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
            val goal = goal(prefs, id)
            val showGoal = prefs.getBoolean("hydration_show_goal_$id", true)
            val goalText = "of $goal glasses"
            val layout = if (compact) R.layout.widget_hydration_compact else R.layout.widget_hydration_large
            val minusAction = action(context, id, ACTION_REMOVE_WATER, 0)
            val plusAction = action(context, id, ACTION_ADD_WATER, 1)
            val rootAction = settingsAction(context, id)
            val promoted = UniversalProductionWidgetRenderer.buildIfPromoted(
                context,
                manager,
                id,
                UniversalWidgetIds.HYDRATION,
                UniversalWidgetContent(
                    mapOf(
                        "universal.icon" to UniversalElementContent(imageResId = R.drawable.ic_water_glass),
                        "universal.count" to UniversalElementContent(text = count.toString()),
                        "universal.goal" to UniversalElementContent(text = goalText, visible = showGoal),
                        "universal.minus" to UniversalElementContent(imageResId = R.drawable.minus),
                        "universal.plus" to UniversalElementContent(imageResId = R.drawable.plus)
                    )
                ),
                UniversalWidgetActions(
                    root = rootAction,
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
                setTextViewText(R.id.hydration_count, if (compact && showGoal) "$count/$goal" else count.toString())
                setTextViewText(R.id.hydration_goal, goalText)
                setViewVisibility(R.id.hydration_goal, if (!compact && showGoal) View.VISIBLE else View.GONE)
                setOnClickPendingIntent(R.id.hydration_minus, minusAction)
                setOnClickPendingIntent(R.id.hydration_plus, plusAction)
                setOnClickPendingIntent(android.R.id.background, rootAction)
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

        private fun goal(prefs: android.content.SharedPreferences, id: Int): Int =
            prefs.getInt("hydration_goal_$id", DEFAULT_DAILY_GOAL).coerceAtLeast(1)

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

        private fun settingsAction(context: Context, id: Int) = PendingIntent.getActivity(
            context,
            id,
            Intent(context, WidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
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
        showSettings()
    }

    private fun showSettings() {
        val prefs = getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE)
        val hour = prefs.getInt("hydration_hour_$id", 0)
        val minute = prefs.getInt("hydration_minute_$id", 0)
        val goal = prefs.getInt("hydration_goal_$id", 8).coerceAtLeast(1)
        val showGoal = prefs.getBoolean("hydration_show_goal_$id", true)
        AlertDialog.Builder(this)
            .setTitle("Hydration settings")
            .setItems(arrayOf(
                "Daily clear time · ${formatTime(hour, minute)}",
                "Daily goal · $goal glasses",
                "Show daily goal · ${if (showGoal) "On" else "Off"}",
                "Done"
            )) { _, which ->
                when (which) {
                    0 -> TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                        HydrationWidget.saveResetTime(this, id, selectedHour, selectedMinute)
                        updateWidget(); showSettings()
                    }, hour, minute, false).show()
                    1 -> editGoal(goal)
                    2 -> {
                        prefs.edit().putBoolean("hydration_show_goal_$id", !showGoal).apply()
                        updateWidget(); showSettings()
                    }
                    else -> finishConfiguration()
                }
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun editGoal(currentGoal: Int) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentGoal.toString())
            selectAll()
        }
        AlertDialog.Builder(this)
            .setTitle("Daily goal")
            .setView(input)
            .setNegativeButton(android.R.string.cancel) { _, _ -> showSettings() }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                HydrationWidget.saveGoal(this, id, input.text.toString().toIntOrNull() ?: currentGoal)
                updateWidget(); showSettings()
            }
            .show()
    }

    private fun updateWidget() = HydrationWidget.updateWidget(this, AppWidgetManager.getInstance(this), id)

    private fun finishConfiguration() {
        com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
            .initializeConfiguredWidget(this, com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds.HYDRATION, id)
        updateWidget()
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        finish()
    }

    private fun formatTime(hour: Int, minute: Int) = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(time)
    }
}

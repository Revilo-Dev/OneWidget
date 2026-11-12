package dev.revilodev.onewidget.widgets.extra

import android.app.PendingIntent
import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.WidgetConfigActivity
import dev.revilodev.onewidget.studio.universal.UniversalElementContent
import dev.revilodev.onewidget.studio.universal.UniversalProductionWidgetRenderer
import dev.revilodev.onewidget.studio.universal.UniversalWidgetActions
import dev.revilodev.onewidget.studio.universal.UniversalWidgetContent
import dev.revilodev.onewidget.studio.universal.UniversalWidgetIds
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** A plain plus/minus counter that clears at a user-selected local time each day. */
open class CounterWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { updateWidget(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateWidget(context, manager, id)

    override fun onDeleted(context: Context, ids: IntArray) {
        dev.revilodev.onewidget.widgets.WidgetAppearance.delete(context, ids)

        UniversalProductionWidgetRenderer.deleteModes(context, UniversalWidgetIds.COUNTER, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        when (intent.action) {
            ACTION_ADD -> change(context, id, 1)
            ACTION_REMOVE -> change(context, id, -1)
            ACTION_RESET -> Unit
            else -> return
        }
        updateWidget(context, AppWidgetManager.getInstance(context), id)
    }

    companion object {
        private const val ACTION_ADD = "dev.revilodev.onewidget.action.ADD_COUNTER"
        private const val ACTION_REMOVE = "dev.revilodev.onewidget.action.REMOVE_COUNTER"
        private const val ACTION_RESET = "dev.revilodev.onewidget.action.RESET_COUNTER"

        fun saveResetTime(context: Context, id: Int, hour: Int, minute: Int) {
            context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit()
                .putInt("counter_hour_$id", hour).putInt("counter_minute_$id", minute)
                .putString("counter_slot_$id", resetSlot(hour, minute)).putInt("counter_value_$id", 0).apply()
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val value = currentValue(prefs, id)
            val options = dev.revilodev.onewidget.widgets.WidgetInitialSizeSpec.renderOptions(manager, id)
            val compact = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) <= 100
            val singleCell = compact && options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180) <= 100
            val reset = "Clears ${formatReset(prefs.getInt("counter_hour_$id", 0), prefs.getInt("counter_minute_$id", 0))}"
            val showClearText = prefs.getBoolean("counter_show_clear_$id", true)
            val minusAction = action(context, id, ACTION_REMOVE, 0)
            val plusAction = action(context, id, ACTION_ADD, 1)
            val rootAction = PendingIntent.getActivity(
                context,
                id,
                Intent(context, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val promoted = UniversalProductionWidgetRenderer.buildIfPromoted(
                context,
                manager,
                id,
                UniversalWidgetIds.COUNTER,
                UniversalWidgetContent(
                    mapOf(
                        "universal.minus" to UniversalElementContent(imageResId = R.drawable.minus),
                        "universal.value" to UniversalElementContent(text = value.toString()),
                        "universal.plus" to UniversalElementContent(imageResId = R.drawable.plus),
                        "universal.reset" to UniversalElementContent(text = reset, visible = showClearText)
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
            val views = promoted ?: RemoteViews(
                context.packageName,
                when {
                    singleCell -> R.layout.widget_counter_square
                    compact -> R.layout.widget_counter_compact
                    else -> R.layout.widget_counter_large
                }
            ).apply {
                BlurWidget.applySurfaceTint(this, prefs, id)
                setTextViewText(R.id.counter_value, value.toString())
                setTextViewText(R.id.counter_reset, reset)
                setViewVisibility(R.id.counter_reset, if (!compact && showClearText) View.VISIBLE else View.GONE)
                setImageViewResource(R.id.counter_minus, R.drawable.minus)
                setImageViewResource(R.id.counter_plus, R.drawable.plus)
                setOnClickPendingIntent(R.id.counter_minus, minusAction)
                setOnClickPendingIntent(R.id.counter_plus, plusAction)
                setOnClickPendingIntent(android.R.id.background, rootAction)
            }
            dev.revilodev.onewidget.widgets.WidgetSurface.update(context, manager, id, views)
            scheduleReset(context, id)
        }

        private fun change(context: Context, id: Int, delta: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val value = currentValue(prefs, id)
            prefs.edit().putInt("counter_value_$id", (value + delta).coerceAtLeast(0)).apply()
        }

        private fun currentValue(prefs: android.content.SharedPreferences, id: Int): Int {
            val hour = prefs.getInt("counter_hour_$id", 0)
            val minute = prefs.getInt("counter_minute_$id", 0)
            val slot = resetSlot(hour, minute)
            return if (prefs.getString("counter_slot_$id", null) == slot) prefs.getInt("counter_value_$id", 0)
            else {
                prefs.edit().putString("counter_slot_$id", slot).putInt("counter_value_$id", 0).apply()
                0
            }
        }

        private fun action(context: Context, id: Int, action: String, offset: Int) = PendingIntent.getBroadcast(context, id * 10 + offset,
            Intent(context, CounterWidget::class.java).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        private fun scheduleReset(context: Context, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val reset = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, prefs.getInt("counter_hour_$id", 0))
                set(Calendar.MINUTE, prefs.getInt("counter_minute_$id", 0))
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reset.timeInMillis,
                action(context, id, ACTION_RESET, 2)
            )
        }

        private fun resetSlot(hour: Int, minute: Int): String {
            val now = Calendar.getInstance()
            val reset = now.clone() as Calendar
            reset.set(Calendar.HOUR_OF_DAY, hour); reset.set(Calendar.MINUTE, minute); reset.set(Calendar.SECOND, 0); reset.set(Calendar.MILLISECOND, 0)
            if (now.before(reset)) reset.add(Calendar.DAY_OF_YEAR, -1)
            return SimpleDateFormat("yyyyMMdd", Locale.US).format(reset.time)
        }

        private fun formatReset(hour: Int, minute: Int) = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(time)
        }
    }
}


class CounterConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, WidgetConfigActivity::class.java).putExtras(intent).addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT))
        finish()
    }
}

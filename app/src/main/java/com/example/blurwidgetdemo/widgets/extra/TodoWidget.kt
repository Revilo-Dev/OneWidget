package dev.revilodev.onewidget.widgets.extra

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.appcompat.app.AppCompatActivity
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.WidgetConfigActivity
import dev.revilodev.onewidget.widgets.WidgetAppearance
import dev.revilodev.onewidget.widgets.WidgetSurface
import java.util.Calendar

data class TodoItem(val text: String, val complete: Boolean)

open class TodoWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { update(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = update(context, manager, id)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val manager = AppWidgetManager.getInstance(context)
        val provider = manager.getAppWidgetInfo(id)?.provider?.className ?: return
        if (!dev.revilodev.onewidget.widgets.variants.ExactWidgetVariants.isProviderFor(provider, TodoWidget::class.java)) return
        val index = intent.getIntExtra(EXTRA_INDEX, -1)
        when (intent.action) {
            ACTION_TOGGLE -> save(context, id, items(context, id).mapIndexed { position, item -> if (position == index) item.copy(complete = !item.complete) else item })
            ACTION_DELETE -> save(context, id, items(context, id).filterIndexed { position, _ -> position != index })
            ACTION_CLEAR -> save(context, id, emptyList())
            else -> return
        }
        update(context, manager, id)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { context.getSystemService(AlarmManager::class.java).cancel(action(context, it, ACTION_CLEAR, 9)) }
        WidgetAppearance.delete(context, ids)
    }

    companion object {
        const val ACTION_TOGGLE = "dev.revilodev.onewidget.action.TODO_TOGGLE"
        const val ACTION_DELETE = "dev.revilodev.onewidget.action.TODO_DELETE"
        const val ACTION_CLEAR = "dev.revilodev.onewidget.action.TODO_CLEAR"
        const val EXTRA_INDEX = "todo_index"

        fun items(context: Context, id: Int): List<TodoItem> = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            .getString("todo_items_" + id, "").orEmpty().lineSequence().mapNotNull { raw ->
                raw.split("\t", limit = 2).takeIf { it.size == 2 }?.let { TodoItem(it[1], it[0] == "1") }
            }.toList()

        fun save(context: Context, id: Int, values: List<TodoItem>) {
            val serialized = values.joinToString("\n") { (if (it.complete) "1" else "0") + "\t" + it.text.replace('\t', ' ').replace('\n', ' ') }
            context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit().putString("todo_items_" + id, serialized).apply()
        }

        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val hour = prefs.getInt("todo_clear_hour_" + id, -1)
            val minute = prefs.getInt("todo_clear_minute_" + id, 0)
            if (hour >= 0) {
                val current = slot(hour, minute)
                val previous = prefs.getString("todo_clear_slot_" + id, null)
                if (previous != null && previous != current) save(context, id, emptyList())
                if (previous != current) prefs.edit().putString("todo_clear_slot_" + id, current).apply()
            } else prefs.edit().remove("todo_clear_slot_" + id).apply()
            val views = RemoteViews(context.packageName, R.layout.widget_todo).apply {
                val color = WidgetAppearance.textColor(prefs, id)
                val scale = WidgetAppearance.textScale(prefs, id) / 100f
                setTextColor(R.id.todo_title, color)
                setTextColor(R.id.todo_empty, color)
                setFloat(R.id.todo_title, "setTextSize", 18f * scale)
                setFloat(R.id.todo_empty, "setTextSize", 14f * scale)
                setInt(R.id.todo_empty, "setGravity", WidgetAppearance.alignment(prefs, id).gravity)
                setOnClickPendingIntent(R.id.todo_add, PendingIntent.getActivity(context, id,
                    Intent(context, WidgetConfigActivity::class.java).setData(Uri.parse("todo://" + id + "/configure"))
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                setPendingIntentTemplate(R.id.todo_list, action(context, id, ACTION_TOGGLE, -1))
                setEmptyView(R.id.todo_list, R.id.todo_empty)
            }
            if (Build.VERSION.SDK_INT >= 31) {
                val collection = RemoteViews.RemoteCollectionItems.Builder().setViewTypeCount(1).setHasStableIds(false)
                items(context, id).forEachIndexed { index, item -> collection.addItem(index.toLong(), row(context, id, index, item)) }
                views.setRemoteAdapter(R.id.todo_list, collection.build())
            } else legacyAdapter(context, manager, id, views)
            WidgetSurface.update(context, manager, id, views)
            scheduleClear(context, id, hour, minute)
        }

        @Suppress("DEPRECATION")
        private fun legacyAdapter(context: Context, manager: AppWidgetManager, id: Int, views: RemoteViews) {
            views.setRemoteAdapter(R.id.todo_list, Intent(context, TodoRemoteViewsService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).setData(Uri.parse("todo://" + id + "/items")))
            manager.notifyAppWidgetViewDataChanged(id, R.id.todo_list)
        }

        fun row(context: Context, id: Int, index: Int, item: TodoItem): RemoteViews {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            return RemoteViews(context.packageName, R.layout.widget_todo_item).apply {
                setTextViewText(R.id.todo_item_text, item.text)
                setTextColor(R.id.todo_item_text, WidgetAppearance.textColor(prefs, id))
                setFloat(R.id.todo_item_text, "setTextSize", 16f * WidgetAppearance.textScale(prefs, id) / 100f)
                setImageViewResource(R.id.todo_item_check, if (item.complete) R.drawable.checkbox_checked else R.drawable.checkbox_unchecked)
                setOnClickFillInIntent(R.id.todo_item_row, Intent().putExtra(EXTRA_INDEX, index).setAction(ACTION_TOGGLE))
                setOnClickFillInIntent(R.id.todo_item_delete, Intent().putExtra(EXTRA_INDEX, index).setAction(ACTION_DELETE))
            }
        }

        private fun scheduleClear(context: Context, id: Int, hour: Int, minute: Int) {
            val alarm = context.getSystemService(AlarmManager::class.java)
            val pending = action(context, id, ACTION_CLEAR, 9)
            if (hour < 0) { alarm.cancel(pending); return }
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23)); set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            alarm.set(AlarmManager.RTC, next.timeInMillis, pending)
        }

        private fun action(context: Context, id: Int, action: String, index: Int): PendingIntent {
            val template = index == -1
            val mutable = if (template && Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE
                else if (template) 0 else PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getBroadcast(context, id, Intent(context, TodoWidget::class.java)
                .setAction(if (template) null else action).setData(Uri.parse("todo://" + id + "/" + action + "/" + index))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra(EXTRA_INDEX, index), PendingIntent.FLAG_UPDATE_CURRENT or mutable)
        }

        private fun slot(hour: Int, minute: Int): String {
            val now = Calendar.getInstance()
            val reset = now.clone() as Calendar
            reset.set(Calendar.HOUR_OF_DAY, hour); reset.set(Calendar.MINUTE, minute)
            reset.set(Calendar.SECOND, 0); reset.set(Calendar.MILLISECOND, 0)
            if (now.before(reset)) reset.add(Calendar.DAY_OF_YEAR, -1)
            return reset.get(Calendar.YEAR).toString() + "-" + reset.get(Calendar.DAY_OF_YEAR)
        }
    }
}

class TodoRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = Factory(applicationContext, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))

    class Factory(private val context: Context, private val id: Int) : RemoteViewsFactory {
        private var values = emptyList<TodoItem>()
        override fun onCreate() = Unit
        override fun onDataSetChanged() { values = TodoWidget.items(context, id) }
        override fun onDestroy() = Unit
        override fun getCount() = values.size
        override fun getViewAt(position: Int) = values.getOrNull(position)?.let { TodoWidget.row(context, id, position, it) }
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = false
    }
}

class TodoWidgetConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, WidgetConfigActivity::class.java).putExtras(intent).addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT))
        finish()
    }
}

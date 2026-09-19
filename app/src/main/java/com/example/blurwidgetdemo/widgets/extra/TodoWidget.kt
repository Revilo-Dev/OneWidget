package dev.revilodev.onewidget.widgets.extra

import android.app.PendingIntent
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.RemoteViewsService
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.WidgetConfigActivity
import java.util.Calendar

data class TodoItem(val text: String, val complete: Boolean)

open class TodoWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { update(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = update(context, manager, id)
    override fun onReceive(context: Context, intent: Intent) { super.onReceive(context, intent); val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1); if (id < 0) return; when (intent.action) { ACTION_TOGGLE -> mutate(context, id) { list -> list.mapIndexed { index, item -> if (index == intent.getIntExtra(EXTRA_INDEX, -1)) item.copy(complete = !item.complete) else item } }; ACTION_DELETE -> mutate(context, id) { it.filterIndexed { index, _ -> index != intent.getIntExtra(EXTRA_INDEX, -1) } }; ACTION_CLEAR -> save(context, id, emptyList()); else -> return }; update(context, AppWidgetManager.getInstance(context), id) }
    companion object {
        const val ACTION_TOGGLE = "dev.revilodev.onewidget.action.TODO_TOGGLE"; const val ACTION_DELETE = "dev.revilodev.onewidget.action.TODO_DELETE"; const val ACTION_CLEAR = "dev.revilodev.onewidget.action.TODO_CLEAR"; const val EXTRA_INDEX = "todo_index"
        fun items(context: Context, id: Int): List<TodoItem> = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getString("todo_items_$id", "").orEmpty().lineSequence().mapNotNull { raw -> raw.split("\t", limit = 2).takeIf { it.size == 2 }?.let { TodoItem(it[1], it[0] == "1") } }.toList()
        fun save(context: Context, id: Int, values: List<TodoItem>) { context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit().putString("todo_items_$id", values.joinToString("\n") { "${if (it.complete) 1 else 0}\t${it.text.replace('\t', ' ').replace('\n', ' ')}" }).apply() }
        fun add(context: Context, id: Int, text: String) { if (text.isNotBlank()) save(context, id, items(context, id) + TodoItem(text.trim(), false)); update(context, AppWidgetManager.getInstance(context), id) }
        private fun mutate(context: Context, id: Int, block: (List<TodoItem>) -> List<TodoItem>) = save(context, id, block(items(context, id)))
        fun update(context: Context, manager: AppWidgetManager, id: Int) { val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE); val clearHour = prefs.getInt("todo_clear_hour_$id", -1); val clearMinute = prefs.getInt("todo_clear_minute_$id", 0); if (clearHour >= 0 && prefs.getString("todo_clear_slot_$id", "") != slot(clearHour, clearMinute)) { save(context, id, emptyList()); prefs.edit().putString("todo_clear_slot_$id", slot(clearHour, clearMinute)).apply() }; val dataIntent = Intent(context, TodoRemoteViewsService::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).apply { data = Uri.parse("todo://$id/items") }; val views = RemoteViews(context.packageName, R.layout.widget_todo).apply { BlurWidget.applySurfaceTint(this, prefs, id); setOnClickPendingIntent(R.id.todo_add, configAction(context,id)); setRemoteAdapter(R.id.todo_list, dataIntent); setPendingIntentTemplate(R.id.todo_list, action(context,id,ACTION_TOGGLE,-1)); setEmptyView(R.id.todo_list, R.id.todo_empty) }; manager.updateAppWidget(id, views); manager.notifyAppWidgetViewDataChanged(id, R.id.todo_list); scheduleClear(context,id,clearHour,clearMinute) }
        private fun scheduleClear(context: Context,id: Int,hour: Int,minute: Int) { if(hour < 0) return; val next=Calendar.getInstance().apply{set(Calendar.HOUR_OF_DAY,hour);set(Calendar.MINUTE,minute);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0);if(timeInMillis<=System.currentTimeMillis())add(Calendar.DAY_OF_YEAR,1)}; context.getSystemService(android.app.AlarmManager::class.java).setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP,next.timeInMillis,action(context,id,ACTION_CLEAR,9)) }
        private fun action(context: Context,id:Int,action:String,index:Int)=PendingIntent.getBroadcast(context,(id * 100) + (index * 10) + action.hashCode().and(7),Intent(context,TodoWidget::class.java).setAction(action).setData(Uri.parse("todo://$id/$action/$index")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra(EXTRA_INDEX,index),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        private fun configAction(context: Context,id:Int)=PendingIntent.getActivity(context,id,Intent(context,WidgetConfigActivity::class.java).setData(Uri.parse("todo://$id/configure")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        private fun slot(hour:Int,minute:Int):String { val now=Calendar.getInstance(); val reset=now.clone() as Calendar; reset.set(Calendar.HOUR_OF_DAY,hour); reset.set(Calendar.MINUTE,minute); if(now.before(reset)) reset.add(Calendar.DAY_OF_YEAR,-1); return "${reset.get(Calendar.YEAR)}-${reset.get(Calendar.DAY_OF_YEAR)}" }
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
        override fun getViewAt(position: Int): RemoteViews? = values.getOrNull(position)?.let { item -> RemoteViews(context.packageName, R.layout.widget_todo_item).apply { setTextViewText(R.id.todo_item_text, item.text); setImageViewResource(R.id.todo_item_check, if (item.complete) R.drawable.checkbox_checked else R.drawable.checkbox_unchecked); setOnClickFillInIntent(R.id.todo_item_row, Intent().putExtra(TodoWidget.EXTRA_INDEX, position).setAction(TodoWidget.ACTION_TOGGLE)); setOnClickFillInIntent(R.id.todo_item_delete, Intent().putExtra(TodoWidget.EXTRA_INDEX, position).setAction(TodoWidget.ACTION_DELETE)) } }
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = true
    }
}

class TodoWidgetConfigActivity : AppCompatActivity() {
    private var id = -1

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setResult(RESULT_CANCELED)
        id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (id < 0) { finish(); return }
        setContentView(R.layout.activity_todo_widget_config)
        val input = findViewById<EditText>(R.id.todo_editor_input)
        findViewById<Button>(R.id.todo_editor_add).setOnClickListener {
            TodoWidget.add(this, id, input.text.toString())
            input.text?.clear()
            refreshItems()
            input.requestFocus()
        }
        findViewById<View>(R.id.todo_editor_settings).setOnClickListener { showSettings() }
        findViewById<Button>(R.id.todo_editor_done).setOnClickListener { finishConfiguration() }
        refreshItems()
        input.requestFocus()
    }

    private fun refreshItems() {
        val summary = TodoWidget.items(this, id).joinToString("\n") { item ->
            "${if (item.complete) "✓" else "○"}  ${item.text}"
        }.ifBlank { "Your to-dos will appear here." }
        findViewById<TextView>(R.id.todo_editor_items).text = summary
    }

    private fun showSettings() {
        val prefs = getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE)
        val hour = prefs.getInt("todo_clear_hour_$id", -1)
        val schedule = if (hour < 0) "Clear automatically · Off" else "Clear automatically · ${String.format("%d:%02d", hour, prefs.getInt("todo_clear_minute_$id", 0))}"
        AlertDialog.Builder(this)
            .setTitle("To do settings")
            .setItems(arrayOf(schedule, "Clear list now")) { _, which ->
                when (which) {
                    0 -> if (hour < 0) TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                        prefs.edit().putInt("todo_clear_hour_$id", selectedHour).putInt("todo_clear_minute_$id", selectedMinute).apply()
                        TodoWidget.update(this, AppWidgetManager.getInstance(this), id)
                    }, 9, 0, false).show() else {
                        prefs.edit().putInt("todo_clear_hour_$id", -1).apply()
                        TodoWidget.update(this, AppWidgetManager.getInstance(this), id)
                    }
                    1 -> {
                        TodoWidget.save(this, id, emptyList())
                        TodoWidget.update(this, AppWidgetManager.getInstance(this), id)
                        refreshItems()
                    }
                }
            }
            .show()
    }

    private fun finishConfiguration() {
        TodoWidget.update(this, AppWidgetManager.getInstance(this), id)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        finish()
    }
}

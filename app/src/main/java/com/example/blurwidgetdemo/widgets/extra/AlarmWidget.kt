package com.example.blurwidgetdemo.widgets.extra
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.example.blurwidgetdemo.R
import java.text.DateFormat
import java.util.Date
class AlarmWidget : NamedActionWidget() { override val title = "Alarm"; override val icon = R.drawable.time; override fun action(context: Context, id: Int) = Intent(AlarmClock.ACTION_SHOW_ALARMS); override fun displayText(context: Context, id: Int): String { val next = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime; return if (next == null) "No alarm set" else "Alarm ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(next))}" } }

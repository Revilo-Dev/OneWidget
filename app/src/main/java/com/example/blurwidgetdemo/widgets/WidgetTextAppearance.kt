package dev.revilodev.onewidget.widgets

import android.content.Context
import android.util.TypedValue
import android.util.Xml
import android.widget.RemoteViews
import dev.revilodev.onewidget.BlurWidget
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

object WidgetTextAppearance {
    private data class Label(val id: Int, val sizeSp: Float)
    private val labels = ConcurrentHashMap<Int, List<Label>>()
    private val styledRenderers = listOf("widget_studio_", "widget_digital_clock", "widget_battery", "widget_storage", "widget_calendar_compact", "widget_calendar_large")

    fun apply(context: Context, id: Int, views: RemoteViews) {
        val layout = views.layoutId
        val name = context.resources.getResourceEntryName(layout)
        if (styledRenderers.any(name::startsWith)) return
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        val color = WidgetAppearance.textColor(prefs, id)
        val customColor = prefs.contains("text_color_" + id)
        val scale = WidgetAppearance.textScale(prefs, id) / 100f
        val alignment = WidgetAppearance.alignment(prefs, id).gravity
        labels.getOrPut(layout) { readLabels(context, layout) }.forEach { label ->
            if (customColor) views.setTextColor(label.id, color)
            views.setTextViewTextSize(label.id, TypedValue.COMPLEX_UNIT_SP, label.sizeSp * scale)
            views.setInt(label.id, "setGravity", alignment)
        }
    }

    private fun readLabels(context: Context, layout: Int): List<Label> {
        val result = mutableListOf<Label>()
        val namespace = "http://schemas.android.com/apk/res/android"
        context.resources.getLayout(layout).use { parser ->
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG || parser.name !in setOf("TextView", "TextClock", "Chronometer", "Button")) continue
                val id = parser.getAttributeResourceValue(namespace, "id", 0)
                if (id == 0) continue
                val values = context.obtainStyledAttributes(Xml.asAttributeSet(parser), intArrayOf(android.R.attr.textSize))
                val sizeSp = try {
                    val value = values.peekValue(0)
                    if (value?.type == TypedValue.TYPE_DIMENSION && value.data and TypedValue.COMPLEX_UNIT_MASK == TypedValue.COMPLEX_UNIT_SP) {
                        TypedValue.complexToFloat(value.data)
                    } else values.getDimension(0, 14f * context.resources.displayMetrics.scaledDensity) / context.resources.displayMetrics.scaledDensity
                } finally { values.recycle() }
                result += Label(id, sizeSp)
            }
        }
        return result
    }
}

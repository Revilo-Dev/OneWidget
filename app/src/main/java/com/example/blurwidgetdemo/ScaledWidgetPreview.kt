package com.example.blurwidgetdemo

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.example.blurwidgetdemo.widgets.WidgetProgressBarRenderer
import com.example.blurwidgetdemo.widgets.extra.ToggleOverlayRenderer
import kotlin.math.min

/**
 * Hosts the actual widget XML and scales it uniformly for the in-app picker.
 */
class ScaledWidgetPreview(
    context: Context,
    @LayoutRes layout: Int,
    columns: Int,
    rows: Int,
    title: String,
    icon: Int
) : FrameLayout(context) {
    // A preview cell maps directly to one grid cell.  Gaps are supplied by the picker layout,
    // rather than baked into one axis of a widget, which keeps every n×m preview proportional.
    private val sourceWidth = dp(columns * 84).coerceAtLeast(dp(72))
    private val sourceHeight = dp(rows * 84).coerceAtLeast(dp(72))
    private val surface = FrameLayout(context)
    private var contentScale = 1f
    private val preview = LayoutInflater.from(context).inflate(layout, surface, false).also { view ->
        // The app picker intentionally has no extra blurred or rounded surface over the widget.
        view.setBackgroundColor(Color.TRANSPARENT)
        view.foreground = null
        bindBaseContent(view, title, icon)
        surface.addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    init {
        minimumHeight = dp(72)
        contentDescription = context.getString(R.string.widget_preview)
        preview.isClickable = false
        preview.isFocusable = false
        preview.findViewById<View>(android.R.id.background)?.isClickable = false
        surface.addView(ImageView(context).apply {
            setImageResource(previewSurface(columns, rows))
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = null
        }, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(surface)
        setPadding(0, dp(4), 0, dp(4))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(1)
        contentScale = min(1f, available.toFloat() / sourceWidth)
        surface.measure(
            MeasureSpec.makeMeasureSpec(sourceWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(sourceHeight, MeasureSpec.EXACTLY)
        )
        surface.scaleX = contentScale
        surface.scaleY = contentScale
        setMeasuredDimension(
            resolveSize((sourceWidth * contentScale).toInt() + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize((sourceHeight * contentScale).toInt() + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val scaledWidth = (sourceWidth * contentScale).toInt()
        val horizontalSpace = (width - paddingLeft - paddingRight - scaledWidth).coerceAtLeast(0)
        val surfaceLeft = paddingLeft + horizontalSpace / 2
        surface.layout(surfaceLeft, paddingTop, surfaceLeft + sourceWidth, paddingTop + sourceHeight)
        surface.pivotX = 0f
        surface.pivotY = 0f
    }

    /** Supplies the same baseline content the providers put into otherwise-empty RemoteViews XML. */
    private fun bindBaseContent(root: View, title: String, icon: Int) {
        root.findViewById<TextView>(R.id.action_label)?.text = if (title == "Step counter") "6,524" else title
        root.findViewById<ImageView>(R.id.action_icon)?.setImageResource(icon)
        root.findViewById<ImageView>(R.id.toggle_icon)?.setImageResource(icon)
        root.findViewById<ImageView>(R.id.toggle_circle)?.setImageBitmap(
            ToggleOverlayRenderer.renderFill(168, 84, TOGGLE_ACCENT)
        )

        val (primary, secondary) = when (title) {
            "Next event" -> "Team catch-up" to "Today • 10:00 AM"
            "Storage" -> "Storage 62%" to "78.3 GB of 128 GB used"
            "Hydration" -> "4 glasses" to "Daily goal: 8 glasses"
            "Step counter" -> "6,524" to "Today"
            "Alarm" -> "7:30 AM" to "Weekdays"
            "Media player" -> "Now playing" to "Choose music"
            else -> null to null
        }
        root.findViewById<TextView>(R.id.widget_primary)?.text = primary
        root.findViewById<TextView>(R.id.widget_secondary)?.text = secondary
        root.findViewById<ImageView>(R.id.steps_icon)?.setImageResource(R.drawable.running)
        root.findViewById<TextView>(R.id.steps_count)?.text = "6,524"
        root.findViewById<TextView>(R.id.steps_goal)?.text = "/ 10,000 steps"

        root.findViewById<ImageView>(R.id.hydration_icon)?.setImageResource(R.drawable.ic_water_glass)
        root.findViewById<TextView>(R.id.hydration_count)?.text = "4"
        root.findViewById<TextView>(R.id.hydration_goal)?.text = "of 8 glasses"
        root.findViewById<ImageView>(R.id.hydration_minus)?.setImageResource(R.drawable.minus)
        root.findViewById<ImageView>(R.id.hydration_plus)?.setImageResource(R.drawable.plus)
        root.findViewById<TextView>(R.id.counter_value)?.text = "6"
        root.findViewById<TextView>(R.id.counter_reset)?.text = "Clears 8:00 PM"
        root.findViewById<ImageView>(R.id.counter_minus)?.setImageResource(R.drawable.minus)
        root.findViewById<ImageView>(R.id.counter_plus)?.setImageResource(R.drawable.plus)

        root.findViewById<TextView>(R.id.media_title)?.text = "Midnight City"
        root.findViewById<TextView>(R.id.media_artist)?.text = "M83"
        root.findViewById<ImageView>(R.id.media_art)?.setImageResource(R.drawable.music_alt)
        root.findViewById<ImageView>(R.id.media_idle_icon)?.visibility = View.GONE
        root.findViewById<ImageView>(R.id.media_previous)?.setImageResource(R.drawable.control_previous)
        root.findViewById<ImageView>(R.id.media_play_pause)?.setImageResource(R.drawable.control_pause)
        root.findViewById<ImageView>(R.id.media_next)?.setImageResource(R.drawable.control_next)
        // A preview has no live media session, so don't present a misleading fixed timestamp.
        root.findViewById<TextView>(R.id.media_time)?.visibility = View.GONE
        root.findViewById<ImageView>(R.id.media_progress)?.setImageBitmap(
            WidgetProgressBarRenderer.renderMedia(54, 0xFF8E99FF.toInt())
        )

        root.findViewById<ImageView>(R.id.alarm_toggle)?.setImageResource(R.drawable.category_clock)
        root.findViewById<TextView>(R.id.alarm_time)?.text = "7:30 AM"
        root.findViewById<TextView>(R.id.alarm_status)?.text = "Weekdays • On"
        root.findViewById<ImageView>(R.id.alarm_snooze_icon)?.setImageResource(R.drawable.alarm_snooze)
        root.findViewById<TextView>(R.id.alarm_snooze_label)?.text = "Snooze for today"

        root.findViewById<TextView>(R.id.battery_percentage)?.text = "85%"
        root.findViewById<ImageView>(R.id.battery_icon)?.setImageResource(R.drawable.phone)
        listOf(R.id.battery_label_1, R.id.battery_label_2, R.id.battery_label_3, R.id.battery_label_4)
            .forEachIndexed { index, id -> root.findViewById<TextView>(id)?.text = when (index) {
                0 -> "Phone  85%"; 1 -> "Buds  72%"; else -> ""
            } }
        listOf(R.id.battery_compact_bar, R.id.battery_bar_1, R.id.battery_bar_2, R.id.battery_bar_3, R.id.battery_bar_4)
            .forEachIndexed { index, id -> root.findViewById<ImageView>(id)?.setImageBitmap(
                WidgetProgressBarRenderer.render(if (index == 1) 72 else 85,
                    intArrayOf(0xFF3BCE8E.toInt(), 0xFF87FA6A.toInt(), 0xFF3ACD8C.toInt()))
            ) }

        root.findViewById<TextView>(R.id.weather_temperature)?.text = "24°"
        root.findViewById<TextView>(R.id.weather_condition)?.text = "Sunny"
        root.findViewById<TextView>(R.id.weather_location)?.text = "Current location"
        root.findViewById<ImageView>(R.id.weather_icon)?.setImageResource(R.drawable.weather_sunny)

        root.findViewById<TextView>(R.id.storage_label)?.text = "Storage  62%"
        root.findViewById<TextView>(R.id.memory_label)?.text = "Memory  41%"
        root.findViewById<TextView>(R.id.data_label)?.text = "Data  28%"
        listOf(R.id.compact_progress, R.id.storage_bar, R.id.memory_bar, R.id.data_bar)
            .forEachIndexed { index, id -> root.findViewById<ImageView>(id)?.setImageBitmap(
                WidgetProgressBarRenderer.render(listOf(62, 62, 41, 28)[index],
                    intArrayOf(0xFF63D2A9.toInt(), 0xFF73DDB2.toInt()))
            ) }

        root.findViewById<TextView>(R.id.month_title)?.text = "August 2026"
        val calendarCells = intArrayOf(
            R.id.day_0, R.id.day_1, R.id.day_2, R.id.day_3, R.id.day_4, R.id.day_5, R.id.day_6,
            R.id.day_7, R.id.day_8, R.id.day_9, R.id.day_10, R.id.day_11, R.id.day_12, R.id.day_13,
            R.id.day_14, R.id.day_15, R.id.day_16, R.id.day_17, R.id.day_18, R.id.day_19, R.id.day_20
        )
        calendarCells.forEachIndexed { index, id ->
            root.findViewById<TextView>(id)?.text = "${listOf("M", "T", "W", "T", "F", "S", "S")[index % 7]}\n${10 + index}"
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun previewSurface(columns: Int, rows: Int) = when {
        columns == 1 && rows == 1 -> R.drawable._2x2_widget_large_dark
        rows == 1 && columns <= 2 -> R.drawable._2x1_widget_large_dark
        rows == 1 -> R.drawable._4x1_widget_large_dark
        columns <= 2 -> R.drawable._2x2_widget_large_dark
        else -> R.drawable._4x2_widget_large_dark
    }

    private companion object {
        const val TOGGLE_ACCENT = 0xFF387AFF.toInt()
    }
}

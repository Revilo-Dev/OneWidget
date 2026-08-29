package com.example.blurwidgetdemo

import android.Manifest
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.blurwidgetdemo.widgets.battery.BatteryWidget
import com.example.blurwidgetdemo.widgets.calendar.CalendarDisplayWidget
import com.example.blurwidgetdemo.widgets.calendar.CalendarWidget
import com.example.blurwidgetdemo.widgets.clock.DigitalClockWidget
import com.example.blurwidgetdemo.widgets.extra.AlarmWidget
import com.example.blurwidgetdemo.widgets.extra.CounterWidget
import com.example.blurwidgetdemo.widgets.extra.GoogleSearchWidget
import com.example.blurwidgetdemo.widgets.extra.MediaPlayerWidget
import com.example.blurwidgetdemo.widgets.extra.OpenAppWidget
import com.example.blurwidgetdemo.widgets.extra.StepCounterWidget
import com.example.blurwidgetdemo.widgets.extra.TextWidget
import com.example.blurwidgetdemo.widgets.extra.SamsungBrowserSearchWidget
import com.example.blurwidgetdemo.widgets.extra.ToggleKind
import com.example.blurwidgetdemo.widgets.extra.ToggleWidget
import com.example.blurwidgetdemo.widgets.extra.YouTubeSearchWidget
import com.example.blurwidgetdemo.widgets.hydration.HydrationWidget
import com.example.blurwidgetdemo.widgets.storage.StorageWidget
import com.example.blurwidgetdemo.widgets.weather.WeatherWidget
import dev.oneuiproject.oneui.widget.RoundedLinearLayout

/** A launcher-like list that previews the same layouts supplied to the widget host. */
class MainActivity : AppCompatActivity() {
    private var pendingProvider: Class<out AppWidgetProvider>? = null
    private var pendingSize: WidgetSize? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addWidgetPicker()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_settings -> {
            startActivity(Intent(this, AppearanceSettingsActivity::class.java))
            true
        }
        R.id.menu_about -> {
            startActivity(Intent(this, AboutActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun addWidgetPicker() {
        val container = findViewById<LinearLayout>(R.id.widget_picker_list)
        pickerCategories().forEachIndexed { index, category ->
            container.addView(categoryCard(category), LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { if (index > 0) topMargin = dp(10) })
        }
    }

    private fun pickerCategories() = listOf(
        PickerCategory("Glass", R.drawable.category_blur, listOf(widget(
            "Glass", R.drawable.ic_home_24, BlurWidget::class.java,
            sizes("1 × 1" to R.layout.widget_preview_square, "2 × 1" to R.layout.widget_preview_strip,
                "2 × 2" to R.layout.widget_preview_square, "4 × 2" to R.layout.widget_preview_large,
                "4 × 4" to R.layout.widget_preview_large, "4 × 5" to R.layout.widget_preview_large)
        ))),
        PickerCategory("Clock", R.drawable.category_clock, listOf(
            widget(
            "Digital clock", R.drawable.time, DigitalClockWidget::class.java,
            sizes("1 × 1" to R.layout.widget_digital_clock_compact_clock_stamp,
                "2 × 1" to R.layout.widget_digital_clock_compact_clock_stamp,
                "3 × 2" to R.layout.widget_digital_clock_medium_clock_stamp,
                "4 × 2" to R.layout.widget_digital_clock_large_clock_stamp,
                "4 × 4" to R.layout.widget_digital_clock_large_clock_stamp)
            ),
            widget("Alarm", R.drawable.category_clock, AlarmWidget::class.java,
                sizes("2 × 1" to R.layout.widget_alarm_compact, "3 × 2" to R.layout.widget_alarm_large,
                    "4 × 2" to R.layout.widget_alarm_large))
        )),
        PickerCategory("Battery", R.drawable.category_battery, listOf(widget(
            "Battery", R.drawable.battery, BatteryWidget::class.java,
            sizes("1 × 1" to R.layout.widget_battery_compact, "2 × 1" to R.layout.widget_battery_compact,
                "3 × 1" to R.layout.widget_battery_compact, "4 × 1" to R.layout.widget_battery_compact,
                "2 × 2" to R.layout.widget_battery_large, "4 × 2" to R.layout.widget_battery_large)
        ))),
        PickerCategory("Weather", R.drawable.category_weather, listOf(widget(
            "Weather", R.drawable.weather_sunny, WeatherWidget::class.java,
            sizes("1 × 1" to R.layout.widget_weather_compact, "2 × 1" to R.layout.widget_weather_compact,
                "3 × 1" to R.layout.widget_weather_compact, "4 × 1" to R.layout.widget_weather_compact,
                "2 × 2" to R.layout.widget_weather_large, "4 × 2" to R.layout.widget_weather_large)
        ))),
        PickerCategory("Calendar", R.drawable.category_calander, listOf(
            widget("Next event", R.drawable.calendar_today, CalendarWidget::class.java,
                sizes("1 × 1" to R.layout.widget_calendar_compact, "2 × 1" to R.layout.widget_calendar_compact,
                    "3 × 1" to R.layout.widget_calendar_compact, "4 × 1" to R.layout.widget_calendar_compact,
                    "2 × 2" to R.layout.widget_calendar_large, "4 × 2" to R.layout.widget_calendar_large)),
            widget("Calendar display", R.drawable.calendar_today, CalendarDisplayWidget::class.java,
                sizes("2 × 2" to R.layout.widget_calendar_display, "4 × 2" to R.layout.widget_calendar_display))
        )),
        PickerCategory("Storage", R.drawable.category_storage, listOf(widget(
            "Storage", R.drawable.manage_storage, StorageWidget::class.java,
            sizes("1 × 1" to R.layout.widget_storage_compact, "2 × 1" to R.layout.widget_storage_compact,
                "3 × 1" to R.layout.widget_storage_compact, "4 × 1" to R.layout.widget_storage_compact,
                "2 × 2" to R.layout.widget_storage_large, "4 × 2" to R.layout.widget_storage_large)
        ))),
        PickerCategory("Health", R.drawable.category_health, listOf(
            stepCounterWidget(),
            widget("Hydration", R.drawable.water_drop, HydrationWidget::class.java,
                sizes("1 × 1" to R.layout.widget_hydration_compact, "2 × 1" to R.layout.widget_hydration_compact,
                    "3 × 1" to R.layout.widget_hydration_compact, "4 × 1" to R.layout.widget_hydration_compact,
                    "2 × 2" to R.layout.widget_hydration_large, "4 × 2" to R.layout.widget_hydration_large))
        )),
        PickerCategory("Search", R.drawable.category_search, listOf(
            actionWidget("Google Search", R.drawable.google, GoogleSearchWidget::class.java, 2),
            actionWidget("Samsung Browser search", R.drawable.samsung_browser, SamsungBrowserSearchWidget::class.java, 2),
            actionWidget("YouTube search", R.drawable.youtube, YouTubeSearchWidget::class.java, 2)
        )),
        PickerCategory("Media player", R.drawable.category_media, listOf(
            widget("Media player", R.drawable.music_alt, MediaPlayerWidget::class.java,
                sizes("2 × 1" to R.layout.widget_media_compact, "2 × 2" to R.layout.widget_media_large,
                    "3 × 2" to R.layout.widget_media_large, "4 × 2" to R.layout.widget_media_large))
        )),
        PickerCategory("Other", R.drawable.category_more, listOf(
            actionWidget("Open app", R.drawable.phone, OpenAppWidget::class.java, 2),
            actionWidget("Text", R.drawable.text_check_on, TextWidget::class.java, 2),
            widget("Counter", R.drawable.list_add, CounterWidget::class.java,
                sizes("2 × 1" to R.layout.widget_counter_compact, "2 × 2" to R.layout.widget_counter_large,
                    "4 × 2" to R.layout.widget_counter_large))
        )),
        PickerCategory("Toggles", R.drawable.category_settings, ToggleKind.entries.map { kind ->
            widget(kind.label, kind.icon, ToggleWidget::class.java,
                toggleSizes("1 × 1" to R.layout.widget_toggle_square, "2 × 1" to R.layout.widget_toggle), kind)
        })
    )

    private fun actionWidget(title: String, icon: Int, provider: Class<out AppWidgetProvider>, count: Int): PickerWidget {
        val variants = if (count == 2) {
            sizes("1 × 1" to R.layout.widget_action, "2 × 1" to R.layout.widget_action)
        } else {
            sizes("1 × 1" to R.layout.widget_action, "2 × 1" to R.layout.widget_action,
                "2 × 2" to R.layout.widget_action, "4 × 2" to R.layout.widget_action)
        }
        return widget(title, icon, provider, variants)
    }

    private fun stepCounterWidget() = widget(
        "Step counter", R.drawable.running, StepCounterWidget::class.java,
        sizes(
            "1 × 1" to R.layout.widget_steps_compact,
            "2 × 1" to R.layout.widget_steps_compact,
            "3 × 1" to R.layout.widget_steps_compact,
            "4 × 1" to R.layout.widget_steps_compact,
            "5 × 1" to R.layout.widget_steps_compact
        )
    )

    private fun sizes(vararg definitions: Pair<String, Int>): List<WidgetSize> = definitions
        .filterNot { it.first == "1 × 1" }
        .map { (name, layout) ->
        val parts = name.split(" × ")
        WidgetSize(name, layout, parts[0].toInt(), parts[1].toInt())
    }

    private fun toggleSizes(vararg definitions: Pair<String, Int>): List<WidgetSize> = definitions.map { (name, layout) ->
        val parts = name.split(" × ")
        WidgetSize(name, layout, parts[0].toInt(), parts[1].toInt())
    }

    private fun widget(title: String, icon: Int, provider: Class<out AppWidgetProvider>, sizes: List<WidgetSize>, toggleKind: ToggleKind? = null) =
        PickerWidget(title, icon, provider, sizes, toggleKind)

    private fun categoryCard(category: PickerCategory): View {
        val card = RoundedLinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(themeColor(android.R.attr.colorBackground))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val row = pickerRow(category.title, category.icon, category.widgetCountText())
        row.setOnClickListener { toggle(content, row) }
        card.addView(row)
        category.widgets.forEachIndexed { index, item ->
            if (category.widgets.size == 1) content.addView(previewSection(item, false))
            else content.addView(widgetRow(item, index != 0))
        }
        card.addView(content)
        return card
    }

    private fun widgetRow(item: PickerWidget, showDivider: Boolean): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        if (showDivider) addView(divider())
        val previews = previewSection(item, true).apply { visibility = View.GONE }
        val row = pickerRow(item.title, item.icon, item.sizeCountText())
        row.setOnClickListener { toggle(previews, row) }
        addView(row)
        addView(previews)
    }

    private fun previewSection(item: PickerWidget, inset: Boolean): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(if (inset) dp(24) else dp(16), 0, if (inset) dp(24) else dp(16), dp(16))
        var index = 0
        while (index < item.sizes.size) {
            val current = item.sizes[index]
            val next = item.sizes.getOrNull(index + 1)
            if (index > 0) addView(divider(dp(8)))
            if (current.isSingleCell && next?.isWideSingleRow == true) {
                addView(inlinePreviewRow(item, current, next))
                index += 2
            } else {
                addView(previewTile(item, current))
                index++
            }
        }
    }

    private fun inlinePreviewRow(item: PickerWidget, square: WidgetSize, wide: WidgetSize): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.TOP
        addView(previewTile(item, square), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(View(this@MainActivity), LinearLayout.LayoutParams(dp(8), 1))
        addView(previewTile(item, wide), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f))
    }

    private fun previewTile(item: PickerWidget, size: WidgetSize): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = selectableBackground()
        isClickable = true
        isFocusable = true
        contentDescription = getString(R.string.place_widget_preview, item.title, size.label)
        setPadding(dp(4), dp(6), dp(4), dp(6))
        setOnClickListener { showPlacementPrompt(item, size) }
        addView(TextView(this@MainActivity).apply {
            text = size.label
            textSize = 13f
            setTextColor(themeColor(android.R.attr.textColorSecondary))
            gravity = Gravity.CENTER_HORIZONTAL
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(ScaledWidgetPreview(this@MainActivity, size.layout, size.columns, size.rows, item.title, item.icon),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun pickerRow(title: String, icon: Int, count: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(72)
        setPadding(dp(20), dp(10), dp(14), dp(10))
        background = selectableBackground()
        isClickable = true
        isFocusable = true
        addView(ImageView(this@MainActivity).apply {
            setImageResource(icon)
            imageTintList = null
            contentDescription = null
        }, LinearLayout.LayoutParams(dp(28), dp(28)))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 17f
                setTypeface(typeface, Typeface.NORMAL)
                setTextColor(themeColor(android.R.attr.textColorPrimary))
                maxLines = 1
            })
            addView(TextView(this@MainActivity).apply {
                text = count
                textSize = 14f
                setTextColor(themeColor(android.R.attr.textColorSecondary))
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) })
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(18) })
        addView(ImageView(this@MainActivity).apply {
            setImageResource(R.drawable.ic_widget_picker_expand)
            imageTintList = android.content.res.ColorStateList.valueOf(themeColor(android.R.attr.textColorSecondary))
            contentDescription = getString(R.string.expand_widget_category)
        }, LinearLayout.LayoutParams(dp(24), dp(24)))
    }

    private fun toggle(content: View, row: LinearLayout) {
        val expanded = content.visibility != View.VISIBLE
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        (row.getChildAt(row.childCount - 1) as? ImageView)?.apply {
            animate().rotation(if (expanded) 180f else 0f).setDuration(160).start()
            contentDescription = getString(if (expanded) R.string.collapse_widget_category else R.string.expand_widget_category)
        }
    }

    private fun showPlacementPrompt(item: PickerWidget, size: WidgetSize) {
        AlertDialog.Builder(this)
            .setTitle(R.string.place_widget_title)
            .setMessage(getString(R.string.place_widget_message, item.title, size.label))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.place_widget_action) { _, _ -> requestPlacement(item, size) }
            .show()
    }

    private fun requestPlacement(item: PickerWidget, size: WidgetSize) {
        val provider = item.provider
        if (provider == WeatherWidget::class.java && !hasWeatherLocationPermission()) {
            pendingProvider = provider
            pendingSize = size
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), WEATHER_PERMISSION_REQUEST)
        } else if (provider == CalendarWidget::class.java && checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            pendingProvider = provider
            pendingSize = size
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), CALENDAR_PERMISSION_REQUEST)
        } else {
            requestWidgetPin(provider, size, item.toggleKind)
        }
    }

    private fun hasWeatherLocationPermission() = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestWidgetPin(provider: Class<out AppWidgetProvider>, size: WidgetSize, toggleKind: ToggleKind? = null) {
        val manager = getSystemService(AppWidgetManager::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(this, provider), widgetOptions(size, toggleKind), null)
        } else {
            AlertDialog.Builder(this)
                .setMessage(R.string.add_widget_not_supported)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == CALENDAR_PERMISSION_REQUEST || requestCode == WEATHER_PERMISSION_REQUEST) &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            pendingProvider?.let { provider -> pendingSize?.let { requestWidgetPin(provider, it) } }
        }
        pendingProvider = null
        pendingSize = null
    }

    /** Samsung Home honors these app-widget dimensions when a widget is pinned from the app. */
    private fun widgetOptions(size: WidgetSize, toggleKind: ToggleKind?) = Bundle().apply {
        val width = 72 + (size.columns - 1) * 90
        val height = 76 + (size.rows - 1) * 100
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, arrayListOf(SizeF(width.toFloat(), height.toFloat())))
        }
        toggleKind?.let { putString(ToggleWidget.OPTION_TOGGLE_KIND, it.name) }
    }

    private fun divider(height: Int = dp(12)): View = FrameLayout(this).apply {
        addView(View(this@MainActivity).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(1).toFloat()
                setColor(if (isDarkTheme()) 0x66FFFFFF else 0x22000000)
            }
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1), Gravity.CENTER_VERTICAL))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    }

    private fun isDarkTheme() = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun selectableBackground() = obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).run {
        getDrawable(0).also { recycle() }
    }

    private fun themeColor(attribute: Int): Int {
        val value = android.util.TypedValue()
        theme.resolveAttribute(attribute, value, true)
        return if (value.resourceId != 0) ContextCompat.getColor(this, value.resourceId) else value.data
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private data class PickerCategory(val title: String, val icon: Int, val widgets: List<PickerWidget>) {
        fun widgetCountText(): String {
            val count = widgets.sumOf { it.sizes.size }
            return "$count widget${if (count == 1) "" else "s"}"
        }
    }
    private data class PickerWidget(val title: String, val icon: Int, val provider: Class<out AppWidgetProvider>, val sizes: List<WidgetSize>, val toggleKind: ToggleKind? = null) {
        fun sizeCountText() = "${sizes.size} size${if (sizes.size == 1) "" else "s"}"
    }
    private data class WidgetSize(val label: String, val layout: Int, val columns: Int, val rows: Int) {
        val isSingleCell get() = columns == 1 && rows == 1
        val isWideSingleRow get() = columns == 2 && rows == 1
    }

    private companion object {
        const val CALENDAR_PERMISSION_REQUEST = 41
        const val WEATHER_PERMISSION_REQUEST = 42
    }
}

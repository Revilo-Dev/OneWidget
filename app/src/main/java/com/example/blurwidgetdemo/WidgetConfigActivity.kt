package com.example.blurwidgetdemo

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.picker3.app.SeslColorPickerDialog
import dev.oneuiproject.oneui.widget.CardItemView
import dev.oneuiproject.oneui.widget.SwitchItemView
import kotlin.math.min
import kotlin.math.roundToInt
import com.example.blurwidgetdemo.widgets.clock.DigitalClockWidget
import com.example.blurwidgetdemo.widgets.clock.ClockWidgetVariants
import com.example.blurwidgetdemo.studio.battery.BatteryStudioDescriptor
import com.example.blurwidgetdemo.studio.clock.ClockStudioDescriptor
import com.example.blurwidgetdemo.studio.storage.StorageStudioDescriptor
import com.example.blurwidgetdemo.studio.calendar.CalendarStudioDescriptor
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.production.ProductionWidgetDesignRepository
import com.example.blurwidgetdemo.studio.production.WidgetProductionDesignModeRepository
import com.example.blurwidgetdemo.studio.rendering.WidgetRenderSize
import com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetStudioCatalog
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetStyle
import com.example.blurwidgetdemo.widgets.clock.ClockWidgetPreferences
import com.example.blurwidgetdemo.widgets.clock.ClockTimeFormat
import com.example.blurwidgetdemo.widgets.clock.ClockTextAlignment
import com.example.blurwidgetdemo.widgets.clock.ClockTapAction
import com.example.blurwidgetdemo.widgets.clock.ClockFont
import com.example.blurwidgetdemo.widgets.clock.ClockWidgetLayout
import com.example.blurwidgetdemo.widgets.clock.ClockConfigPreviewMapper
import com.example.blurwidgetdemo.widgets.clock.ClockRemoteViewsRenderer
import com.example.blurwidgetdemo.widgets.clock.ClockRenderMode
import com.example.blurwidgetdemo.widgets.clock.ClockRenderSize
import com.example.blurwidgetdemo.widgets.battery.BatteryConfigPreviewMapper
import com.example.blurwidgetdemo.widgets.battery.BatteryRemoteViewsRenderer
import com.example.blurwidgetdemo.widgets.battery.BatteryRenderMode
import com.example.blurwidgetdemo.widgets.battery.BatteryRenderSize
import com.example.blurwidgetdemo.widgets.battery.BatteryWidgetConfigurationRepository
import com.example.blurwidgetdemo.widgets.battery.BatteryWidget
import com.example.blurwidgetdemo.studio.preview.RemoteViewsPreviewHost
import com.example.blurwidgetdemo.widgets.weather.WeatherWidget
import com.example.blurwidgetdemo.widgets.WidgetAppearance
import com.example.blurwidgetdemo.widgets.calendar.CalendarWidget
import com.example.blurwidgetdemo.widgets.calendar.CalendarConfigPreviewMapper
import com.example.blurwidgetdemo.widgets.calendar.CalendarRemoteViewsRenderer
import com.example.blurwidgetdemo.widgets.calendar.CalendarRenderMode
import com.example.blurwidgetdemo.widgets.calendar.CalendarRenderSize
import com.example.blurwidgetdemo.widgets.calendar.CalendarSamples
import com.example.blurwidgetdemo.widgets.calendar.CalendarWidgetConfigurationRepository
import com.example.blurwidgetdemo.widgets.calendar.CalendarDisplayWidget
import com.example.blurwidgetdemo.widgets.storage.StorageWidget
import com.example.blurwidgetdemo.widgets.storage.StorageConfigPreviewMapper
import com.example.blurwidgetdemo.widgets.storage.StorageRemoteViewsRenderer
import com.example.blurwidgetdemo.widgets.storage.StorageRenderMode
import com.example.blurwidgetdemo.widgets.storage.StorageRenderSize
import com.example.blurwidgetdemo.widgets.storage.StorageWidgetConfigurationRepository
import com.example.blurwidgetdemo.widgets.hydration.HydrationWidget
import com.example.blurwidgetdemo.widgets.extra.ToggleKind
import com.example.blurwidgetdemo.widgets.extra.ToggleWidget
import com.example.blurwidgetdemo.widgets.extra.ToggleWidgetV2
import com.example.blurwidgetdemo.widgets.extra.ToggleWidgetV3
import com.example.blurwidgetdemo.widgets.extra.AlarmWidget
import com.example.blurwidgetdemo.widgets.extra.CounterWidget
import com.example.blurwidgetdemo.widgets.extra.OpenAppWidget
import com.example.blurwidgetdemo.widgets.extra.StepCounterWidget
import com.example.blurwidgetdemo.widgets.extra.TextWidget
import com.example.blurwidgetdemo.widgets.extra.TodoItem
import com.example.blurwidgetdemo.widgets.extra.TodoWidget
import com.example.blurwidgetdemo.widgets.extra.WidgetAssetBitmaps
import com.example.blurwidgetdemo.widgets.variants.ExactWidgetVariants
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SeslSeekBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun Drawable.copyForPreview(): Drawable =
    constantState?.newDrawable()?.mutate() ?: mutate()

class WidgetConfigActivity : AppCompatActivity() {

    private enum class ImageTarget { OPEN_APP_BACKGROUND, OPEN_APP_ICON, TEXT_BACKGROUND }

    private var pendingImageTarget: ImageTarget? = null
    private val imagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            when (pendingImageTarget) {
                ImageTarget.OPEN_APP_BACKGROUND -> openAppBackgroundUri = uri.toString()
                ImageTarget.OPEN_APP_ICON -> openAppIconUri = uri.toString()
                ImageTarget.TEXT_BACKGROUND -> textBackgroundUri = uri.toString()
                null -> Unit
            }
            refreshAssetOptionSummaries()
            updatePreview()
        }
        pendingImageTarget = null
    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var currentHue = BlurWidget.DEFAULT_TINT_HUE
    private var currentSaturation = BlurWidget.DEFAULT_TINT_SATURATION
    private var currentValue = BlurWidget.DEFAULT_TINT_VALUE
    private var currentAlpha = BlurWidget.DEFAULT_TINT_ALPHA
    private var useDarkTint = false
    private var suppressTintModeChange = false
    private var recentColors = listOf(BlurWidget.tintColor(
        BlurWidget.DEFAULT_TINT_HUE,
        BlurWidget.DEFAULT_TINT_SATURATION,
        BlurWidget.DEFAULT_TINT_VALUE,
        BlurWidget.DEFAULT_TINT_ALPHA
    ))
    private var colorPickerDialog: SeslColorPickerDialog? = null
    private var previewWallpaper: Drawable? = null
    private var clockSettings: com.example.blurwidgetdemo.widgets.clock.ClockWidgetSettings? = null
    private var currentTextScale = 100
    private var currentTextColor = Color.WHITE
    private var currentTextAlignment = WidgetAppearance.Alignment.CENTRE
    private var storageCompactMetric = StorageWidget.CompactMetric.STORAGE
    private var storageHideData = false
    private var storageShowTitles = true
    private var storageDetailMode = StorageWidget.DetailMode.BOTH
    private var toggleAccentColor = 0xFF387AFF.toInt()
    private var toggleKind = ToggleKind.WIFI
    private var batteryCompactDevice = "phone"
    private var batteryColors = mutableMapOf("low" to 0xFFF44336.toInt(), "power_saving" to 0xFFFF9800.toInt(), "normal" to 0xFF4CAF50.toInt(), "charging" to 0xFF009688.toInt())
    private var storageColors = mutableMapOf(
        StorageWidget.CompactMetric.STORAGE to 0xFF08B0CE.toInt(),
        StorageWidget.CompactMetric.DATA to 0xFF5BD686.toInt(),
        StorageWidget.CompactMetric.MEMORY to 0xFF1D79ED.toInt()
    )
    private var calendarAccentColor = 0xFF08B0CE.toInt()
    private var weatherUnit = "C"
    private var weatherShowLocation = true
    private var openAppPackage: String? = null
    private var openAppClass: String? = null
    private var openAppName = ""
    private var openAppCustomLabel = ""
    private var openAppBackgroundUri: String? = null
    private var openAppIconUri: String? = null
    private var textWidgetContent = "Text"
    private var textWidgetEmoji = ""
    private var textWidgetIconEnabled = true
    private var textBackgroundUri: String? = null
    private var alarmHour = -1
    private var alarmMinute = 0
    private var hydrationHour = 0
    private var hydrationMinute = 0
    private var hydrationGoal = 8
    private var hydrationShowGoal = true
    private var counterHour = 0
    private var counterMinute = 0
    private var counterShowClear = true
    private var todoItems = emptyList<TodoItem>()
    private var todoClearHour = -1
    private var todoClearMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            fontDebug("configuration received invalid appWidgetId")
            finish()
            return
        }
        fontDebug("configuration id=$appWidgetId provider=${providerClassName()}")

        loadSettings()
        setupClockOptions()
        setupUniversalTextOptions()
        setupStorageOptions()
        setupToggleOptions()
        setupBatteryOptions()
        setupCalendarOptions()
        setupWeatherOptions()
        setupOpenAppOptions()
        setupTextWidgetOptions()
        setupAlarmOptions()
        setupHydrationOptions()
        setupCounterOptions()
        setupTodoOptions()
        setupStepCounterOptions()
        setupWallpaperPreview()
        setupColourPicker()
        setupPresetControls()
        setupButtons()
        updatePreview()
    }

    override fun onDestroy() {
        colorPickerDialog?.dismiss()
        colorPickerDialog = null
        super.onDestroy()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE)
        val defaults = BlurWidget.defaultTint(prefs)
        currentHue = prefs.getFloat("tint_hue_$appWidgetId", defaults.hue)
        currentSaturation = prefs.getFloat("tint_saturation_$appWidgetId", defaults.saturation)
        currentValue = prefs.getFloat("tint_value_$appWidgetId", defaults.value)
        currentAlpha = if (prefs.contains("tint_alpha_$appWidgetId")) {
            prefs.getInt("tint_alpha_$appWidgetId", defaults.alpha)
        } else if (prefs.contains("level_$appWidgetId")) {
            val oldLevel = prefs.getInt("level_$appWidgetId", 1).coerceIn(0, BlurWidget.PRESET_ALPHAS.lastIndex)
            BlurWidget.PRESET_ALPHAS[oldLevel]
        } else defaults.alpha.coerceIn(MIN_BLUR_ALPHA, MAX_BLUR_ALPHA)
        useDarkTint = currentSaturation < NEUTRAL_SATURATION_THRESHOLD && currentValue < DARK_VALUE_THRESHOLD
        recentColors = listOf(currentTintColor(), BlurWidget.tintColor(
            BlurWidget.DEFAULT_TINT_HUE,
            BlurWidget.DEFAULT_TINT_SATURATION,
            BlurWidget.DEFAULT_TINT_VALUE,
            BlurWidget.DEFAULT_TINT_ALPHA
        )).distinct().take(MAX_RECENT_COLORS)
        if (isClockWidget()) clockSettings = ClockWidgetPreferences.load(ClockWidgetPreferences.preferences(this), appWidgetId)
        currentTextScale = if (isClockWidget()) clockSettings!!.textScalePercent else WidgetAppearance.textScale(prefs, appWidgetId)
        currentTextColor = if (isClockWidget()) clockSettings!!.textColor else WidgetAppearance.textColor(prefs, appWidgetId)
        currentTextAlignment = if (isWeatherWidget() && !prefs.contains("text_alignment_$appWidgetId")) WidgetAppearance.Alignment.START else if (isClockWidget()) when (clockSettings!!.alignment) {
            ClockTextAlignment.START -> WidgetAppearance.Alignment.START
            ClockTextAlignment.CENTER -> WidgetAppearance.Alignment.CENTRE
            ClockTextAlignment.END -> WidgetAppearance.Alignment.END
        } else WidgetAppearance.alignment(prefs, appWidgetId)
        if (isSearchWidget() && currentTextAlignment == WidgetAppearance.Alignment.END) {
            currentTextAlignment = WidgetAppearance.Alignment.CENTRE
        }
        storageCompactMetric = StorageWidget.CompactMetric.from(prefs.getString("storage_compact_metric_$appWidgetId", null))
        storageHideData = prefs.getBoolean("storage_hide_data_$appWidgetId", false)
        storageShowTitles = prefs.getBoolean("storage_show_titles_$appWidgetId", true)
        storageDetailMode = StorageWidget.DetailMode.from(prefs.getString("storage_detail_mode_$appWidgetId", null))
        toggleAccentColor = prefs.getInt("toggle_accent_$appWidgetId", 0xFF387AFF.toInt())
        val initialToggleKind = AppWidgetManager.getInstance(this).getAppWidgetOptions(appWidgetId)
            .getString(ToggleWidget.OPTION_TOGGLE_KIND)
        toggleKind = ToggleKind.from(prefs.getString("toggle_kind_$appWidgetId", initialToggleKind))
        batteryCompactDevice = prefs.getString("battery_compact_device_$appWidgetId", "phone") ?: "phone"
        batteryColors.keys.forEach { batteryColors[it] = BatteryWidget.colour(this, appWidgetId, it) }
        StorageWidget.CompactMetric.entries.forEach { storageColors[it] = StorageWidget.barColor(this, appWidgetId, it) }
        if (isCalendarDisplayWidget()) calendarAccentColor = CalendarDisplayWidget.accentColor(this, appWidgetId)
        weatherUnit = prefs.getString("weather_unit_$appWidgetId", "C") ?: "C"
        weatherShowLocation = prefs.getBoolean("weather_show_location_$appWidgetId", true)
        openAppPackage = prefs.getString("open_app_package_$appWidgetId", null)
        openAppClass = prefs.getString("open_app_class_$appWidgetId", null)
        openAppName = prefs.getString("open_app_label_$appWidgetId", "").orEmpty()
        openAppCustomLabel = prefs.getString("open_app_custom_label_$appWidgetId", "").orEmpty()
        openAppBackgroundUri = prefs.getString("open_app_background_uri_$appWidgetId", null)
        openAppIconUri = prefs.getString("open_app_icon_uri_$appWidgetId", null)
        textWidgetContent = prefs.getString("text_widget_$appWidgetId", "Text") ?: "Text"
        textWidgetEmoji = prefs.getString("text_emoji_$appWidgetId", "").orEmpty()
        textWidgetIconEnabled = prefs.getBoolean("text_icon_enabled_$appWidgetId", true)
        textBackgroundUri = prefs.getString("text_background_uri_$appWidgetId", null)
        alarmHour = prefs.getInt("alarm_hour_$appWidgetId", -1)
        alarmMinute = prefs.getInt("alarm_minute_$appWidgetId", 0)
        hydrationHour = prefs.getInt("hydration_hour_$appWidgetId", 0)
        hydrationMinute = prefs.getInt("hydration_minute_$appWidgetId", 0)
        hydrationGoal = prefs.getInt("hydration_goal_$appWidgetId", 8).coerceAtLeast(1)
        hydrationShowGoal = prefs.getBoolean("hydration_show_goal_$appWidgetId", true)
        counterHour = prefs.getInt("counter_hour_$appWidgetId", 0)
        counterMinute = prefs.getInt("counter_minute_$appWidgetId", 0)
        counterShowClear = prefs.getBoolean("counter_show_clear_$appWidgetId", true)
        todoItems = TodoWidget.items(this, appWidgetId)
        todoClearHour = prefs.getInt("todo_clear_hour_$appWidgetId", -1)
        todoClearMinute = prefs.getInt("todo_clear_minute_$appWidgetId", 0)
    }

    private fun providerClassName(): String? = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className
    private fun baseProviderClassName(): String? = ExactWidgetVariants.baseClassName(providerClassName())
    private fun fontDebug(message: String) {
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) Log.d("ClockFontDebug", message)
    }
    private fun isClockWidget(): Boolean = baseProviderClassName() == DigitalClockWidget::class.java.name
    private fun isBatteryWidget(): Boolean = baseProviderClassName() == BatteryWidget::class.java.name
    private fun isWeatherWidget(): Boolean = baseProviderClassName() == WeatherWidget::class.java.name
    private fun isCalendarWidget(): Boolean = baseProviderClassName() == CalendarWidget::class.java.name
    private fun isCalendarDisplayWidget(): Boolean = baseProviderClassName() == CalendarDisplayWidget::class.java.name
    private fun isStorageWidget(): Boolean = baseProviderClassName() == StorageWidget::class.java.name
    private fun isHydrationWidget(): Boolean = baseProviderClassName() == HydrationWidget::class.java.name
    private fun isOpenAppWidget(): Boolean = baseProviderClassName() == OpenAppWidget::class.java.name
    private fun isTextWidget(): Boolean = baseProviderClassName() == TextWidget::class.java.name
    private fun isAlarmWidget(): Boolean = baseProviderClassName() == AlarmWidget::class.java.name
    private fun isCounterWidget(): Boolean = baseProviderClassName() == CounterWidget::class.java.name
    private fun isTodoWidget(): Boolean = baseProviderClassName() == TodoWidget::class.java.name
    private fun isStepCounterWidget(): Boolean = baseProviderClassName() == StepCounterWidget::class.java.name
    private fun isSearchWidget(): Boolean = baseProviderClassName()?.let {
        it in setOf(
            com.example.blurwidgetdemo.widgets.extra.GoogleSearchWidget::class.java.name,
            com.example.blurwidgetdemo.widgets.extra.SamsungBrowserSearchWidget::class.java.name,
            com.example.blurwidgetdemo.widgets.extra.YouTubeSearchWidget::class.java.name
        )
    } == true
    private fun isToggleWidget(): Boolean = providerClassName()?.let { provider ->
        baseProviderClassName() == ToggleWidget::class.java.name || provider in setOf(
            ToggleWidget::class.java.name,
            ToggleWidgetV2::class.java.name,
            ToggleWidgetV3::class.java.name
        )
    } == true

    private fun setupUniversalTextOptions() {
        findViewById<CardItemView>(R.id.universal_text_scale).summary = "${currentTextScale}%"
        findViewById<SeslSeekBar>(R.id.universal_text_scale_slider).progress = currentTextScale - 50
        findViewById<CardItemView>(R.id.universal_text_colour).summary = "#${currentTextColor.toUInt().toString(16).uppercase().padStart(8, '0')}"
        findViewById<CardItemView>(R.id.universal_text_alignment).summary = currentTextAlignment.label
        findViewById<SeslSeekBar>(R.id.universal_text_scale_slider).setOnSeekBarChangeListener(object : SeslSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeslSeekBar, progress: Int, fromUser: Boolean) {
                currentTextScale = progress + 50
                if (isClockWidget()) clockSettings = clockSettings?.copy(textScalePercent = currentTextScale)
                findViewById<CardItemView>(R.id.universal_text_scale).summary = "${currentTextScale}%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeslSeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeslSeekBar) = Unit
        })
        findViewById<CardItemView>(R.id.universal_text_colour).setOnClickListener {
            SeslColorPickerDialog(this, { color ->
                currentTextColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
                if (isClockWidget()) clockSettings = clockSettings?.copy(textColor = currentTextColor)
                findViewById<CardItemView>(R.id.universal_text_colour).summary = "#${currentTextColor.toUInt().toString(16).uppercase().padStart(8, '0')}"
                updatePreview()
            }, currentTextColor, intArrayOf(currentTextColor), true).show()
        }
        findViewById<CardItemView>(R.id.universal_text_alignment).setOnClickListener {
            val alignmentOptions = if (isSearchWidget() || isWeatherWidget()) {
                listOf(WidgetAppearance.Alignment.CENTRE, WidgetAppearance.Alignment.START)
            } else {
                WidgetAppearance.Alignment.entries.toList()
            }
            AlertDialog.Builder(this).setItems(alignmentOptions.map { it.label }.toTypedArray()) { _, which ->
                currentTextAlignment = alignmentOptions[which]
                if (isClockWidget()) clockSettings = clockSettings?.copy(alignment = when (currentTextAlignment) {
                    WidgetAppearance.Alignment.START -> ClockTextAlignment.START
                    WidgetAppearance.Alignment.CENTRE -> ClockTextAlignment.CENTER
                    WidgetAppearance.Alignment.END -> ClockTextAlignment.END
                })
                findViewById<CardItemView>(R.id.universal_text_alignment).summary = currentTextAlignment.label
                updatePreview()
            }.show()
        }
    }

    private fun setupStorageOptions() {
        if (!isStorageWidget()) return
        findViewById<View>(R.id.storage_options_section).visibility = View.VISIBLE
        findViewById<SwitchItemView>(R.id.storage_hide_data).apply {
            isChecked = storageHideData
            onCheckedChangedListener = { _, checked -> storageHideData = checked; updatePreview() }
        }
        findViewById<SwitchItemView>(R.id.storage_show_titles).apply { isChecked = storageShowTitles; onCheckedChangedListener = { _, checked -> storageShowTitles = checked; updatePreview() } }
        findViewById<CardItemView>(R.id.storage_detail_mode).apply {
            summary = storageDetailMode.label
            setOnClickListener { AlertDialog.Builder(this@WidgetConfigActivity).setItems(StorageWidget.DetailMode.entries.map { it.label }.toTypedArray()) { _, which -> storageDetailMode = StorageWidget.DetailMode.entries[which]; summary = storageDetailMode.label; updatePreview() }.show() }
        }
        val item = findViewById<CardItemView>(R.id.storage_compact_metric)
        item.summary = storageCompactMetric.label
        item.setOnClickListener {
            AlertDialog.Builder(this).setItems(StorageWidget.CompactMetric.entries.map { it.label }.toTypedArray()) { _, which ->
                storageCompactMetric = StorageWidget.CompactMetric.entries[which]
                item.summary = storageCompactMetric.label
                updatePreview()
            }.show()
        }
        listOf(
            StorageWidget.CompactMetric.STORAGE to R.id.storage_storage_colour,
            StorageWidget.CompactMetric.DATA to R.id.storage_data_colour,
            StorageWidget.CompactMetric.MEMORY to R.id.storage_memory_colour
        ).forEach { (metric, viewId) ->
            val colorItem = findViewById<CardItemView>(viewId)
            fun refresh() { colorItem.summary = "#${storageColors.getValue(metric).toUInt().toString(16).uppercase().takeLast(6)}" }
            refresh()
            colorItem.setOnClickListener {
                SeslColorPickerDialog(this, { color ->
                    storageColors[metric] = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
                    refresh(); updatePreview()
                }, storageColors.getValue(metric), intArrayOf(storageColors.getValue(metric)), true).show()
            }
        }
    }

    private fun setupToggleOptions() {
        if (!isToggleWidget()) return
        findViewById<View>(R.id.toggle_options_section).visibility = View.VISIBLE
        fun select(kind: ToggleKind) {
            toggleKind = kind
            if (kind == ToggleKind.TORCH && checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            }
        }
        select(toggleKind)
        findViewById<CardItemView>(R.id.toggle_type).apply {
            summary = toggleKind.label
            setOnClickListener { AlertDialog.Builder(this@WidgetConfigActivity).setItems(ToggleKind.entries.map { it.label }.toTypedArray()) { _, which -> select(ToggleKind.entries[which]); summary = toggleKind.label; updatePreview() }.show() }
        }
        val item = findViewById<CardItemView>(R.id.toggle_accent_colour)
        fun refresh() { item.summary = "#${toggleAccentColor.toUInt().toString(16).uppercase().takeLast(6)}" }
        refresh()
        item.setOnClickListener {
            SeslColorPickerDialog(this, { color ->
                toggleAccentColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
                refresh(); updatePreview()
            }, toggleAccentColor, intArrayOf(toggleAccentColor), true).show()
        }
    }

    private fun setupBatteryOptions() {
        if (!isBatteryWidget()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), BLUETOOTH_PERMISSION_REQUEST)
        }
        findViewById<View>(R.id.battery_options_section).visibility = View.VISIBLE
        val deviceItem = findViewById<CardItemView>(R.id.battery_compact_device)
        fun devices() = BatteryWidget.availableDevices(this)
        fun refreshDevice() { deviceItem.summary = devices().firstOrNull { it.key == batteryCompactDevice }?.label ?: "Phone" }
        refreshDevice()
        deviceItem.setOnClickListener {
            val options = devices()
            AlertDialog.Builder(this).setItems(options.map { it.label }.toTypedArray()) { _, which ->
                batteryCompactDevice = options[which].key; refreshDevice(); updatePreview()
            }.show()
        }
        listOf("low" to R.id.battery_low_colour, "power_saving" to R.id.battery_power_saving_colour, "normal" to R.id.battery_normal_colour, "charging" to R.id.battery_charging_colour).forEach { (state, viewId) ->
            val item = findViewById<CardItemView>(viewId)
            fun refresh() { item.summary = "#${batteryColors.getValue(state).toUInt().toString(16).uppercase().takeLast(6)}" }
            refresh()
            item.setOnClickListener { SeslColorPickerDialog(this, { color -> batteryColors[state] = Color.rgb(Color.red(color), Color.green(color), Color.blue(color)); refresh(); updatePreview() }, batteryColors.getValue(state), intArrayOf(batteryColors.getValue(state)), true).show() }
        }
    }

    private fun setupCalendarOptions() {
        if (!isCalendarWidget() && !isCalendarDisplayWidget()) return
        if (isCalendarWidget()) {
            requestPermissionIfNeeded(android.Manifest.permission.READ_CALENDAR, CALENDAR_PERMISSION_REQUEST)
        }
        if (!isCalendarDisplayWidget()) return
        findViewById<View>(R.id.calendar_options_section).visibility = View.VISIBLE
        val item = findViewById<CardItemView>(R.id.calendar_accent_colour)
        fun refresh() { item.summary = "#${calendarAccentColor.toUInt().toString(16).uppercase().takeLast(6)}" }
        refresh()
        item.setOnClickListener {
            SeslColorPickerDialog(this, { color ->
                calendarAccentColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
                refresh(); updatePreview()
            }, calendarAccentColor, intArrayOf(calendarAccentColor), true).show()
        }
    }

    private fun setupWeatherOptions() {
        if (!isWeatherWidget()) return
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                WEATHER_PERMISSION_REQUEST
            )
        }
        findViewById<View>(R.id.weather_options_section).visibility = View.VISIBLE
        val unit = findViewById<CardItemView>(R.id.weather_temperature_unit)
        fun bindUnit() { unit.summary = if (weatherUnit == "F") "Fahrenheit" else "Celsius" }
        bindUnit()
        unit.setOnClickListener {
            AlertDialog.Builder(this).setItems(arrayOf("Celsius (°C)", "Fahrenheit (°F)")) { _, which ->
                weatherUnit = if (which == 0) "C" else "F"; bindUnit(); updatePreview()
            }.show()
        }
        findViewById<SwitchItemView>(R.id.weather_show_location).apply {
            isChecked = weatherShowLocation
            onCheckedChangedListener = { _, checked -> weatherShowLocation = checked; updatePreview() }
        }
    }

    private fun setupOpenAppOptions() {
        if (!isOpenAppWidget()) return
        findViewById<View>(R.id.open_app_options_section).visibility = View.VISIBLE
        refreshAssetOptionSummaries()
        findViewById<CardItemView>(R.id.open_app_choose_app).setOnClickListener { chooseOpenApp() }
        findViewById<CardItemView>(R.id.open_app_label).setOnClickListener {
            showTextEditor("Widget label", openAppCustomLabel, "Use the app name") { value ->
                openAppCustomLabel = value
                refreshAssetOptionSummaries()
                updatePreview()
            }
        }
        findViewById<CardItemView>(R.id.open_app_background_image).setOnClickListener {
            chooseOrRemoveImage(ImageTarget.OPEN_APP_BACKGROUND, openAppBackgroundUri)
        }
        findViewById<CardItemView>(R.id.open_app_icon_image).setOnClickListener {
            chooseOrRemoveImage(ImageTarget.OPEN_APP_ICON, openAppIconUri)
        }
    }

    private fun setupTextWidgetOptions() {
        if (!isTextWidget()) return
        findViewById<View>(R.id.text_widget_options_section).visibility = View.VISIBLE
        refreshAssetOptionSummaries()
        findViewById<CardItemView>(R.id.text_widget_content).setOnClickListener {
            showTextEditor("Widget text", textWidgetContent, "Enter widget text", multiline = true) { value ->
                textWidgetContent = value.ifBlank { "Text" }
                refreshAssetOptionSummaries()
                updatePreview()
            }
        }
        findViewById<CardItemView>(R.id.text_widget_icon).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Icon")
                .setItems(arrayOf("Use default icon", "Use emoji", "Hide icon")) { _, which ->
                    when (which) {
                        0 -> {
                            textWidgetIconEnabled = true
                            textWidgetEmoji = ""
                            refreshAssetOptionSummaries()
                            updatePreview()
                        }
                        1 -> showTextEditor("Emoji icon", textWidgetEmoji, "Enter any emoji") { value ->
                            textWidgetEmoji = value.trim()
                            textWidgetIconEnabled = textWidgetEmoji.isNotEmpty()
                            refreshAssetOptionSummaries()
                            updatePreview()
                        }
                        else -> {
                            textWidgetIconEnabled = false
                            refreshAssetOptionSummaries()
                            updatePreview()
                        }
                    }
                }
                .show()
        }
        findViewById<CardItemView>(R.id.text_widget_background_image).setOnClickListener {
            chooseOrRemoveImage(ImageTarget.TEXT_BACKGROUND, textBackgroundUri)
        }
    }

    private fun setupAlarmOptions() {
        if (!isAlarmWidget()) return
        findViewById<View>(R.id.alarm_options_section).visibility = View.VISIBLE
        val item = findViewById<CardItemView>(R.id.alarm_time)
        fun bind() { item.summary = if (alarmHour < 0) "Not set" else formatTime(alarmHour, alarmMinute) }
        bind()
        item.setOnClickListener {
            val now = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                alarmHour = hour
                alarmMinute = minute
                bind()
                updatePreview()
            }, if (alarmHour < 0) now.get(Calendar.HOUR_OF_DAY) else alarmHour, alarmMinute, false).show()
        }
    }

    private fun setupHydrationOptions() {
        if (!isHydrationWidget()) return
        findViewById<View>(R.id.hydration_options_section).visibility = View.VISIBLE
        val clearTime = findViewById<CardItemView>(R.id.hydration_clear_time)
        val goal = findViewById<CardItemView>(R.id.hydration_goal)
        fun bind() {
            clearTime.summary = formatTime(hydrationHour, hydrationMinute)
            goal.summary = "$hydrationGoal glasses"
        }
        bind()
        clearTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                hydrationHour = hour
                hydrationMinute = minute
                bind()
                updatePreview()
            }, hydrationHour, hydrationMinute, false).show()
        }
        goal.setOnClickListener {
            showNumberEditor("Daily goal", hydrationGoal) { value ->
                hydrationGoal = value.coerceAtLeast(1)
                bind()
                updatePreview()
            }
        }
        findViewById<SwitchItemView>(R.id.hydration_show_goal).apply {
            isChecked = hydrationShowGoal
            onCheckedChangedListener = { _, checked -> hydrationShowGoal = checked; updatePreview() }
        }
    }

    private fun setupCounterOptions() {
        if (!isCounterWidget()) return
        findViewById<View>(R.id.counter_options_section).visibility = View.VISIBLE
        val clearTime = findViewById<CardItemView>(R.id.counter_clear_time)
        fun bind() { clearTime.summary = formatTime(counterHour, counterMinute) }
        bind()
        clearTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                counterHour = hour
                counterMinute = minute
                bind()
                updatePreview()
            }, counterHour, counterMinute, false).show()
        }
        findViewById<SwitchItemView>(R.id.counter_show_clear).apply {
            isChecked = counterShowClear
            onCheckedChangedListener = { _, checked -> counterShowClear = checked; updatePreview() }
        }
    }

    private fun setupTodoOptions() {
        if (!isTodoWidget()) return
        findViewById<View>(R.id.todo_options_section).visibility = View.VISIBLE
        refreshTodoSummaries()
        findViewById<CardItemView>(R.id.todo_items).setOnClickListener { showTodoItemsMenu() }
        findViewById<CardItemView>(R.id.todo_clear_time).setOnClickListener {
            val options = if (todoClearHour < 0) arrayOf("Choose a daily clear time")
            else arrayOf("Change daily clear time", "Turn automatic clearing off")
            AlertDialog.Builder(this).setTitle("Clear automatically").setItems(options) { _, which ->
                if (todoClearHour >= 0 && which == 1) {
                    todoClearHour = -1
                    refreshTodoSummaries()
                    updatePreview()
                } else {
                    TimePickerDialog(this, { _, hour, minute ->
                        todoClearHour = hour
                        todoClearMinute = minute
                        refreshTodoSummaries()
                        updatePreview()
                    }, if (todoClearHour < 0) 9 else todoClearHour, todoClearMinute, false).show()
                }
            }.show()
        }
    }

    private fun setupStepCounterOptions() {
        if (!isStepCounterWidget()) return
        findViewById<View>(R.id.steps_options_section).visibility = View.VISIBLE
        val permission = findViewById<CardItemView>(R.id.steps_permission)
        fun bind() {
            permission.summary = if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            ) "Allowed" else "Tap to allow"
        }
        bind()
        permission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
            ) requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), STEPS_PERMISSION_REQUEST)
        }
    }

    private fun chooseOpenApp() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = packageManager.queryIntentActivities(launcherIntent, 0)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
        if (activities.isEmpty()) {
            Toast.makeText(this, "No launchable apps found", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Choose an app")
            .setItems(activities.map { it.loadLabel(packageManager) }.toTypedArray()) { _, which ->
                val selected = activities[which]
                openAppPackage = selected.activityInfo.packageName
                openAppClass = selected.activityInfo.name
                openAppName = selected.loadLabel(packageManager).toString()
                refreshAssetOptionSummaries()
                updatePreview()
            }
            .show()
    }

    private fun chooseOrRemoveImage(target: ImageTarget, currentValue: String?) {
        if (currentValue == null) {
            pendingImageTarget = target
            imagePicker.launch(arrayOf("image/*"))
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Image")
            .setItems(arrayOf("Choose another image", "Remove image")) { _, which ->
                if (which == 0) {
                    pendingImageTarget = target
                    imagePicker.launch(arrayOf("image/*"))
                } else {
                    when (target) {
                        ImageTarget.OPEN_APP_BACKGROUND -> openAppBackgroundUri = null
                        ImageTarget.OPEN_APP_ICON -> openAppIconUri = null
                        ImageTarget.TEXT_BACKGROUND -> textBackgroundUri = null
                    }
                    refreshAssetOptionSummaries()
                    updatePreview()
                }
            }
            .show()
    }

    private fun refreshAssetOptionSummaries() {
        if (isOpenAppWidget()) {
            findViewById<CardItemView>(R.id.open_app_choose_app).summary = openAppName.ifBlank { "Choose an app" }
            findViewById<CardItemView>(R.id.open_app_label).summary = openAppCustomLabel.ifBlank { "Use app name" }
            findViewById<CardItemView>(R.id.open_app_background_image).summary = if (openAppBackgroundUri == null) "None" else "Custom image"
            findViewById<CardItemView>(R.id.open_app_icon_image).summary = if (openAppIconUri == null) "Use app icon" else "Custom image"
        }
        if (isTextWidget()) {
            findViewById<CardItemView>(R.id.text_widget_content).summary = textWidgetContent
            findViewById<CardItemView>(R.id.text_widget_icon).summary = when {
                !textWidgetIconEnabled -> "Off"
                textWidgetEmoji.isNotBlank() -> textWidgetEmoji
                else -> "Default icon"
            }
            findViewById<CardItemView>(R.id.text_widget_background_image).summary = if (textBackgroundUri == null) "None" else "Custom image"
        }
    }

    private fun showTodoItemsMenu() {
        val choices = buildList {
            add("Add item")
            todoItems.forEach { add("Remove · ${it.text}") }
            if (todoItems.isNotEmpty()) add("Clear all")
        }
        AlertDialog.Builder(this).setTitle("To-do items").setItems(choices.toTypedArray()) { _, which ->
            when {
                which == 0 -> showTextEditor("Add item", "", "To-do") { value ->
                    if (value.isNotBlank()) todoItems = todoItems + TodoItem(value.trim(), false)
                    refreshTodoSummaries()
                    updatePreview()
                }
                which <= todoItems.size -> {
                    todoItems = todoItems.filterIndexed { index, _ -> index != which - 1 }
                    refreshTodoSummaries()
                    updatePreview()
                }
                else -> {
                    todoItems = emptyList()
                    refreshTodoSummaries()
                    updatePreview()
                }
            }
        }.show()
    }

    private fun refreshTodoSummaries() {
        if (!isTodoWidget()) return
        findViewById<CardItemView>(R.id.todo_items).summary = when (todoItems.size) {
            0 -> "No items"
            1 -> "1 item"
            else -> "${todoItems.size} items"
        }
        findViewById<CardItemView>(R.id.todo_clear_time).summary =
            if (todoClearHour < 0) "Off" else formatTime(todoClearHour, todoClearMinute)
    }

    private fun showTextEditor(
        title: String,
        currentValue: String,
        hint: String,
        multiline: Boolean = false,
        onSave: (String) -> Unit
    ) {
        val input = EditText(this).apply {
            setText(currentValue)
            this.hint = hint
            setSingleLine(!multiline)
            if (multiline) minLines = 3
            setSelection(text.length)
        }
        AlertDialog.Builder(this).setTitle(title).setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> onSave(input.text.toString().trim()) }
            .show()
    }

    private fun showNumberEditor(title: String, currentValue: Int, onSave: (Int) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentValue.toString())
            selectAll()
        }
        AlertDialog.Builder(this).setTitle(title).setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> onSave(input.text.toString().toIntOrNull() ?: currentValue) }
            .show()
    }

    private fun formatTime(hour: Int, minute: Int): String = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(time)
    }

    private fun requestPermissionIfNeeded(permission: String, requestCode: Int) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALENDAR_PERMISSION_REQUEST &&
            grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.calendar_permission_denied_config, Toast.LENGTH_LONG).show()
        } else if (requestCode == WEATHER_PERMISSION_REQUEST &&
            grantResults.none { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, R.string.weather_permission_denied_config, Toast.LENGTH_LONG).show()
        } else if (requestCode == STEPS_PERMISSION_REQUEST) {
            if (grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.steps_permission_denied, Toast.LENGTH_LONG).show()
            }
            setupStepCounterOptions()
            updatePreview()
        }
    }

    private fun setupClockOptions() {
        if (!isClockWidget()) return
        // The RemoteViews preview uses some of the same view IDs as these controls. Scope
        // lookups to the settings section so a preview TextClock can never be cast to a
        // settings SwitchItemView when the selected font refreshes the preview.
        val clockOptions = findViewById<View>(R.id.clock_options_section)
        clockOptions.visibility = View.VISIBLE
        fun <T : View> control(id: Int): T = clockOptions.findViewById(id)
        // Text controls are shared with every widget in the universal Text section.
        control<View>(R.id.clock_text_scale).visibility = View.GONE
        control<View>(R.id.clock_text_scale_slider).visibility = View.GONE
        control<View>(R.id.clock_text_colour).visibility = View.GONE
        var settings = clockSettings ?: return
        fun bind() {
            control<SwitchItemView>(R.id.clock_show_date).isChecked = settings.showDate
            control<SwitchItemView>(R.id.clock_show_day).isChecked = settings.showDayOfWeek
            control<SwitchItemView>(R.id.clock_date_above).isChecked = settings.dateAboveTime
            control<SwitchItemView>(R.id.clock_show_phone_battery).isChecked = settings.showPhoneBattery
            control<SwitchItemView>(R.id.clock_background).isChecked = settings.backgroundEnabled
            control<SwitchItemView>(R.id.clock_show_am_pm).apply {
                visibility = if (settings.timeFormat == ClockTimeFormat.TWENTY_FOUR_HOUR) View.GONE else View.VISIBLE
                isChecked = settings.showAmPm
            }
            control<SwitchItemView>(R.id.clock_show_seconds).isChecked = settings.showSeconds
            control<CardItemView>(R.id.clock_time_format).summary = settings.timeFormat.label()
            control<CardItemView>(R.id.clock_alignment).summary = settings.alignment.label()
            control<CardItemView>(R.id.clock_tap_action).summary = "Open ${settings.tapAction.label()}"
            control<CardItemView>(R.id.clock_font).summary = settings.font.label()
            control<CardItemView>(R.id.clock_text_scale).summary = "${settings.textScalePercent}%"
            control<SeslSeekBar>(R.id.clock_text_scale_slider).progress = settings.textScalePercent - 50
            control<CardItemView>(R.id.clock_text_colour).summary = "#${settings.textColor.toUInt().toString(16).uppercase().padStart(8, '0')}"
        }
        bind()
        control<SwitchItemView>(R.id.clock_show_date).onCheckedChangedListener = { _, checked -> settings = settings.copy(showDate = checked); clockSettings = settings; updatePreview() }
        control<SwitchItemView>(R.id.clock_show_day).onCheckedChangedListener = { _, checked -> settings = settings.copy(showDayOfWeek = checked); clockSettings = settings; updatePreview() }
        control<SwitchItemView>(R.id.clock_date_above).onCheckedChangedListener = { _, checked -> settings = settings.copy(dateAboveTime = checked); clockSettings = settings; updatePreview() }
        control<SwitchItemView>(R.id.clock_show_phone_battery).onCheckedChangedListener = { _, checked -> settings = settings.copy(showPhoneBattery = checked); clockSettings = settings; updatePreview() }
        control<SwitchItemView>(R.id.clock_background).onCheckedChangedListener = { _, checked -> settings = settings.copy(backgroundEnabled = checked); clockSettings = settings; updatePreview() }
        control<SwitchItemView>(R.id.clock_show_am_pm).onCheckedChangedListener = { _, checked -> settings = settings.copy(showAmPm = checked); clockSettings = settings; updatePreview() }
        control<SwitchItemView>(R.id.clock_show_seconds).onCheckedChangedListener = { _, checked -> settings = settings.copy(showSeconds = checked); clockSettings = settings; updatePreview() }
        control<SeslSeekBar>(R.id.clock_text_scale_slider).setOnSeekBarChangeListener(object : SeslSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeslSeekBar, progress: Int, fromUser: Boolean) {
                settings = settings.copy(textScalePercent = progress + 50)
                clockSettings = settings
                control<CardItemView>(R.id.clock_text_scale).summary = "${progress + 50}%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeslSeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeslSeekBar) = Unit
        })
        fun <T> choose(view: CardItemView, values: Array<T>, label: (T) -> String, update: (T) -> Unit) { view.setOnClickListener { AlertDialog.Builder(this).setItems(values.map(label).toTypedArray()) { _, which -> update(values[which]); bind() }.show() } }
        choose(control(R.id.clock_time_format), ClockTimeFormat.entries.toTypedArray(), { it.label() }) { settings = settings.copy(timeFormat = it); clockSettings = settings; updatePreview() }
        choose(control(R.id.clock_alignment), ClockTextAlignment.entries.toTypedArray(), { it.label() }) { settings = settings.copy(alignment = it); clockSettings = settings; updatePreview() }
        choose(control(R.id.clock_tap_action), ClockTapAction.entries.toTypedArray(), { it.label() }) { settings = settings.copy(tapAction = it); clockSettings = settings }
        choose(control(R.id.clock_font), ClockFont.entries.toTypedArray(), { it.label() }) {
            fontDebug("font selected id=$appWidgetId value=${it.preferenceValue}")
            settings = settings.copy(font = it); clockSettings = settings; updatePreview()
        }
        control<CardItemView>(R.id.clock_text_colour).setOnClickListener {
            SeslColorPickerDialog(this, { color ->
                settings = settings.copy(textColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color)))
                clockSettings = settings
                bind()
                updatePreview()
            }, settings.textColor, intArrayOf(), false).show()
        }
        clockSettings = settings
    }

    private fun Enum<*>.label(): String = name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    private fun setupWallpaperPreview() {
        val wallpaper = wallpaperPreviewDrawable()
        previewWallpaper = wallpaper
        findViewById<ImageView>(R.id.preview_wallpaper).setImageDrawable(wallpaper.copyForPreview())
    }

    private fun setupColourPicker() {
        findViewById<View>(R.id.color_picker_row).setOnClickListener {
            openColorPickerDialog()
        }
        updateColourUi()
    }

    private fun openColorPickerDialog() {
        colorPickerDialog?.dismiss()
        colorPickerDialog = SeslColorPickerDialog(
            this,
            { color -> onColorPicked(color) },
            currentTintColor(),
            recentColors.toIntArray(),
            true
        ).apply {
            setTransparencyControlEnabled(true)
            show()
            window?.decorView?.post {
                setOnBitmapSetListener { captureScreenBitmap() }
            }
        }
    }

    private fun onColorPicked(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        currentHue = hsv[0]
        currentSaturation = hsv[1]
        currentValue = hsv[2]
        currentAlpha = Color.alpha(color).coerceIn(MIN_BLUR_ALPHA, MAX_BLUR_ALPHA)
        useDarkTint = Color.luminance(color) < DARK_LUMINANCE_THRESHOLD
        recentColors = (listOf(currentTintColor()) + recentColors).distinct().take(MAX_RECENT_COLORS)
        updateTintModeSwitch()
        updateColourUi()
        updatePreview()
    }

    private fun captureScreenBitmap(): Bitmap {
        val rootView = window.decorView.rootView
        val bitmap = Bitmap.createBitmap(rootView.width.coerceAtLeast(1), rootView.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        rootView.draw(Canvas(bitmap))
        return bitmap
    }

    private fun setupPresetControls() {
        findViewById<SwitchItemView>(R.id.dark_tint_switch).apply {
            isChecked = useDarkTint
            onCheckedChangedListener = { _, checked ->
                if (!suppressTintModeChange) {
                    useDarkTint = checked
                    applyNeutralTint(currentAlpha)
                }
            }
        }

        OPACITY_PRESETS.forEach { preset ->
            findViewById<TextView>(preset.viewId).setOnClickListener {
                applyNeutralTint(preset.alpha)
            }
        }
        findViewById<TextView>(R.id.preset_default).setOnClickListener {
            resetToDefault()
        }
    }

    private fun applyNeutralTint(alpha: Int) {
        currentAlpha = alpha.coerceIn(MIN_BLUR_ALPHA, MAX_BLUR_ALPHA)
        currentHue = BlurWidget.DEFAULT_TINT_HUE
        currentSaturation = BlurWidget.DEFAULT_TINT_SATURATION
        currentValue = if (useDarkTint) 0f else 1f
        recentColors = (listOf(currentTintColor()) + recentColors).distinct().take(MAX_RECENT_COLORS)
        updateTintModeSwitch()
        updateColourUi()
        updatePreview()
    }

    private fun resetToDefault() {
        useDarkTint = false
        currentHue = BlurWidget.DEFAULT_TINT_HUE
        currentSaturation = BlurWidget.DEFAULT_TINT_SATURATION
        currentValue = BlurWidget.DEFAULT_TINT_VALUE
        currentAlpha = BlurWidget.DEFAULT_TINT_ALPHA
        updateTintModeSwitch()
        updateColourUi()
        updatePreview()
    }

    private fun updateTintModeSwitch() {
        suppressTintModeChange = true
        findViewById<SwitchItemView>(R.id.dark_tint_switch).isChecked = useDarkTint
        suppressTintModeChange = false
    }

    private fun updateColourUi() {
        val color = currentTintColor()
        val tintRow = findViewById<CardItemView>(R.id.color_picker_row)
        tintRow.summary = getString(
            R.string.tint_values,
            argbHex(color),
            opacityPercent(currentAlpha)
        )
        tintRow.getEndImageView().apply {
            setImageDrawable(null)
            background = roundedSwatchDrawable(color)
        }
    }

    private fun setupButtons() {
        findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        findViewById<TextView>(R.id.btn_save).setOnClickListener {
            saveAndFinish()
        }
    }

    private data class PreviewSpec(val minWidthDp: Int, val minHeightDp: Int, val layoutMode: Int)
    private data class OpacityPreset(val viewId: Int, val alpha: Int)

    private fun updatePreview() {
        val previewWidget = findViewById<FrameLayout>(R.id.preview_widget)
        runCatching {
            val spec = currentPreviewSpec()
            sizePreviewWidget(previewWidget, spec)
            renderPreviewWidget(previewWidget, spec)
        }.onFailure { error ->
            Log.e("ConfigPreview", "Preview update failed; using safe fallback", error)
            val fallback = PreviewSpec(180, 80, BlurWidget.layoutMode(180, 80))
            sizePreviewWidget(previewWidget, fallback)
            renderLegacyPreviewWidget(previewWidget, fallback)
        }
    }

    private fun currentPreviewSpec(): PreviewSpec {
        val options = runCatching {
            AppWidgetManager.getInstance(this).getAppWidgetOptions(appWidgetId)
        }.getOrNull()
        val minWidth = options
            ?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            ?.takeIf { it > 0 }
            ?: 360
        val minHeight = options
            ?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            ?.takeIf { it > 0 }
            ?: 260
        return PreviewSpec(minWidth, minHeight, BlurWidget.layoutMode(minWidth, minHeight))
    }

    private fun sizePreviewWidget(previewWidget: FrameLayout, spec: PreviewSpec) {
        val width = spec.minWidthDp.coerceAtLeast(150)
        val height = spec.minHeightDp.coerceAtLeast(64)
        previewWidget.layoutParams = FrameLayout.LayoutParams(dp(width), dp(height), Gravity.CENTER)
        previewWidget.post {
            val container = findViewById<FrameLayout>(R.id.preview_container)
            val availableWidth = (container.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels) - dp(34)
            val availableHeight = (container.height.takeIf { it > 0 } ?: dp(242)) - dp(28)
            val scale = min(
                availableWidth.toFloat() / previewWidget.width.coerceAtLeast(1),
                availableHeight.toFloat() / previewWidget.height.coerceAtLeast(1)
            ).coerceAtMost(1f)
            previewWidget.pivotX = previewWidget.width / 2f
            previewWidget.pivotY = previewWidget.height / 2f
            previewWidget.scaleX = scale
            previewWidget.scaleY = scale
        }
    }

    private fun renderPreviewWidget(previewWidget: FrameLayout, spec: PreviewSpec) {
        if (previewWidget is RemoteViewsPreviewHost && !isOpenAppWidget() && !isTextWidget() &&
            (isClockWidget() || isBatteryWidget() || isStorageWidget() || isCalendarWidget() ||
                UniversalWidgetStudioCatalog.definitionForProvider(providerClassName()) != null)
        ) {
            previewWidget.background = null
            previewWidget.clipToOutline = false
            val remoteViews = if (isClockWidget()) {
                val settings = ClockConfigPreviewMapper.map(
                    settings = clockSettings ?: ClockWidgetPreferences.load(
                        ClockWidgetPreferences.preferences(this),
                        appWidgetId
                    ),
                    hue = currentHue,
                    saturation = currentSaturation,
                    value = currentValue,
                    alpha = currentAlpha,
                    textScalePercent = currentTextScale,
                    textColor = currentTextColor
                )
                ClockRemoteViewsRenderer.build(
                    context = this,
                    configuration = settings,
                    renderSize = ClockRenderSize(spec.minWidthDp.toFloat(), spec.minHeightDp.toFloat()),
                    design = null,
                    mode = ClockRenderMode.CONFIG_PREVIEW
                ).remoteViews
            } else if (isBatteryWidget()) {
                val configuration = BatteryConfigPreviewMapper.map(
                    base = BatteryWidgetConfigurationRepository.load(this, appWidgetId),
                    surfaceColor = currentTintColor(),
                    textColor = currentTextColor,
                    textScalePercent = currentTextScale,
                    alignment = currentTextAlignment,
                    compactDeviceKey = batteryCompactDevice,
                    colors = batteryColors
                )
                BatteryRemoteViewsRenderer.build(
                    context = this,
                    configuration = configuration,
                    runtimeData = BatteryWidget.runtimeData(this),
                    renderSize = BatteryRenderSize(spec.minWidthDp.toFloat(), spec.minHeightDp.toFloat()),
                    design = null,
                    mode = BatteryRenderMode.CONFIG_PREVIEW
                ).remoteViews
            } else if (isStorageWidget()) {
                val configuration = StorageConfigPreviewMapper.map(
                    base = StorageWidgetConfigurationRepository.load(this, appWidgetId),
                    surfaceColor = currentTintColor(),
                    textColor = currentTextColor,
                    textScalePercent = currentTextScale,
                    alignment = currentTextAlignment,
                    compactMetric = storageCompactMetric,
                    hideData = storageHideData,
                    showTitles = storageShowTitles,
                    detailMode = storageDetailMode,
                    colors = storageColors
                )
                StorageRemoteViewsRenderer.build(
                    context = this,
                    configuration = configuration,
                    runtimeData = StorageWidget.runtimeData(this),
                    renderSize = StorageRenderSize(
                        spec.minWidthDp.toFloat(),
                        spec.minHeightDp.toFloat()
                    ),
                    design = null,
                    mode = StorageRenderMode.CONFIG_PREVIEW
                ).remoteViews
            } else if (isCalendarWidget()) {
                val configuration = CalendarConfigPreviewMapper.map(
                    base = CalendarWidgetConfigurationRepository.load(this, appWidgetId),
                    surfaceColor = currentTintColor(),
                    textColor = currentTextColor,
                    textScalePercent = currentTextScale,
                    alignment = currentTextAlignment
                )
                CalendarRemoteViewsRenderer.build(
                    context = this,
                    configuration = configuration,
                    content = CalendarSamples.event(),
                    renderSize = CalendarRenderSize(
                        spec.minWidthDp.toFloat(),
                        spec.minHeightDp.toFloat()
                    ),
                    design = null,
                    mode = CalendarRenderMode.CONFIG_PREVIEW
                ).remoteViews
            } else if (isOpenAppWidget() || isTextWidget()) {
                buildActionPreview(spec)
            } else {
                requireNotNull(
                    UniversalProductionWidgetRenderer.buildConfigurationSample(
                        context = this,
                        providerClassName = providerClassName(),
                        renderSize = WidgetRenderSize(
                            spec.minWidthDp.toFloat(),
                            spec.minHeightDp.toFloat()
                        ),
                        style = UniversalWidgetStyle(
                            surfaceColor = currentTintColor(),
                            textColor = currentTextColor,
                            textScalePercent = currentTextScale
                        )
                    )
                )
            }
            val applyResult = previewWidget.show(remoteViews)
            val status = findViewById<TextView>(R.id.config_preview_status)
            if (applyResult.successful) {
                status.text = getString(R.string.config_preview_local_note)
                return
            }
            Log.w("ConfigPreview", "RemoteViews.apply failed; using legacy preview: ${applyResult.error}")
            status.text = getString(R.string.config_preview_legacy_fallback, applyResult.error.orEmpty())
        }
        renderLegacyPreviewWidget(previewWidget, spec)
    }

    private fun buildActionPreview(spec: PreviewSpec): RemoteViews {
        val preferences = getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE)
        val oneRow = spec.minHeightDp <= 100
        val backgroundResource = when {
            oneRow && WidgetAppearance.bordersEnabled(preferences) -> R.drawable.widget_clock_background_fully_rounded
            oneRow -> R.drawable.widget_clock_background_fully_rounded_no_border
            WidgetAppearance.bordersEnabled(preferences) -> R.drawable.widget_clock_background
            else -> R.drawable.widget_clock_background_no_border
        }
        return RemoteViews(packageName, R.layout.widget_action).apply {
            setInt(android.R.id.background, "setBackgroundResource", backgroundResource)
            BlurWidget.applySurfaceColor(this, currentTintColor())
            setTextColor(R.id.action_label, currentTextColor)
            setTextViewTextSize(R.id.action_label, TypedValue.COMPLEX_UNIT_SP, 18f * currentTextScale / 100f)
            setInt(R.id.action_label, "setGravity", currentTextAlignment.gravity)
            val label: String
            val iconBitmap: Bitmap?
            val iconResource: Int
            val iconVisible: Boolean
            val backgroundUri: String?
            if (isOpenAppWidget()) {
                label = openAppCustomLabel.ifBlank { openAppName.ifBlank { "Open app" } }
                iconBitmap = WidgetAssetBitmaps.fromUri(this@WidgetConfigActivity, openAppIconUri)
                    ?: openAppPackage?.let { packageName ->
                        runCatching {
                            WidgetAssetBitmaps.fromDrawable(packageManager.getApplicationIcon(packageName))
                        }.getOrNull()
                    }
                iconResource = R.drawable.phone
                iconVisible = true
                backgroundUri = openAppBackgroundUri
            } else {
                label = textWidgetContent.ifBlank { "Text" }
                iconBitmap = textWidgetEmoji.takeIf { textWidgetIconEnabled && it.isNotBlank() }
                    ?.let(WidgetAssetBitmaps::emoji)
                iconResource = R.drawable.text_check_on
                iconVisible = textWidgetIconEnabled
                backgroundUri = textBackgroundUri
            }
            if (iconBitmap == null) setImageViewResource(R.id.action_icon, iconResource)
            else setImageViewBitmap(R.id.action_icon, iconBitmap)
            setViewVisibility(R.id.action_icon, if (iconVisible) View.VISIBLE else View.GONE)
            val background = WidgetAssetBitmaps.fromUri(this@WidgetConfigActivity, backgroundUri)
            setViewVisibility(R.id.action_background, if (background == null) View.GONE else View.VISIBLE)
            background?.let { setImageViewBitmap(R.id.action_background, it) }
            setTextViewText(R.id.action_label, label)
        }
    }

    /** Deprecated fallback retained for RemoteViews apply/inflate failures and unmigrated widgets. */
    private fun renderLegacyPreviewWidget(previewWidget: FrameLayout, spec: PreviewSpec) {
        previewWidget.removeAllViews()
        findViewById<TextView>(R.id.config_preview_status).text = getString(R.string.config_preview_local_note)
        previewWidget.background = roundedDrawable(Color.TRANSPARENT, previewRadiusFor(spec))
        previewWidget.clipToOutline = true

        val wallpaperLayer = ImageView(this).apply {
            setImageDrawable(previewWallpaper?.copyForPreview() ?: wallpaperPreviewDrawable().also { previewWallpaper = it }.copyForPreview())
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0.9f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP))
            }
        }
        previewWidget.addView(wallpaperLayer, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        val clockSettings = if (isClockWidget()) clockSettings ?: ClockWidgetPreferences.load(ClockWidgetPreferences.preferences(this), appWidgetId) else null
        previewWidget.addView(
            View(this).apply { background = roundedDrawable(if (clockSettings?.backgroundEnabled == false) Color.TRANSPARENT else currentTintColor(), previewRadiusFor(spec)) },
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        if (isOpenAppWidget() || isTextWidget()) {
            val backgroundUri = if (isOpenAppWidget()) openAppBackgroundUri else textBackgroundUri
            WidgetAssetBitmaps.fromUri(this, backgroundUri)?.let { background ->
                previewWidget.addView(ImageView(this).apply {
                    setImageBitmap(background)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }
            val iconVisible = isOpenAppWidget() || textWidgetIconEnabled
            if (iconVisible) {
                row.addView(ImageView(this).apply {
                    if (isOpenAppWidget()) {
                        val bitmap = WidgetAssetBitmaps.fromUri(this@WidgetConfigActivity, openAppIconUri)
                            ?: openAppPackage?.let { packageName ->
                                runCatching { WidgetAssetBitmaps.fromDrawable(packageManager.getApplicationIcon(packageName)) }.getOrNull()
                            }
                        if (bitmap == null) setImageResource(R.drawable.phone) else setImageBitmap(bitmap)
                    } else if (textWidgetEmoji.isNotBlank()) {
                        setImageBitmap(WidgetAssetBitmaps.emoji(textWidgetEmoji))
                    } else {
                        setImageResource(R.drawable.text_check_on)
                    }
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }, LinearLayout.LayoutParams(dp(34), dp(34)))
            }
            row.addView(TextView(this).apply {
                text = if (isOpenAppWidget()) {
                    openAppCustomLabel.ifBlank { openAppName.ifBlank { "Open app" } }
                } else textWidgetContent.ifBlank { "Text" }
                textSize = 18f * currentTextScale / 100f
                setTextColor(currentTextColor)
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                gravity = currentTextAlignment.gravity
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                if (iconVisible) marginStart = dp(12)
            })
            previewWidget.addView(row, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        } else if (isClockWidget()) {
            val settings = clockSettings!!
            val clockCategory = ClockWidgetLayout.category(spec.minWidthDp, spec.minHeightDp)
            val time = TextView(this).apply {
                text = when {
                    settings.timeFormat == ClockTimeFormat.TWELVE_HOUR && !settings.showAmPm -> if (settings.showSeconds) "12:45:30" else "12:45"
                    settings.showSeconds -> "12:45:30 PM"
                    else -> "12:45 PM"
                }
                textSize = when (clockCategory) { ClockWidgetLayout.Category.COMPACT -> 28f; ClockWidgetLayout.Category.MEDIUM -> 42f; ClockWidgetLayout.Category.LARGE -> 52f } * settings.textScalePercent / 100f
                setTextColor(settings.textColor)
                setClockTypeface(settings.font)
                gravity = android.view.Gravity.CENTER
            }
            previewWidget.addView(time, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            if (settings.showDate && spec.layoutMode != BlurWidget.LAYOUT_MODE_COMPACT_STRIP) {
                previewWidget.addView(TextView(this).apply {
                    text = if (settings.showDayOfWeek) "Tuesday, August 4" else "August 4"
                    textSize = 14f; setTextColor(settings.textColor); setClockTypeface(settings.font)
                    gravity = android.view.Gravity.CENTER_HORIZONTAL or if (settings.dateAboveTime) android.view.Gravity.TOP else android.view.Gravity.BOTTOM
                    if (settings.dateAboveTime) setPadding(0, dp(18), 0, 0) else setPadding(0, 0, 0, dp(18))
                }, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            if (settings.showPhoneBattery) {
                previewWidget.addView(TextView(this).apply {
                    text = "▱ 85%"
                    textSize = 14f * settings.textScalePercent / 100f
                    setTextColor(settings.textColor)
                    gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                    setPadding(0, 0, 0, dp(12))
                }, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
        } else if (isBatteryWidget()) {
            previewWidget.addView(TextView(this).apply {
                text = "85%"
                textSize = if (spec.layoutMode == BlurWidget.LAYOUT_MODE_COMPACT_STRIP) 25f else 42f
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        } else if (isToggleWidget()) {
            previewWidget.addView(ImageView(this).apply {
                setImageResource(toggleKind.icon)
                background = roundedDrawable(toggleAccentColor, 100f)
                setPadding(dp(18), dp(18), dp(18), dp(18))
            }, FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER))
        } else if (isWeatherWidget()) {
            previewWidget.addView(TextView(this).apply {
                text = "Weather\nSamsung Weather"
                textSize = 24f
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        } else if (isCalendarWidget() || isCalendarDisplayWidget() || isStorageWidget() || isHydrationWidget()) {
            previewWidget.addView(TextView(this).apply {
                text = when {
                    isCalendarWidget() -> "Next event\nTeam catch-up • 10:00 AM"
                    isCalendarDisplayWidget() -> "August 2026\nS 10   M 11   T 12   W 13   T 14   F 15   S 16"
                    isStorageWidget() -> "Storage 62%\n78.3 GB of 128 GB used"
                    else -> "4 / 8 glasses\nTap to add a glass"
                }
                textSize = 23f * currentTextScale / 100f
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(currentTextColor)
                gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
    }

    private fun ClockTextAlignment.previewGravity(): Int = when (this) {
        ClockTextAlignment.START -> android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        ClockTextAlignment.END -> android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
        ClockTextAlignment.CENTER -> android.view.Gravity.CENTER
    }

    private fun TextView.setClockTypeface(font: ClockFont, bold: Boolean = false) {
        val resource = font.previewFontResource()
        typeface = when {
            resource == null -> Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            else -> runCatching {
                val base = resources.getFont(resource)
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && font == ClockFont.SAMSUNG_DEFAULT_BOLD -> Typeface.create(base, 700, false)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && font == ClockFont.SAMSUNG_DEFAULT_THIN -> Typeface.create(base, 300, false)
                    bold -> Typeface.create(base, Typeface.BOLD)
                    else -> base
                }
            }.getOrElse { Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL) }
        }
    }

    private fun ClockFont.previewFontResource(): Int? = when (this) {
        ClockFont.SYSTEM -> null
        ClockFont.SAMSUNG_DEFAULT, ClockFont.SAMSUNG_DEFAULT_BOLD, ClockFont.SAMSUNG_DEFAULT_THIN -> R.font.samsung_number_default_vf
        ClockFont.SAMSUNG_MONO -> R.font.samsung_number_mono_vf
        ClockFont.CLOCK_BOLD_SERIF -> R.font.clock_bold_serif
        ClockFont.CLOCK_STRIPE -> R.font.clock_stripe
        ClockFont.CLOCK_STAMP -> R.font.clock_stamp
        ClockFont.ALATSI -> R.font.alatsi
        ClockFont.CAPRIOLA -> R.font.capriola
        ClockFont.FREDERICKA -> R.font.fredericka_the_great
        ClockFont.LATO -> R.font.lato
        ClockFont.STARDOS_STENCIL -> R.font.stardos_stencil
        ClockFont.MODAK -> R.font.modak_regular
    }

    private fun currentTintColor(): Int =
        BlurWidget.tintColor(currentHue, currentSaturation, currentValue, currentAlpha)

    private fun roundedDrawable(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = radiusDp * resources.displayMetrics.density
            setColor(color)
        }

    private fun roundedSwatchDrawable(color: Int): GradientDrawable =
        roundedDrawable(color, 16f).apply {
            setStroke(dp(1), Color.argb(70, 0, 0, 0))
        }

    private fun previewRadiusFor(spec: PreviewSpec): Float = when (spec.layoutMode) {
        BlurWidget.LAYOUT_MODE_COMPACT_STRIP -> 38f
        else -> 24f
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun saveAndFinish() {
        val preferences = getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE)
        preferences.edit()
            .putFloat("tint_hue_$appWidgetId", currentHue)
            .putFloat("tint_saturation_$appWidgetId", currentSaturation)
            .putFloat("tint_value_$appWidgetId", currentValue)
            .putInt("tint_alpha_$appWidgetId", currentAlpha)
            .putInt("text_scale_$appWidgetId", currentTextScale)
            .putInt("text_color_$appWidgetId", currentTextColor)
            .putString("text_alignment_$appWidgetId", currentTextAlignment.name)
            .putString("storage_compact_metric_$appWidgetId", storageCompactMetric.name)
            .putBoolean("storage_hide_data_$appWidgetId", storageHideData)
            .putBoolean("storage_show_titles_$appWidgetId", storageShowTitles)
            .putString("storage_detail_mode_$appWidgetId", storageDetailMode.name)
            .putInt("toggle_accent_$appWidgetId", toggleAccentColor)
            .putString("toggle_kind_$appWidgetId", toggleKind.name)
            .putString("battery_compact_device_$appWidgetId", batteryCompactDevice)
            .apply { batteryColors.forEach { (state, color) -> putInt("battery_colour_${state}_$appWidgetId", color) } }
            .apply { StorageWidget.CompactMetric.entries.forEach { putInt("storage_bar_${it.name.lowercase()}_$appWidgetId", storageColors.getValue(it)) } }
            .putInt("calendar_accent_$appWidgetId", calendarAccentColor)
            .putString("weather_unit_$appWidgetId", weatherUnit)
            .putBoolean("weather_show_location_$appWidgetId", weatherShowLocation)
            .apply {
                if (isOpenAppWidget()) {
                    if (openAppPackage == null) remove("open_app_package_$appWidgetId") else putString("open_app_package_$appWidgetId", openAppPackage)
                    if (openAppClass == null) remove("open_app_class_$appWidgetId") else putString("open_app_class_$appWidgetId", openAppClass)
                    putString("open_app_label_$appWidgetId", openAppName)
                    putString("open_app_custom_label_$appWidgetId", openAppCustomLabel)
                    if (openAppBackgroundUri == null) remove("open_app_background_uri_$appWidgetId") else putString("open_app_background_uri_$appWidgetId", openAppBackgroundUri)
                    if (openAppIconUri == null) remove("open_app_icon_uri_$appWidgetId") else putString("open_app_icon_uri_$appWidgetId", openAppIconUri)
                }
                if (isTextWidget()) {
                    putString("text_widget_$appWidgetId", textWidgetContent.ifBlank { "Text" })
                    putBoolean("text_icon_enabled_$appWidgetId", textWidgetIconEnabled)
                    if (textWidgetEmoji.isBlank()) remove("text_emoji_$appWidgetId") else putString("text_emoji_$appWidgetId", textWidgetEmoji)
                    if (textBackgroundUri == null) remove("text_background_uri_$appWidgetId") else putString("text_background_uri_$appWidgetId", textBackgroundUri)
                }
                if (isAlarmWidget() && alarmHour >= 0) {
                    putInt("alarm_hour_$appWidgetId", alarmHour)
                    putInt("alarm_minute_$appWidgetId", alarmMinute)
                    putBoolean("alarm_enabled_$appWidgetId", true)
                    remove("alarm_snoozed_$appWidgetId")
                }
                if (isHydrationWidget()) {
                    putInt("hydration_hour_$appWidgetId", hydrationHour)
                    putInt("hydration_minute_$appWidgetId", hydrationMinute)
                    putInt("hydration_goal_$appWidgetId", hydrationGoal.coerceAtLeast(1))
                    putBoolean("hydration_show_goal_$appWidgetId", hydrationShowGoal)
                }
                if (isCounterWidget()) {
                    putInt("counter_hour_$appWidgetId", counterHour)
                    putInt("counter_minute_$appWidgetId", counterMinute)
                    putBoolean("counter_show_clear_$appWidgetId", counterShowClear)
                }
                if (isTodoWidget()) {
                    putInt("todo_clear_hour_$appWidgetId", todoClearHour)
                    putInt("todo_clear_minute_$appWidgetId", todoClearMinute)
                }
            }
            .apply()

        if (isTodoWidget()) TodoWidget.save(this, appWidgetId, todoItems)

        if (isClockWidget()) {
            fontDebug("saving id=$appWidgetId font=${(clockSettings ?: ClockWidgetPreferences.load(ClockWidgetPreferences.preferences(this), appWidgetId)).font.preferenceValue}")
            ClockWidgetPreferences.save(
                ClockWidgetPreferences.preferences(this),
                appWidgetId,
                (clockSettings ?: ClockWidgetPreferences.load(ClockWidgetPreferences.preferences(this), appWidgetId)).copy(
                    hue = currentHue, saturation = currentSaturation, value = currentValue, alpha = currentAlpha,
                    textScalePercent = currentTextScale, textColor = currentTextColor
                )
            )
            fontDebug("saved id=$appWidgetId font=${ClockWidgetPreferences.load(ClockWidgetPreferences.preferences(this), appWidgetId).font.preferenceValue}")
        }

        val manager = AppWidgetManager.getInstance(this)
        val provider = providerClassName()
        val baseProvider = ExactWidgetVariants.baseClassName(provider)
        val productionWidgetId = when {
            baseProvider == DigitalClockWidget::class.java.name -> ClockStudioDescriptor.WIDGET_ID
            baseProvider == BatteryWidget::class.java.name -> BatteryStudioDescriptor.WIDGET_ID
            baseProvider == StorageWidget::class.java.name -> StorageStudioDescriptor.WIDGET_ID
            baseProvider == CalendarWidget::class.java.name -> CalendarStudioDescriptor.WIDGET_ID
            else -> UniversalWidgetStudioCatalog.definitionForProvider(baseProvider)?.widgetId
        }
        productionWidgetId?.let { widgetId ->
            val hasPromotion = when (
                val active = ProductionWidgetDesignRepository(this).loadActive(widgetId)
            ) {
                is StudioRepositoryResult.Success -> active.value != null
                is StudioRepositoryResult.Failure -> false
            }
            // Configuration completion is the creation boundary. Absent keys on upgraded
            // installed widgets stay legacy until the user deliberately reconfigures them.
            WidgetProductionDesignModeRepository(this).initializeNewWidget(
                widgetId,
                appWidgetId,
                hasPromotion
            )
        }
        if (baseProvider == DigitalClockWidget::class.java.name) {
            DigitalClockWidget.updateWidget(this, manager, appWidgetId, "CONFIG_SAVE")
        } else if (baseProvider == BatteryWidget::class.java.name) {
            BatteryWidget.updateWidget(this, manager, appWidgetId)
        } else if (baseProvider == WeatherWidget::class.java.name) {
            WeatherWidget.updateWidget(this, manager, appWidgetId)
        } else if (baseProvider == CalendarWidget::class.java.name) {
            CalendarWidget.updateWidget(this, manager, appWidgetId)
        } else if (baseProvider == CalendarDisplayWidget::class.java.name) {
            CalendarDisplayWidget.updateWidget(this, manager, appWidgetId)
        } else if (baseProvider == StorageWidget::class.java.name) {
            StorageWidget.updateWidget(this, manager, appWidgetId)
        } else if (baseProvider == HydrationWidget::class.java.name) {
            HydrationWidget.updateWidget(this, manager, appWidgetId)
        } else if (provider != null) {
            sendBroadcast(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setComponent(android.content.ComponentName(this, provider))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId)))
        } else {
            BlurWidget.updateWidget(this, manager, appWidgetId)
        }

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }

    @SuppressLint("MissingPermission")
    private fun wallpaperPreviewDrawable(): Drawable {
        val manager = WallpaperManager.getInstance(this)
        listOf<() -> Drawable?>(
            { manager.samsungSemDrawable(SAMSUNG_SUB_HOME) },
            { manager.samsungSemDrawable(SAMSUNG_MAIN_HOME) },
            { manager.samsungSemDrawable(SAMSUNG_SYSTEM_HOME) },
            { manager.samsungAppliedWallpaperDrawable() },
            { manager.appliedSystemWallpaperDrawable() },
            { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) manager.getDrawable(WallpaperManager.FLAG_SYSTEM) else null },
            { manager.peekDrawable() },
            { manager.drawable },
            { manager.fastDrawable },
            { manager.builtInDrawable }
        ).forEach { loader ->
            try {
                loader()?.let { return it }
            } catch (_: Exception) {
                // Try the next wallpaper API before falling back to a representative home-screen wash.
            }
        }
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(116, 142, 158),
                Color.rgb(196, 164, 186),
                Color.rgb(226, 232, 232)
            )
        ).apply {
            cornerRadius = 28f * resources.displayMetrics.density
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun WallpaperManager.samsungSemDrawable(which: Int): Drawable? =
        runCatching {
            val method = javaClass.methods.firstOrNull { method ->
                method.name == "semGetDrawable" &&
                    method.parameterTypes.size == 1 &&
                    method.parameterTypes[0] == Integer.TYPE
            } ?: javaClass.declaredMethods.firstOrNull { method ->
                method.name == "semGetDrawable" &&
                    method.parameterTypes.size == 1 &&
                    method.parameterTypes[0] == Integer.TYPE
            } ?: return null
            method.isAccessible = true
            method.invoke(this, which) as? Drawable
        }.getOrNull()

    @SuppressLint("MissingPermission")
    private fun WallpaperManager.appliedSystemWallpaperDrawable(): Drawable? {
        return getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use { descriptor ->
            BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor)?.let { BitmapDrawable(resources, it) }
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun WallpaperManager.samsungAppliedWallpaperDrawable(): Drawable? {
        val methods = (javaClass.methods.asSequence() + javaClass.declaredMethods.asSequence())
            .distinctBy { "${it.name}:${it.parameterTypes.joinToString()}" }
            .filter { method ->
                method.name.contains("wallpaper", ignoreCase = true) ||
                    method.name.contains("sem", ignoreCase = true)
            }
            .filter { method -> method.parameterTypes.all { it == Integer.TYPE } }
            .toList()

        val whichCandidates = intArrayOf(SAMSUNG_SUB_HOME, SAMSUNG_MAIN_HOME, SAMSUNG_SYSTEM_HOME, WallpaperManager.FLAG_SYSTEM)
        val userCandidates = intArrayOf(0)
        methods.forEach { method ->
            val args = when (method.parameterTypes.size) {
                1 -> whichCandidates.map { arrayOf<Any>(it) }
                2 -> whichCandidates.flatMap { which ->
                    userCandidates.flatMap { user ->
                        listOf(arrayOf<Any>(which, user), arrayOf<Any>(user, which))
                    }
                }
                else -> emptyList()
            }
            args.forEach { candidate ->
                runCatching {
                    method.isAccessible = true
                    when (val result = method.invoke(this, *candidate)) {
                        is Drawable -> return result
                        is Bitmap -> return BitmapDrawable(resources, result)
                        is ParcelFileDescriptor -> result.use { descriptor ->
                            BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor)?.let {
                                return BitmapDrawable(resources, it)
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun argbHex(color: Int): String = "#${color.toUInt().toString(16).uppercase().padStart(8, '0')}"
    private fun opacityPercent(alpha: Int): Int = (alpha.coerceIn(0, 255) / 255f * 100f).roundToInt()

    companion object {
        private const val CALENDAR_PERMISSION_REQUEST = 101
        private const val WEATHER_PERMISSION_REQUEST = 102
        private const val BLUETOOTH_PERMISSION_REQUEST = 103
        private const val CAMERA_PERMISSION_REQUEST = 104
        private const val STEPS_PERMISSION_REQUEST = 105
        private const val MIN_BLUR_ALPHA = 1
        private const val MAX_BLUR_ALPHA = 254
        private const val MAX_RECENT_COLORS = 6
        private const val NEUTRAL_SATURATION_THRESHOLD = 0.05f
        private const val DARK_VALUE_THRESHOLD = 0.5f
        private const val DARK_LUMINANCE_THRESHOLD = 0.5f
        private const val SAMSUNG_MAIN_HOME = 5
        private const val SAMSUNG_SUB_HOME = 17
        private const val SAMSUNG_SYSTEM_HOME = 1
        private val OPACITY_PRESETS = listOf(
            OpacityPreset(R.id.preset_glass, 38),
            OpacityPreset(R.id.preset_light, 102),
            OpacityPreset(R.id.preset_medium, 178),
            OpacityPreset(R.id.preset_solid, 240)
        )
    }
}

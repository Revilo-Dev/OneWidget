package com.example.blurwidgetdemo

import android.annotation.SuppressLint
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
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.picker3.app.SeslColorPickerDialog
import dev.oneuiproject.oneui.widget.CardItemView
import dev.oneuiproject.oneui.widget.SwitchItemView
import kotlin.math.min
import kotlin.math.roundToInt
import com.example.blurwidgetdemo.widgets.clock.DigitalClockWidget
import com.example.blurwidgetdemo.widgets.clock.ClockWidgetPreferences
import com.example.blurwidgetdemo.widgets.clock.ClockTimeFormat
import com.example.blurwidgetdemo.widgets.clock.ClockTextAlignment
import com.example.blurwidgetdemo.widgets.clock.ClockTapAction
import com.example.blurwidgetdemo.widgets.clock.ClockFont
import com.example.blurwidgetdemo.widgets.clock.ClockWidgetLayout
import com.example.blurwidgetdemo.widgets.clock.fontResource
import com.example.blurwidgetdemo.widgets.battery.BatteryWidget
import com.example.blurwidgetdemo.widgets.weather.WeatherWidget
import com.example.blurwidgetdemo.widgets.WidgetAppearance
import com.example.blurwidgetdemo.widgets.calendar.CalendarWidget
import com.example.blurwidgetdemo.widgets.calendar.CalendarDisplayWidget
import com.example.blurwidgetdemo.widgets.storage.StorageWidget
import com.example.blurwidgetdemo.widgets.hydration.HydrationWidget
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SeslSeekBar

private fun Drawable.copyForPreview(): Drawable =
    constantState?.newDrawable()?.mutate() ?: mutate()

class WidgetConfigActivity : AppCompatActivity() {

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
        currentHue = prefs.getFloat("tint_hue_$appWidgetId", BlurWidget.DEFAULT_TINT_HUE)
        currentSaturation = prefs.getFloat("tint_saturation_$appWidgetId", BlurWidget.DEFAULT_TINT_SATURATION)
        currentValue = prefs.getFloat("tint_value_$appWidgetId", BlurWidget.DEFAULT_TINT_VALUE)
        currentAlpha = if (prefs.contains("tint_alpha_$appWidgetId")) {
            prefs.getInt("tint_alpha_$appWidgetId", BlurWidget.DEFAULT_TINT_ALPHA)
        } else {
            val oldLevel = prefs.getInt("level_$appWidgetId", 1).coerceIn(0, BlurWidget.PRESET_ALPHAS.lastIndex)
            BlurWidget.PRESET_ALPHAS[oldLevel]
        }.coerceIn(MIN_BLUR_ALPHA, MAX_BLUR_ALPHA)
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
        currentTextAlignment = if (isClockWidget()) when (clockSettings!!.alignment) {
            ClockTextAlignment.START -> WidgetAppearance.Alignment.START
            ClockTextAlignment.CENTER -> WidgetAppearance.Alignment.CENTRE
            ClockTextAlignment.END -> WidgetAppearance.Alignment.END
        } else WidgetAppearance.alignment(prefs, appWidgetId)
        storageCompactMetric = StorageWidget.CompactMetric.from(prefs.getString("storage_compact_metric_$appWidgetId", null))
        storageHideData = prefs.getBoolean("storage_hide_data_$appWidgetId", false)
        storageShowTitles = prefs.getBoolean("storage_show_titles_$appWidgetId", true)
        storageDetailMode = StorageWidget.DetailMode.from(prefs.getString("storage_detail_mode_$appWidgetId", null))
        toggleAccentColor = prefs.getInt("toggle_accent_$appWidgetId", 0xFF387AFF.toInt())
        batteryCompactDevice = prefs.getString("battery_compact_device_$appWidgetId", "phone") ?: "phone"
        batteryColors.keys.forEach { batteryColors[it] = BatteryWidget.colour(this, appWidgetId, it) }
        StorageWidget.CompactMetric.entries.forEach { storageColors[it] = StorageWidget.barColor(this, appWidgetId, it) }
        if (isCalendarDisplayWidget()) calendarAccentColor = CalendarDisplayWidget.accentColor(this, appWidgetId)
        weatherUnit = prefs.getString("weather_unit_$appWidgetId", "C") ?: "C"
        weatherShowLocation = prefs.getBoolean("weather_show_location_$appWidgetId", true)
    }

    private fun providerClassName(): String? = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className
    private fun fontDebug(message: String) {
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) Log.d("ClockFontDebug", message)
    }
    private fun isClockWidget(): Boolean = providerClassName() == DigitalClockWidget::class.java.name
    private fun isBatteryWidget(): Boolean = providerClassName() == BatteryWidget::class.java.name
    private fun isWeatherWidget(): Boolean = providerClassName() == WeatherWidget::class.java.name
    private fun isCalendarWidget(): Boolean = providerClassName() == CalendarWidget::class.java.name
    private fun isCalendarDisplayWidget(): Boolean = providerClassName() == CalendarDisplayWidget::class.java.name
    private fun isStorageWidget(): Boolean = providerClassName() == StorageWidget::class.java.name
    private fun isHydrationWidget(): Boolean = providerClassName() == HydrationWidget::class.java.name
    private fun isToggleWidget(): Boolean = providerClassName()?.substringAfterLast('.') in setOf("DataToggleWidget", "DarkModeToggleWidget", "WifiToggleWidget", "DoNotDisturbToggleWidget", "BluetoothToggleWidget", "LocationToggleWidget", "HotspotToggleWidget", "CameraToggleWidget", "TorchToggleWidget")

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
            AlertDialog.Builder(this).setItems(WidgetAppearance.Alignment.entries.map { it.label }.toTypedArray()) { _, which ->
                currentTextAlignment = WidgetAppearance.Alignment.entries[which]
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
        if (providerClassName()?.endsWith("TorchToggleWidget") == true && checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 103)
        findViewById<View>(R.id.toggle_options_section).visibility = View.VISIBLE
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
            requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 102)
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
        requestPermissionIfNeeded(android.Manifest.permission.READ_CALENDAR, 101)
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
        requestPermissionIfNeeded(android.Manifest.permission.ACCESS_COARSE_LOCATION, 102)
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

    private fun requestPermissionIfNeeded(permission: String, requestCode: Int) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(permission), requestCode)
    }

    private fun setupClockOptions() {
        if (!isClockWidget()) return
        findViewById<View>(R.id.clock_options_section).visibility = View.VISIBLE
        // Text controls are shared with every widget in the universal Text section.
        findViewById<View>(R.id.clock_text_scale).visibility = View.GONE
        findViewById<View>(R.id.clock_text_scale_slider).visibility = View.GONE
        findViewById<View>(R.id.clock_text_colour).visibility = View.GONE
        var settings = clockSettings ?: return
        fun bind() {
            findViewById<SwitchItemView>(R.id.clock_show_date).isChecked = settings.showDate
            findViewById<SwitchItemView>(R.id.clock_show_day).isChecked = settings.showDayOfWeek
            findViewById<SwitchItemView>(R.id.clock_date_above).isChecked = settings.dateAboveTime
            findViewById<SwitchItemView>(R.id.clock_show_phone_battery).isChecked = settings.showPhoneBattery
            findViewById<SwitchItemView>(R.id.clock_background).isChecked = settings.backgroundEnabled
            findViewById<SwitchItemView>(R.id.clock_show_am_pm).apply {
                visibility = if (settings.timeFormat == ClockTimeFormat.TWELVE_HOUR) View.VISIBLE else View.GONE
                isChecked = settings.showAmPm
            }
            findViewById<SwitchItemView>(R.id.clock_show_seconds).isChecked = settings.showSeconds
            findViewById<CardItemView>(R.id.clock_time_format).summary = settings.timeFormat.label()
            findViewById<CardItemView>(R.id.clock_alignment).summary = settings.alignment.label()
            findViewById<CardItemView>(R.id.clock_tap_action).summary = "Open ${settings.tapAction.label()}"
            findViewById<CardItemView>(R.id.clock_font).summary = settings.font.label()
            findViewById<CardItemView>(R.id.clock_text_scale).summary = "${settings.textScalePercent}%"
            findViewById<SeslSeekBar>(R.id.clock_text_scale_slider).progress = settings.textScalePercent - 50
            findViewById<CardItemView>(R.id.clock_text_colour).summary = "#${settings.textColor.toUInt().toString(16).uppercase().padStart(8, '0')}"
        }
        bind()
        findViewById<SwitchItemView>(R.id.clock_show_date).onCheckedChangedListener = { _, checked -> settings = settings.copy(showDate = checked); clockSettings = settings; updatePreview() }
        findViewById<SwitchItemView>(R.id.clock_show_day).onCheckedChangedListener = { _, checked -> settings = settings.copy(showDayOfWeek = checked); clockSettings = settings; updatePreview() }
        findViewById<SwitchItemView>(R.id.clock_date_above).onCheckedChangedListener = { _, checked -> settings = settings.copy(dateAboveTime = checked); clockSettings = settings; updatePreview() }
        findViewById<SwitchItemView>(R.id.clock_show_phone_battery).onCheckedChangedListener = { _, checked -> settings = settings.copy(showPhoneBattery = checked); clockSettings = settings; updatePreview() }
        findViewById<SwitchItemView>(R.id.clock_background).onCheckedChangedListener = { _, checked -> settings = settings.copy(backgroundEnabled = checked); clockSettings = settings; updatePreview() }
        findViewById<SwitchItemView>(R.id.clock_show_am_pm).onCheckedChangedListener = { _, checked -> settings = settings.copy(showAmPm = checked); clockSettings = settings; updatePreview() }
        findViewById<SwitchItemView>(R.id.clock_show_seconds).onCheckedChangedListener = { _, checked -> settings = settings.copy(showSeconds = checked); clockSettings = settings; updatePreview() }
        findViewById<SeslSeekBar>(R.id.clock_text_scale_slider).setOnSeekBarChangeListener(object : SeslSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeslSeekBar, progress: Int, fromUser: Boolean) {
                settings = settings.copy(textScalePercent = progress + 50)
                clockSettings = settings
                findViewById<CardItemView>(R.id.clock_text_scale).summary = "${progress + 50}%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeslSeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeslSeekBar) = Unit
        })
        fun <T> choose(view: CardItemView, values: Array<T>, label: (T) -> String, update: (T) -> Unit) { view.setOnClickListener { AlertDialog.Builder(this).setItems(values.map(label).toTypedArray()) { _, which -> update(values[which]); bind() }.show() } }
        choose(findViewById(R.id.clock_time_format), ClockTimeFormat.entries.toTypedArray(), { it.label() }) { settings = settings.copy(timeFormat = it); clockSettings = settings; updatePreview() }
        choose(findViewById(R.id.clock_alignment), ClockTextAlignment.entries.toTypedArray(), { it.label() }) { settings = settings.copy(alignment = it); clockSettings = settings; updatePreview() }
        choose(findViewById(R.id.clock_tap_action), ClockTapAction.entries.toTypedArray(), { it.label() }) { settings = settings.copy(tapAction = it); clockSettings = settings }
        choose(findViewById(R.id.clock_font), ClockFont.entries.toTypedArray(), { it.label() }) {
            fontDebug("font selected id=$appWidgetId value=${it.preferenceValue}")
            settings = settings.copy(font = it); clockSettings = settings; updatePreview()
        }
        findViewById<CardItemView>(R.id.clock_text_colour).setOnClickListener {
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
        val spec = currentPreviewSpec()
        val previewWidget = findViewById<FrameLayout>(R.id.preview_widget)

        sizePreviewWidget(previewWidget, spec)
        renderPreviewWidget(previewWidget, spec)
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
        previewWidget.removeAllViews()
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
        if (isClockWidget()) {
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
                val provider = providerClassName().orEmpty()
                setImageResource(when {
                    provider.endsWith("WifiToggleWidget") -> R.drawable.wifi_2
                    provider.endsWith("DarkModeToggleWidget") -> R.drawable.dark
                    provider.endsWith("DoNotDisturbToggleWidget") -> R.drawable.do_not_disturb
                    provider.endsWith("BluetoothToggleWidget") -> R.drawable.devices
                    provider.endsWith("LocationToggleWidget") -> R.drawable.location
                    provider.endsWith("CameraToggleWidget") -> R.drawable.image
                    provider.endsWith("TorchToggleWidget") -> R.drawable.flashlight
                    else -> R.drawable.network_storage
                })
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
        val resource = font.fontResource()
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
        getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE).edit()
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
            .putString("battery_compact_device_$appWidgetId", batteryCompactDevice)
            .apply { batteryColors.forEach { (state, color) -> putInt("battery_colour_${state}_$appWidgetId", color) } }
            .apply { StorageWidget.CompactMetric.entries.forEach { putInt("storage_bar_${it.name.lowercase()}_$appWidgetId", storageColors.getValue(it)) } }
            .putInt("calendar_accent_$appWidgetId", calendarAccentColor)
            .putString("weather_unit_$appWidgetId", weatherUnit)
            .putBoolean("weather_show_location_$appWidgetId", weatherShowLocation)
            .apply()

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
        if (provider == DigitalClockWidget::class.java.name) {
            DigitalClockWidget.updateWidget(this, manager, appWidgetId, "CONFIG_SAVE")
        } else if (provider == BatteryWidget::class.java.name) {
            BatteryWidget.updateWidget(this, manager, appWidgetId)
        } else if (provider == WeatherWidget::class.java.name) {
            WeatherWidget.updateWidget(this, manager, appWidgetId)
        } else if (provider == CalendarWidget::class.java.name) {
            CalendarWidget.updateWidget(this, manager, appWidgetId)
        } else if (provider == CalendarDisplayWidget::class.java.name) {
            CalendarDisplayWidget.updateWidget(this, manager, appWidgetId)
        } else if (provider == StorageWidget::class.java.name) {
            StorageWidget.updateWidget(this, manager, appWidgetId)
        } else if (provider == HydrationWidget::class.java.name) {
            HydrationWidget.updateWidget(this, manager, appWidgetId)
        } else if (isToggleWidget() && provider != null) {
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
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

package dev.revilodev.onewidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SeslSeekBar
import androidx.picker3.app.SeslColorPickerDialog
import dev.revilodev.onewidget.studio.WidgetStudioActivity
import dev.revilodev.onewidget.studio.developer.DeveloperAccessCode
import dev.revilodev.onewidget.studio.developer.DeveloperModeCoordinator
import dev.revilodev.onewidget.studio.developer.DeveloperModeRepository
import dev.revilodev.onewidget.studio.developer.DeveloperWidgetComponentController
import dev.revilodev.onewidget.studio.developer.DeveloperWidgetUpdateReason
import dev.revilodev.onewidget.widgets.battery.BatteryWidget
import dev.revilodev.onewidget.widgets.calendar.CalendarDisplayWidget
import dev.revilodev.onewidget.widgets.calendar.CalendarWidget
import dev.revilodev.onewidget.widgets.clock.DigitalClockWidget
import dev.revilodev.onewidget.widgets.developer.DeveloperWidgetProvider
import dev.revilodev.onewidget.widgets.extra.AlarmWidget
import dev.revilodev.onewidget.widgets.extra.CounterWidget
import dev.revilodev.onewidget.widgets.extra.GoogleSearchWidget
import dev.revilodev.onewidget.widgets.extra.MediaPlayerWidget
import dev.revilodev.onewidget.widgets.extra.OpenAppWidget
import dev.revilodev.onewidget.widgets.extra.SamsungBrowserSearchWidget
import dev.revilodev.onewidget.widgets.extra.StepCounterWidget
import dev.revilodev.onewidget.widgets.extra.TextWidget
import dev.revilodev.onewidget.widgets.extra.ToggleWidget
import dev.revilodev.onewidget.widgets.extra.ToggleWidgetV2
import dev.revilodev.onewidget.widgets.extra.ToggleWidgetV3
import dev.revilodev.onewidget.widgets.extra.YouTubeSearchWidget
import dev.revilodev.onewidget.widgets.hydration.HydrationWidget
import dev.revilodev.onewidget.widgets.storage.StorageWidget
import dev.revilodev.onewidget.widgets.weather.WeatherWidget
import dev.revilodev.onewidget.widgets.variants.ExactWidgetVariants
import dev.oneuiproject.oneui.widget.CardItemView
import dev.oneuiproject.oneui.widget.SwitchItemView

class AppearanceSettingsActivity : AppCompatActivity() {
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var developerModeRepository: DeveloperModeRepository
    private lateinit var developerModeCoordinator: DeveloperModeCoordinator
    private lateinit var developerModeSwitch: SwitchItemView
    private lateinit var widgetStudioEntry: CardItemView
    private var suppressDeveloperModeChange = false
    private var defaultTint = BlurWidget.Companion.TintDefaults(
        BlurWidget.DEFAULT_TINT_HUE,
        BlurWidget.DEFAULT_TINT_SATURATION,
        BlurWidget.DEFAULT_TINT_VALUE,
        BlurWidget.DEFAULT_TINT_ALPHA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appearance_settings)
        prefs = getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE)
        developerModeRepository = DeveloperModeRepository(this)
        developerModeCoordinator = DeveloperModeCoordinator(
            developerModeRepository,
            DeveloperWidgetComponentController(this)
        ).also { it.synchronize() }
        setupDefaultAppearance()
        findViewById<SwitchItemView>(R.id.global_borders).apply {
            isChecked = prefs.getBoolean("global_widget_borders", true)
            onCheckedChangedListener = { _, checked -> prefs.edit().putBoolean("global_widget_borders", checked).apply(); refreshWidgets() }
        }
        findViewById<SwitchItemView>(R.id.global_gradients).apply {
            isChecked = prefs.getBoolean("global_widget_gradients", true)
            onCheckedChangedListener = { _, checked -> prefs.edit().putBoolean("global_widget_gradients", checked).apply(); refreshWidgets() }
        }
        setupDeveloperMode()
    }

    private fun setupDeveloperMode() {
        developerModeSwitch = findViewById(R.id.developer_mode)
        widgetStudioEntry = findViewById(R.id.widget_studio_entry)
        refreshDeveloperModeUi()
        developerModeSwitch.onCheckedChangedListener = { _, checked ->
            if (!suppressDeveloperModeChange) {
                if (checked) {
                    if (!developerModeRepository.isEnabled()) {
                        setDeveloperSwitchChecked(false)
                        showDeveloperCodeDialog()
                    }
                } else {
                    requestDisableDeveloperMode()
                }
            }
        }
        widgetStudioEntry.setOnClickListener {
            if (developerModeRepository.isEnabled()) {
                startActivity(Intent(this, WidgetStudioActivity::class.java))
            }
        }
    }

    private fun showDeveloperCodeDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            keyListener = DigitsKeyListener.getInstance("0123456789")
            filters = arrayOf(InputFilter.LengthFilter(DeveloperAccessCode.REQUIRED_LENGTH))
            hint = getString(R.string.developer_code_hint)
            setPadding(dp(24), dp(12), dp(24), dp(4))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.developer_code_title)
            .setMessage(R.string.developer_code_message)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.enable, null)
            .create()
        dialog.setOnShowListener {
            val enableButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            enableButton.isEnabled = false
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                    enableButton.isEnabled = text?.length == DeveloperAccessCode.REQUIRED_LENGTH
                    input.error = null
                }
                override fun afterTextChanged(text: Editable?) = Unit
            })
            enableButton.setOnClickListener {
                val code = input.text.toString()
                if (DeveloperAccessCode.isCorrect(code)) {
                    developerModeCoordinator.setEnabled(true)
                    DeveloperWidgetProvider.updateAll(
                        this,
                        DeveloperWidgetUpdateReason.DEVELOPER_MODE_ENABLED
                    )
                    refreshDeveloperModeUi()
                    dialog.dismiss()
                } else {
                    input.error = getString(R.string.developer_code_invalid)
                    input.selectAll()
                }
            }
            input.requestFocus()
            dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        dialog.show()
    }

    private fun requestDisableDeveloperMode() {
        val developerWidgetCount = DeveloperWidgetProvider.appWidgetIds(this).size
        if (developerWidgetCount == 0) {
            disableDeveloperMode()
            return
        }
        setDeveloperSwitchChecked(true)
        AlertDialog.Builder(this)
            .setTitle(R.string.disable_developer_mode_title)
            .setMessage(
                resources.getQuantityString(
                    R.plurals.disable_developer_mode_message,
                    developerWidgetCount,
                    developerWidgetCount
                )
            )
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                setDeveloperSwitchChecked(true)
            }
            .setPositiveButton(R.string.disable_anyway) { _, _ ->
                disableDeveloperMode()
            }
            .setOnCancelListener { setDeveloperSwitchChecked(true) }
            .show()
    }

    private fun disableDeveloperMode() {
        developerModeCoordinator.setEnabled(false)
        refreshDeveloperModeUi()
    }

    private fun refreshDeveloperModeUi() {
        val enabled = developerModeRepository.isEnabled()
        setDeveloperSwitchChecked(enabled)
        widgetStudioEntry.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun setDeveloperSwitchChecked(checked: Boolean) {
        suppressDeveloperModeChange = true
        developerModeSwitch.isChecked = checked
        suppressDeveloperModeChange = false
    }

    private fun setupDefaultAppearance() {
        defaultTint = BlurWidget.defaultTint(prefs)
        val tintItem = findViewById<CardItemView>(R.id.default_widget_tint)
        val blurItem = findViewById<CardItemView>(R.id.default_widget_blur)
        val blurSlider = findViewById<SeslSeekBar>(R.id.default_widget_blur_slider)
        fun refreshSummaries() {
            tintItem.summary = "#${BlurWidget.tintColor(defaultTint.hue, defaultTint.saturation, defaultTint.value, defaultTint.alpha).toUInt().toString(16).uppercase().padStart(8, '0')}"
            blurItem.summary = "${(defaultTint.alpha * 100 / 254)}% opacity"
            blurSlider.progress = defaultTint.alpha - 1
        }
        tintItem.setOnClickListener {
            SeslColorPickerDialog(this, { color ->
                val hsv = FloatArray(3)
                Color.colorToHSV(color, hsv)
                defaultTint = defaultTint.copy(hue = hsv[0], saturation = hsv[1], value = hsv[2])
                BlurWidget.saveDefaultTint(prefs, defaultTint)
                refreshSummaries()
                refreshWidgets()
            }, BlurWidget.tintColor(defaultTint.hue, defaultTint.saturation, defaultTint.value, defaultTint.alpha), intArrayOf(), true).show()
        }
        blurSlider.max = 253
        blurSlider.setOnSeekBarChangeListener(object : SeslSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeslSeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                defaultTint = defaultTint.copy(alpha = progress + 1)
                BlurWidget.saveDefaultTint(prefs, defaultTint)
                blurItem.summary = "${(defaultTint.alpha * 100 / 254)}% opacity"
                refreshWidgets()
            }
            override fun onStartTrackingTouch(seekBar: SeslSeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeslSeekBar) = Unit
        })
        refreshSummaries()
    }

    private fun refreshWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        widgetProviders.forEach { provider ->
            val ids = manager.getAppWidgetIds(ComponentName(this, provider))
            if (ids.isNotEmpty()) {
                sendBroadcast(android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(ComponentName(this, provider))
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        val widgetProviders: List<Class<out AppWidgetProvider>> =
            (ExactWidgetVariants.allProviderClasses + listOf(
                ToggleWidgetV2::class.java,
                ToggleWidgetV3::class.java
            )).distinct()
    }
}

package dev.revilodev.onewidget

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
import dev.revilodev.onewidget.ui.PaletteActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SeslSeekBar
import androidx.picker3.app.SeslColorPickerDialog
import dev.revilodev.onewidget.studio.WidgetStudioActivity
import dev.revilodev.onewidget.studio.developer.DeveloperAccessCode
import dev.revilodev.onewidget.studio.developer.DeveloperModeCoordinator
import dev.revilodev.onewidget.studio.developer.DeveloperModeRepository
import dev.revilodev.onewidget.studio.developer.DeveloperWidgetComponentController
import dev.revilodev.onewidget.studio.developer.DeveloperWidgetUpdateReason
import dev.revilodev.onewidget.widgets.developer.DeveloperWidgetProvider
import dev.oneuiproject.oneui.widget.CardItemView
import dev.oneuiproject.oneui.widget.SwitchItemView

class AppearanceSettingsActivity : PaletteActivity() {
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
        findViewById<SwitchItemView>(R.id.use_system_palette).apply {
            isChecked = dev.revilodev.onewidget.ui.SystemPalette.isEnabled(this@AppearanceSettingsActivity)
            onCheckedChangedListener = { _, checked ->
                dev.revilodev.onewidget.ui.SystemPalette.setEnabled(this@AppearanceSettingsActivity, checked)
                recreate()
            }
        }
        prefs = getSharedPreferences(BlurWidget.WIDGET_PREFS, MODE_PRIVATE)
        developerModeRepository = DeveloperModeRepository(this)
        developerModeCoordinator = DeveloperModeCoordinator(
            developerModeRepository,
            DeveloperWidgetComponentController(this)
        ).also { it.synchronize() }
        setupDefaultAppearance()
        findViewById<SwitchItemView>(R.id.global_borders).apply {
            isChecked = prefs.getBoolean("global_widget_borders", true)
            onCheckedChangedListener = { _, checked ->
                prefs.edit().putBoolean("global_widget_borders", checked).apply()
                setupBorderStyle()
                refreshWidgets()
            }
        }
        findViewById<SwitchItemView>(R.id.global_gradients).apply {
            isChecked = prefs.getBoolean("global_widget_gradients", true)
            onCheckedChangedListener = { _, checked -> prefs.edit().putBoolean("global_widget_gradients", checked).apply(); refreshWidgets() }
        }
        setupBorderStyle()
        setupPresets()
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

    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshAction = Runnable { dev.revilodev.onewidget.widgets.WidgetUpdates.requestAll(this) }

    private fun refreshWidgets() {
        refreshHandler.removeCallbacks(refreshAction)
        refreshHandler.postDelayed(refreshAction, 250)
    }

    override fun onStop() {
        super.onStop()
        refreshHandler.removeCallbacks(refreshAction)
        dev.revilodev.onewidget.widgets.WidgetUpdates.requestAll(this)
    }

    private fun setupBorderStyle() {
        val item = findViewById<CardItemView>(R.id.global_border_style)
        val style = dev.revilodev.onewidget.widgets.WidgetAppearance.defaultBorder(prefs)
        item.summary = dev.revilodev.onewidget.widgets.WidgetAppearanceDialogs.borderSummary(style)
        item.setOnClickListener {
            dev.revilodev.onewidget.widgets.WidgetAppearanceDialogs.border(this,
                dev.revilodev.onewidget.widgets.WidgetAppearance.defaultBorder(prefs), { border ->
                    prefs.edit().putBoolean("global_widget_borders", border.enabled)
                        .putInt("global_border_thickness_px", border.thicknessPx)
                        .putInt("global_border_opacity", border.opacityPercent).apply()
                    findViewById<SwitchItemView>(R.id.global_borders).isChecked = border.enabled
                    setupBorderStyle()
                    refreshWidgets()
                })
        }
    }

    private fun setupPresets() {
        val container = findViewById<android.widget.LinearLayout>(R.id.appearance_presets)
        container.removeAllViews()
        val presets = dev.revilodev.onewidget.widgets.WidgetPresets.load(prefs)
        presets.forEach { preset ->
            container.addView(CardItemView(this).apply {
                title = preset.name
                summary = (preset.alpha * 100 / 254).toString() + "% opacity · " +
                    dev.revilodev.onewidget.widgets.WidgetAppearanceDialogs.borderSummary(preset.border)
                setOnClickListener {
                    dev.revilodev.onewidget.widgets.WidgetAppearanceDialogs.preset(this@AppearanceSettingsActivity, preset,
                        { edited ->
                            dev.revilodev.onewidget.widgets.WidgetPresets.save(prefs, presets.map { if (it.id == edited.id) edited else it })
                            setupPresets()
                            refreshWidgets()
                        }, {
                            dev.revilodev.onewidget.widgets.WidgetPresets.save(prefs, presets.filterNot { it.id == preset.id })
                            setupPresets()
                            refreshWidgets()
                        })
                }
            })
        }
        if (presets.size < 24) container.addView(CardItemView(this).apply {
            title = "Add preset"
            setOnClickListener {
                dev.revilodev.onewidget.widgets.WidgetAppearanceDialogs.preset(this@AppearanceSettingsActivity,
                    dev.revilodev.onewidget.widgets.WidgetPresets.newPreset(), { preset ->
                        dev.revilodev.onewidget.widgets.WidgetPresets.save(prefs, presets + preset)
                        setupPresets()
                    })
            }
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

}

package com.example.blurwidgetdemo.widgets.clock

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.app.WallpaperManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.picker3.app.SeslColorPickerDialog
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R

class DigitalClockConfigActivity : AppCompatActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var hue = BlurWidget.DEFAULT_TINT_HUE
    private var saturation = BlurWidget.DEFAULT_TINT_SATURATION
    private var value = BlurWidget.DEFAULT_TINT_VALUE
    private var alpha = BlurWidget.DEFAULT_TINT_ALPHA

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_digital_clock_config)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return finish()
        val settings = ClockWidgetPreferences.load(ClockWidgetPreferences.preferences(this), widgetId)
        hue = state?.getFloat("hue") ?: settings.hue
        saturation = state?.getFloat("saturation") ?: settings.saturation
        value = state?.getFloat("value") ?: settings.value
        alpha = state?.getInt("alpha") ?: settings.alpha
        bind(settings.copy(hue = hue, saturation = saturation, value = value, alpha = alpha))
        findViewById<ImageView>(R.id.clock_preview_wallpaper).setImageDrawable(runCatching { WallpaperManager.getInstance(this).drawable }.getOrNull())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat("hue", hue); outState.putFloat("saturation", saturation)
        outState.putFloat("value", value); outState.putInt("alpha", alpha)
    }

    private fun bind(settings: ClockWidgetSettings) {
        findViewById<Switch>(R.id.clock_show_date).isChecked = settings.showDate
        findViewById<Switch>(R.id.clock_show_day).isChecked = settings.showDayOfWeek
        findViewById<Switch>(R.id.clock_date_above).isChecked = settings.dateAboveTime
        findViewById<Switch>(R.id.clock_background_enabled).isChecked = settings.backgroundEnabled
        bindSpinner(R.id.clock_tap_action, listOf("Open Clock", "Open Calendar"), settings.tapAction.ordinal)
        bindSpinner(R.id.clock_font, ClockFont.entries.map { it.displayName() }, settings.font.ordinal)
        findViewById<RadioGroup>(R.id.clock_time_format).check(when (settings.timeFormat) {
            ClockTimeFormat.SYSTEM -> R.id.format_system; ClockTimeFormat.TWELVE_HOUR -> R.id.format_12; ClockTimeFormat.TWENTY_FOUR_HOUR -> R.id.format_24
        })
        findViewById<RadioGroup>(R.id.clock_alignment).check(when (settings.alignment) {
            ClockTextAlignment.START -> R.id.align_start; ClockTextAlignment.CENTER -> R.id.align_center; ClockTextAlignment.END -> R.id.align_end
        })
        findViewById<RadioGroup>(R.id.clock_opacity).check(when (alpha) {
            in 0..70 -> R.id.opacity_glass; in 71..180 -> R.id.opacity_default; else -> R.id.opacity_solid
        })
        findViewById<RadioGroup>(R.id.clock_opacity).setOnCheckedChangeListener { _, checked ->
            alpha = when (checked) { R.id.opacity_glass -> 38; R.id.opacity_solid -> 240; else -> BlurWidget.DEFAULT_TINT_ALPHA }
            updateColourButton()
        }
        findViewById<Button>(R.id.clock_colour).setOnClickListener { showColourPicker() }
        findViewById<View>(R.id.clock_cancel).setOnClickListener { finish() }
        findViewById<View>(R.id.clock_save).setOnClickListener { save() }
        updateColourButton()
        updatePreview()
    }

    private fun showColourPicker() {
        SeslColorPickerDialog(this, { color ->
            val hsv = FloatArray(3); Color.colorToHSV(color, hsv)
            hue = hsv[0]; saturation = hsv[1]; value = hsv[2]; alpha = Color.alpha(color).coerceIn(1, 254)
            findViewById<RadioGroup>(R.id.clock_opacity).clearCheck(); updateColourButton()
        }, BlurWidget.tintColor(hue, saturation, value, alpha), intArrayOf(), true).apply {
            setTransparencyControlEnabled(true); show()
        }
    }

    private fun updateColourButton() {
        findViewById<Button>(R.id.clock_colour).text = getString(R.string.clock_choose_colour) + "  #" +
            BlurWidget.tintColor(hue, saturation, value, alpha).toUInt().toString(16).uppercase().padStart(8, '0')
        updatePreview()
    }

    private fun updatePreview() {
        findViewById<View>(R.id.clock_preview_widget).setBackgroundColor(BlurWidget.tintColor(hue, saturation, value, alpha))
        val date = findViewById<TextView>(R.id.clock_preview_date)
        date.visibility = if (findViewById<Switch>(R.id.clock_show_date).isChecked) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.clock_preview_time).gravity = when (findViewById<RadioGroup>(R.id.clock_alignment).checkedRadioButtonId) {
            R.id.align_start -> android.view.Gravity.START; R.id.align_end -> android.view.Gravity.END; else -> android.view.Gravity.CENTER_HORIZONTAL
        }
    }

    private fun save() {
        val settings = ClockWidgetSettings(hue, saturation, value, alpha,
            findViewById<Switch>(R.id.clock_show_date).isChecked,
            findViewById<Switch>(R.id.clock_show_day).isChecked,
            findViewById<Switch>(R.id.clock_date_above).isChecked,
            findViewById<Switch>(R.id.clock_background_enabled).isChecked,
            when (findViewById<RadioGroup>(R.id.clock_time_format).checkedRadioButtonId) {
                R.id.format_12 -> ClockTimeFormat.TWELVE_HOUR; R.id.format_24 -> ClockTimeFormat.TWENTY_FOUR_HOUR; else -> ClockTimeFormat.SYSTEM
            },
            when (findViewById<RadioGroup>(R.id.clock_alignment).checkedRadioButtonId) {
                R.id.align_start -> ClockTextAlignment.START; R.id.align_end -> ClockTextAlignment.END; else -> ClockTextAlignment.CENTER
            }, ClockTapAction.entries[findViewById<Spinner>(R.id.clock_tap_action).selectedItemPosition], ClockFont.entries[findViewById<Spinner>(R.id.clock_font).selectedItemPosition])
        ClockWidgetPreferences.save(ClockWidgetPreferences.preferences(this), widgetId, settings)
        DigitalClockWidget.updateWidget(this, AppWidgetManager.getInstance(this), widgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)); finish()
    }

    private fun bindSpinner(id: Int, values: List<String>, selection: Int) {
        val adapter = ArrayAdapter(this, R.layout.clock_spinner_item, values)
        adapter.setDropDownViewResource(R.layout.clock_spinner_item)
        findViewById<Spinner>(id).adapter = adapter
        findViewById<Spinner>(id).setSelection(selection)
    }

    private fun ClockFont.displayName(): String = when (this) {
        ClockFont.SYSTEM -> "System default"; ClockFont.SAMSUNG_DEFAULT -> "Samsung Number Default"; ClockFont.SAMSUNG_DEFAULT_BOLD -> "Samsung Number Default Bold"; ClockFont.SAMSUNG_DEFAULT_THIN -> "Samsung Number Default Thin"; ClockFont.SAMSUNG_MONO -> "Samsung Number Mono"; ClockFont.CLOCK_BOLD_SERIF -> "Clock Bold Serif"; ClockFont.CLOCK_STRIPE -> "Clock Retro Stripe"; ClockFont.CLOCK_STAMP -> "Clock Stamp"; ClockFont.ALATSI -> "Alatsi"; ClockFont.CAPRIOLA -> "Capriola"; ClockFont.FREDERICKA -> "Fredericka the Great"; ClockFont.LATO -> "Lato"; ClockFont.STARDOS_STENCIL -> "Stardos Stencil"; ClockFont.MODAK -> "Modak"
    }
}

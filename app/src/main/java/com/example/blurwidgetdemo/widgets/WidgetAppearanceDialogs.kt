package dev.revilodev.onewidget.widgets

import android.content.Context
import android.graphics.Color
import android.text.InputFilter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SeslSeekBar
import androidx.picker3.app.SeslColorPickerDialog
import dev.oneuiproject.oneui.widget.CardItemView
import dev.oneuiproject.oneui.widget.SwitchItemView

object WidgetAppearanceDialogs {
    private fun column(context: Context) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val padding = (20 * resources.displayMetrics.density).toInt()
        setPadding(padding, 0, padding, padding)
    }

    private fun slider(context: Context, parent: LinearLayout, label: String, min: Int, max: Int, value: Int, changed: (Int) -> Unit) {
        val summary = TextView(context).apply { text = label + ": " + value }
        parent.addView(summary)
        parent.addView(SeslSeekBar(context).apply {
            this.max = max - min
            progress = value.coerceIn(min, max) - min
            contentDescription = label
            setOnSeekBarChangeListener(object : SeslSeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeslSeekBar, progress: Int, fromUser: Boolean) {
                    summary.text = label + ": " + (progress + min)
                    if (fromUser) changed(progress + min)
                }
                override fun onStartTrackingTouch(seekBar: SeslSeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeslSeekBar) = Unit
            })
        })
    }

    fun border(context: Context, initial: WidgetBorderStyle, onSave: (WidgetBorderStyle) -> Unit, onDefault: (() -> Unit)? = null) {
        var style = initial.normalized()
        val content = column(context)
        content.addView(SwitchItemView(context).apply {
            title = "Enable borders"
            isChecked = style.enabled
            onCheckedChangedListener = { _, checked -> style = style.copy(enabled = checked) }
        })
        slider(context, content, "Thickness in pixels", 1, 16, style.thicknessPx) { style = style.copy(thicknessPx = it) }
        slider(context, content, "White opacity percent", 0, 100, style.opacityPercent) { style = style.copy(opacityPercent = it) }
        val builder = AlertDialog.Builder(context).setTitle("Widget border").setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Save") { _, _ -> onSave(style) }
        if (onDefault != null) builder.setNeutralButton("Use defaults") { _, _ -> onDefault() }
        builder.show()
    }

    fun preset(context: Context, initial: WidgetPreset, onSave: (WidgetPreset) -> Unit, onDelete: (() -> Unit)? = null) {
        var preset = initial
        val content = column(context)
        val name = EditText(context).apply {
            setText(initial.name)
            hint = "Preset name"
            isSingleLine = true
            filters = arrayOf(InputFilter.LengthFilter(40))
            contentDescription = "Preset name"
        }
        content.addView(name)
        val tint = CardItemView(context).apply { title = "Tint colour"; summary = "Choose colour" }
        tint.setOnClickListener {
            SeslColorPickerDialog(context, { color ->
                val hsv = FloatArray(3)
                Color.colorToHSV(color, hsv)
                preset = preset.copy(hue = hsv[0], saturation = hsv[1], value = hsv[2])
                tint.summary = "#" + (color and 0x00ffffff).toString(16).uppercase().padStart(6, '0')
            }, preset.color(), intArrayOf(preset.color()), false).show()
        }
        content.addView(tint)
        slider(context, content, "Background opacity percent", 1, 100, (preset.alpha * 100 / 254).coerceAtLeast(1)) {
            preset = preset.copy(alpha = (it * 254 / 100).coerceAtLeast(1))
        }
        content.addView(CardItemView(context).apply {
            title = "Border style"
            summary = borderSummary(preset.border)
            setOnClickListener { border(context, preset.border, { style -> preset = preset.copy(border = style); summary = borderSummary(style) }) }
        })
        val builder = AlertDialog.Builder(context).setTitle("Edit preset").setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Save") { _, _ -> onSave(preset.copy(name = name.text.toString().trim().ifBlank { "Preset" })) }
        if (onDelete != null) builder.setNeutralButton("Delete") { _, _ -> onDelete() }
        builder.show()
    }

    fun borderSummary(style: WidgetBorderStyle) = if (style.enabled) "White · " + style.thicknessPx + " px · " + style.opacityPercent + "% opacity" else "Off"
}

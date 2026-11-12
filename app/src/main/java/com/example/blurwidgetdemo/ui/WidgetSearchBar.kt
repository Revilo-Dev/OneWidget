package dev.revilodev.onewidget.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import dev.revilodev.onewidget.R
import kotlin.math.roundToInt

// uses the toolkit floating search timing with a fixed leading icon
class WidgetSearchBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val diameter = dp(56)
    private val margin = dp(16)
    private val keyboard = context.getSystemService(InputMethodManager::class.java)
    private var morph: ValueAnimator? = null
    var isOpen = false
        private set
    var onOpenChanged: (Boolean) -> Unit = {}
    var onQueryChanged: (String) -> Unit = {}

    private val trigger = ImageButton(context).apply {
        setImageResource(R.drawable.ic_widget_search)
        contentDescription = context.getString(R.string.search_widgets)
        scaleType = ImageView.ScaleType.CENTER
        background = selectableBackground()
        setOnClickListener { setOpen(true) }
    }
    private val input = EditText(context).apply {
        setSingleLine()
        hint = context.getString(R.string.search_widgets)
        setTextSize(16f)
        setPadding(0, 0, 0, 0)
        setBackgroundColor(Color.TRANSPARENT)
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        visibility = GONE
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                onQueryChanged(text?.toString().orEmpty())
            }
            override fun afterTextChanged(text: Editable?) = Unit
        })
        setOnEditorActionListener { _, action, _ ->
            if (action != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
            keyboard.hideSoftInputFromWindow(windowToken, 0)
            true
        }
    }
    private val close = ImageButton(context).apply {
        setImageResource(R.drawable.ic_widget_search_close)
        contentDescription = context.getString(R.string.close_widget_search)
        scaleType = ImageView.ScaleType.CENTER
        background = selectableBackground()
        visibility = GONE
        setOnClickListener { setOpen(false) }
    }

    val query: String get() = input.text.toString()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipToOutline = true
        val density = resources.displayMetrics.density
        background = HighlightedSurfaceDrawable(
            ContextCompat.getColor(context, R.color.oneui_card_bg), diameter / 2f,
            SurfaceHighlight.chromeWidth(density)
        )
        addView(trigger, LayoutParams(diameter, diameter))
        addView(input, LayoutParams(0, dp(48), 1f))
        addView(close, LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(4) })
    }

    fun restoreQuery(query: String) {
        setOpen(true, animate = false)
        input.setText(query)
        input.setSelection(input.length())
    }

    fun setOpen(open: Boolean, animate: Boolean = true) {
        if (open == isOpen) return
        isOpen = open
        morph?.cancel()
        onOpenChanged(open)
        trigger.isClickable = !open
        trigger.isFocusable = !open
        trigger.importantForAccessibility = if (open) IMPORTANT_FOR_ACCESSIBILITY_NO else IMPORTANT_FOR_ACCESSIBILITY_AUTO

        if (!open) {
            keyboard.hideSoftInputFromWindow(input.windowToken, 0)
            input.clearFocus()
            input.text.clear()
            input.visibility = GONE
            close.visibility = GONE
        }
        if (!animate) {
            setBarWidth(if (open) ViewGroup.LayoutParams.MATCH_PARENT else diameter)
            if (open) revealInput(false)
            return
        }

        val host = parent as ViewGroup
        val target = if (open) (host.width - margin * 2).coerceAtLeast(diameter) else diameter
        morph = ValueAnimator.ofInt(width.coerceAtLeast(diameter), target).apply {
            duration = if (open) 180L else 130L
            addUpdateListener {
                setBarWidth(it.animatedValue as Int)
                if (open && it.animatedFraction >= 0.5f && input.visibility != VISIBLE) {
                    revealInput(false)
                }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (morph !== animation || isOpen != open) return
                    morph = null
                    if (open) {
                        setBarWidth(ViewGroup.LayoutParams.MATCH_PARENT)
                        revealInput(true)
                    }
                }
            })
        }
        morph?.start()
    }

    private fun setBarWidth(newWidth: Int) {
        if (layoutParams.width != newWidth) {
            layoutParams = layoutParams.apply { width = newWidth }
        }
    }

    private fun revealInput(focus: Boolean) {
        input.visibility = VISIBLE
        close.visibility = VISIBLE
        if (focus) {
            input.requestFocus()
            input.post { if (isOpen && input.hasFocus()) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT) }
        }
    }

    private fun selectableBackground() =
        context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).let { values ->
            try { values.getDrawable(0) } finally { values.recycle() }
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}

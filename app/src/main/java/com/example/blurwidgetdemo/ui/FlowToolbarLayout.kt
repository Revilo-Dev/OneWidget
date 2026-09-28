package dev.revilodev.onewidget.ui

import android.content.Context
import android.util.AttributeSet
import dev.oneuiproject.oneui.layout.ToolbarLayout

/** Keeps One UI navigation, menus and inset handling, sharing Flow's presentation. */
class FlowToolbarLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ToolbarLayout(context, attrs) {
    override fun onFinishInflate() {
        super.onFinishInflate()
        mainRoundedCorners = MainRoundedCorners.NONE
        FlowToolbar.install(this, appBarLayout, toolbar)
    }
}

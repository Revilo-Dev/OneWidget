package dev.revilodev.onewidget.ui

import android.content.Context
import android.util.AttributeSet
import dev.oneuiproject.oneui.layout.ToolbarLayout

class FlowToolbarLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ToolbarLayout(context, attrs) {
    fun hideCollapsedTitle() {
        fun visit(view: android.view.View) {
            if (view is com.google.android.material.appbar.CollapsingToolbarLayout) {
                view.setCollapsedTitleTextColor(android.graphics.Color.TRANSPARENT)
            }
            if (view is android.view.ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
        }
        visit(appBarLayout)
        toolbar.title = null
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mainRoundedCorners = MainRoundedCorners.NONE
        descendants().filterIsInstance<dev.oneuiproject.oneui.widget.RoundedLinearLayout>()
            .forEach { it.applyCardHighlight() }
        FlowToolbar.install(this, appBarLayout, toolbar)
    }
}

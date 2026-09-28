package dev.revilodev.onewidget.ui

/** A return-to-top gesture must end before it can open the large header. */
class FlowScrollState {
    private var canExpandThisGesture = true
    fun beginGesture(canScrollUp: Boolean) { canExpandThisGesture = !canScrollUp }
    fun contentScrolled(canScrollUp: Boolean) {
        if (canScrollUp) canExpandThisGesture = false
    }
    fun mayExpand(canScrollUp: Boolean, touch: Boolean) = touch && canExpandThisGesture && !canScrollUp

    companion object {
        fun showControlSurfaces(collapsed: Boolean, canScrollUp: Boolean) = collapsed && canScrollUp
    }
}

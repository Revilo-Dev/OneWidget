package com.google.android.material.appbar

import android.view.View

/** Package bridge for Material's package-private overlap hook. Keep overlap when collapsed. */
class FlowContentBehavior(overlap: Int) : AppBarLayout.ScrollingViewBehavior() {
    init { overlayTop = overlap }
    override fun getOverlapRatioForOffset(header: View): Float = 1f
    // Overlapping the toolbar must not leave a toolbar-sized gap at the bottom.
    override fun getScrollRange(header: View): Int = super.getScrollRange(header) + overlayTop
}

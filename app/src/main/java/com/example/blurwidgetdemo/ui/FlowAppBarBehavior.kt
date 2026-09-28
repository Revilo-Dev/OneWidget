package dev.revilodev.onewidget.ui

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout

/** Keep header collapse independent of list position, including a fling reaching the top. */
class FlowAppBarBehavior : AppBarLayout.Behavior() {
    private fun mayExpand(target: View, type: Int): Boolean =
        (target as? FlowScrollView)?.gesture?.mayExpand(
            target.canScrollVertically(-1), type == ViewCompat.TYPE_TOUCH
        ) ?: false

    override fun onNestedPreScroll(parent: CoordinatorLayout, child: AppBarLayout,
        target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (dy < 0 && !mayExpand(target, type)) return
        super.onNestedPreScroll(parent, child, target, dx, dy, consumed, type)
    }

    override fun onNestedScroll(parent: CoordinatorLayout, child: AppBarLayout,
        target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, type: Int, consumed: IntArray) {
        if (dyUnconsumed < 0 && !mayExpand(target, type)) return
        super.onNestedScroll(parent, child, target, dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed, type, consumed)
    }
}


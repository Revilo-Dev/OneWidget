package com.example.blurwidgetdemo.widgets.clock

/** Size policy shared by the provider and unit tests; adjust thresholds here only. */
object ClockWidgetLayout {
    const val COMPACT_MAX_HEIGHT_DP = 100
    const val MEDIUM_MIN_WIDTH_DP = 250
    const val LARGE_MIN_WIDTH_DP = 330
    const val LARGE_MIN_HEIGHT_DP = 150

    enum class Category { COMPACT, MEDIUM, LARGE }

    fun category(minWidthDp: Int, minHeightDp: Int): Category = when {
        // Short strips and narrow squares need the compact, single-line treatment.
        minHeightDp <= COMPACT_MAX_HEIGHT_DP || minWidthDp < MEDIUM_MIN_WIDTH_DP -> Category.COMPACT
        minWidthDp >= LARGE_MIN_WIDTH_DP || minHeightDp >= LARGE_MIN_HEIGHT_DP -> Category.LARGE
        else -> Category.MEDIUM
    }
}

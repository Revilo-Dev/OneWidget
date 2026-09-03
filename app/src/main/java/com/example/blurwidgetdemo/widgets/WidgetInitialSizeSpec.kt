package com.example.blurwidgetdemo.widgets

/**
 * App-owned record of a gallery selection. The launcher remains responsible for the eventual
 * widget bounds, which providers read from AppWidgetOptions after placement.
 */
data class WidgetInitialSizeSpec(
    val columns: Int,
    val rows: Int,
    val minWidthDp: Int,
    val minHeightDp: Int,
    val targetCellWidth: Int = columns,
    val targetCellHeight: Int = rows,
    val providerMetadataSize: String
) {
    val stableId: String = "${columns}x${rows}"

    companion object {
        const val EXTRA_COLUMNS = "com.example.blurwidgetdemo.extra.REQUESTED_COLUMNS"
        const val EXTRA_ROWS = "com.example.blurwidgetdemo.extra.REQUESTED_ROWS"

        fun grid(columns: Int, rows: Int): WidgetInitialSizeSpec {
            require(columns > 0 && rows > 0) { "Widget grid dimensions must be positive" }
            return WidgetInitialSizeSpec(
                columns = columns,
                rows = rows,
                minWidthDp = 72 + (columns - 1) * 90,
                minHeightDp = 76 + (rows - 1) * 100,
                providerMetadataSize = when {
                    columns == 1 && rows == 1 -> "small"
                    rows == 1 -> "wideSmall"
                    columns <= 2 -> "medium"
                    else -> "large"
                }
            )
        }
    }
}

package dev.revilodev.onewidget.widgets

import android.appwidget.AppWidgetManager

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
        const val EXTRA_COLUMNS = "dev.revilodev.onewidget.extra.REQUESTED_COLUMNS"
        const val EXTRA_ROWS = "dev.revilodev.onewidget.extra.REQUESTED_ROWS"

        /**
         * Samsung Home can deliver the first provider update before populating widget options.
         * In that window, use the selected provider's declared minimum so an exact-size alias
         * still renders at its own size instead of falling back to a generic large layout.
         */
        fun resolvedBounds(
            manager: AppWidgetManager,
            appWidgetId: Int,
            fallbackWidthDp: Int,
            fallbackHeightDp: Int
        ): Pair<Int, Int> {
            val options = manager.getAppWidgetOptions(appWidgetId)
            val provider = manager.getAppWidgetInfo(appWidgetId)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                .takeIf { it > 0 } ?: provider?.minWidth?.takeIf { it > 0 } ?: fallbackWidthDp
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                .takeIf { it > 0 } ?: provider?.minHeight?.takeIf { it > 0 } ?: fallbackHeightDp
            return width to height
        }

        fun grid(columns: Int, rows: Int): WidgetInitialSizeSpec {
            require(columns > 0 && rows > 0) { "Widget grid dimensions must be positive" }
            return WidgetInitialSizeSpec(
                columns = columns,
                rows = rows,
                minWidthDp = 70 * columns - 30,
                minHeightDp = 70 * rows - 30,
                providerMetadataSize = when {
                    columns == 1 && rows == 1 -> "tiny"
                    columns == 2 && rows == 1 -> "small"
                    rows == 1 -> "wideSmall"
                    columns <= 2 -> "medium"
                    rows >= 5 -> "extraLargeLong"
                    rows >= 4 -> "extraLarge"
                    else -> "large"
                }
            )
        }
    }
}

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
            val options = renderOptions(manager, appWidgetId)
            val provider = manager.getAppWidgetInfo(appWidgetId)
            val density = android.content.res.Resources.getSystem().displayMetrics.density
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                .takeIf { it > 0 } ?: provider?.minWidth?.takeIf { it > 0 }?.let { (it / density).toInt() } ?: fallbackWidthDp
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                .takeIf { it > 0 } ?: provider?.minHeight?.takeIf { it > 0 }?.let { (it / density).toInt() } ?: fallbackHeightDp
            return width.coerceAtLeast(1) to height.coerceAtLeast(1)
        }

        fun renderOptions(manager: AppWidgetManager, appWidgetId: Int): android.os.Bundle {
            val options = android.os.Bundle(manager.getAppWidgetOptions(appWidgetId))
            val landscape = android.content.res.Resources.getSystem().configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
            @Suppress("DEPRECATION")
            val sizes = if (android.os.Build.VERSION.SDK_INT >= 31)
                options.getParcelableArrayList<android.util.SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES).orEmpty()
            else emptyList()
            val size = if (landscape) sizes.maxByOrNull { it.width } else sizes.maxByOrNull { it.height }
            val width = size?.width?.toInt() ?: options.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0))
            val height = size?.height?.toInt() ?: options.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0))
            val info = if (width <= 0 || height <= 0) manager.getAppWidgetInfo(appWidgetId) else null
            val density = android.content.res.Resources.getSystem().displayMetrics.density
            val resolvedWidth = if (width > 0) width else info?.minWidth?.let { (it / density).toInt() } ?: 0
            val resolvedHeight = if (height > 0) height else info?.minHeight?.let { (it / density).toInt() } ?: 0
            if (resolvedWidth > 0) options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, resolvedWidth)
            if (resolvedHeight > 0) options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, resolvedHeight)
            return options
        }

        fun launcherGridSpan(options: android.os.Bundle): android.util.Size? {
            val columns = options.getInt("semAppWidgetColumnSpan")
            val rows = options.getInt("semAppWidgetRowSpan")
            return if (columns > 0 && rows > 0) android.util.Size(columns, rows) else null
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

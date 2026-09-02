package com.example.blurwidgetdemo.studio.developer

import android.appwidget.AppWidgetManager
import android.content.res.Configuration
import android.os.Bundle

@ConsistentCopyVisibility
data class DeveloperWidgetHostSize private constructor(
    val minWidthDp: Int,
    val minHeightDp: Int,
    val maxWidthDp: Int,
    val maxHeightDp: Int,
    val preferredWidthDp: Float,
    val preferredHeightDp: Float
) {
    companion object {
        const val DEFAULT_WIDTH_DP = 110
        const val DEFAULT_HEIGHT_DP = 40

        /**
         * One UI reports a cross-orientation range. Portrait uses the narrower width and taller
         * height; landscape uses the wider width and shorter height. A missing value falls back
         * to its opposite bound, then the legacy clock defaults. All exposed values are positive.
         */
        fun normalize(
            minWidthDp: Int,
            minHeightDp: Int,
            maxWidthDp: Int,
            maxHeightDp: Int,
            isPortrait: Boolean = true
        ): DeveloperWidgetHostSize {
            val normalizedMinWidth = positive(minWidthDp, maxWidthDp, DEFAULT_WIDTH_DP)
            val normalizedMinHeight = positive(minHeightDp, maxHeightDp, DEFAULT_HEIGHT_DP)
            val normalizedMaxWidth = positive(maxWidthDp, minWidthDp, normalizedMinWidth)
            val normalizedMaxHeight = positive(maxHeightDp, minHeightDp, normalizedMinHeight)
            val preferredWidth = if (isPortrait) normalizedMinWidth else normalizedMaxWidth
            val preferredHeight = if (isPortrait) normalizedMaxHeight else normalizedMinHeight
            return DeveloperWidgetHostSize(
                minWidthDp = normalizedMinWidth,
                minHeightDp = normalizedMinHeight,
                maxWidthDp = normalizedMaxWidth,
                maxHeightDp = normalizedMaxHeight,
                preferredWidthDp = preferredWidth.toFloat(),
                preferredHeightDp = preferredHeight.toFloat()
            )
        }

        fun fromPersisted(
            minWidthDp: Int,
            minHeightDp: Int,
            maxWidthDp: Int,
            maxHeightDp: Int,
            preferredWidthDp: Float,
            preferredHeightDp: Float
        ): DeveloperWidgetHostSize {
            val normalized = normalize(minWidthDp, minHeightDp, maxWidthDp, maxHeightDp)
            val width = preferredWidthDp.takeIf { it.isFinite() && it > 0f }
                ?: normalized.preferredWidthDp
            val height = preferredHeightDp.takeIf { it.isFinite() && it > 0f }
                ?: normalized.preferredHeightDp
            return normalized.copy(preferredWidthDp = width, preferredHeightDp = height)
        }

        private fun positive(primary: Int, secondary: Int, fallback: Int): Int = when {
            primary > 0 -> primary
            secondary > 0 -> secondary
            else -> fallback
        }
    }
}

data class DeveloperWidgetHostSizeRead(
    val size: DeveloperWidgetHostSize,
    val launcherValuesPresent: Boolean
)

object DeveloperWidgetHostSizeResolver {
    fun fromOptions(options: Bundle, orientation: Int): DeveloperWidgetHostSizeRead = fromValues(
        minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
        minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
        maxWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0),
        maxHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0),
        isPortrait = orientation != Configuration.ORIENTATION_LANDSCAPE
    )

    fun fromValues(
        minWidthDp: Int,
        minHeightDp: Int,
        maxWidthDp: Int,
        maxHeightDp: Int,
        isPortrait: Boolean = true
    ): DeveloperWidgetHostSizeRead {
        return DeveloperWidgetHostSizeRead(
            size = DeveloperWidgetHostSize.normalize(
                minWidthDp,
                minHeightDp,
                maxWidthDp,
                maxHeightDp,
                isPortrait
            ),
            launcherValuesPresent = listOf(
                minWidthDp,
                minHeightDp,
                maxWidthDp,
                maxHeightDp
            ).any { it > 0 }
        )
    }
}

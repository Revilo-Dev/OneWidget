package dev.revilodev.onewidget.studio.canvas

import dev.revilodev.onewidget.studio.developer.DeveloperWidgetHostSize
import dev.revilodev.onewidget.studio.model.WidgetVariantId

enum class StudioCanvasSizeSource {
    APPROXIMATE_GRID,
    DEVICE_CAPTURED,
    CUSTOM
}
class StudioCanvasSizingController(
    private val previewMetrics: StudioPreviewMetrics = StudioPreviewMetrics()
) {
    var source: StudioCanvasSizeSource = StudioCanvasSizeSource.APPROXIMATE_GRID
        private set

    private var explicitSize: StudioCanvasSize? = null

    fun sizeFor(variantId: WidgetVariantId.Standard): StudioCanvasSize =
        explicitSize.takeIf { source != StudioCanvasSizeSource.APPROXIMATE_GRID }
            ?: previewMetrics.canvasSize(variantId)

    fun useApproximateGrid() {
        source = StudioCanvasSizeSource.APPROXIMATE_GRID
        explicitSize = null
    }

    fun useDeviceSize(hostSize: DeveloperWidgetHostSize) {
        source = StudioCanvasSizeSource.DEVICE_CAPTURED
        explicitSize = StudioCanvasSize(
            hostSize.preferredWidthDp,
            hostSize.preferredHeightDp
        )
    }

    fun useCustomSize(widthDp: Float, heightDp: Float) {
        require(widthDp.isFinite() && widthDp > 0f) { "Custom width must be positive and finite" }
        require(heightDp.isFinite() && heightDp > 0f) { "Custom height must be positive and finite" }
        source = StudioCanvasSizeSource.CUSTOM
        explicitSize = StudioCanvasSize(widthDp, heightDp)
    }

    fun restore(
        restoredSource: StudioCanvasSizeSource,
        widthDp: Float?,
        heightDp: Float?
    ) {
        when (restoredSource) {
            StudioCanvasSizeSource.APPROXIMATE_GRID -> useApproximateGrid()
            StudioCanvasSizeSource.DEVICE_CAPTURED,
            StudioCanvasSizeSource.CUSTOM -> {
                if (
                    widthDp != null && heightDp != null &&
                    widthDp.isFinite() && heightDp.isFinite() &&
                    widthDp > 0f && heightDp > 0f
                ) {
                    source = restoredSource
                    explicitSize = StudioCanvasSize(widthDp, heightDp)
                } else {
                    useApproximateGrid()
                }
            }
        }
    }
}

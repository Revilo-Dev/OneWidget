package dev.revilodev.onewidget.studio.canvas

import dev.revilodev.onewidget.studio.developer.DeveloperWidgetHostSize
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StudioCanvasSizingControllerTest {
    @Test fun `approximate canvas continues to follow selected variant`() {
        val controller = StudioCanvasSizingController()

        assertEquals(StudioCanvasSize(152f, 72f), controller.sizeFor(WidgetVariantId.Standard(2, 1)))
        assertEquals(StudioCanvasSize(312f, 152f), controller.sizeFor(WidgetVariantId.Standard(4, 2)))
        assertEquals(StudioCanvasSizeSource.APPROXIMATE_GRID, controller.source)
    }

    @Test fun `use device size changes canvas only and keeps variant selection unchanged`() {
        val controller = StudioCanvasSizingController()
        val selectedVariant = WidgetVariantId.Standard(4, 2)
        val hostSize = DeveloperWidgetHostSize.normalize(281, 55, 520, 126)

        controller.useDeviceSize(hostSize)

        assertEquals(StudioCanvasSize(281f, 126f), controller.sizeFor(selectedVariant))
        assertEquals(StudioCanvasSizeSource.DEVICE_CAPTURED, controller.source)
        assertEquals(WidgetVariantId.Standard(4, 2), selectedVariant)
    }

    @Test fun `custom size accepts arbitrary positive finite dimensions`() {
        val controller = StudioCanvasSizingController()
        controller.useCustomSize(1400.5f, 720f)

        assertEquals(
            StudioCanvasSize(1400.5f, 720f),
            controller.sizeFor(WidgetVariantId.Standard(1, 1))
        )
        assertEquals(StudioCanvasSizeSource.CUSTOM, controller.source)
    }

    @Test fun `custom size rejects nonpositive and nonfinite dimensions`() {
        val controller = StudioCanvasSizingController()

        assertThrows(IllegalArgumentException::class.java) { controller.useCustomSize(0f, 100f) }
        assertThrows(IllegalArgumentException::class.java) {
            controller.useCustomSize(100f, Float.POSITIVE_INFINITY)
        }
    }

    @Test fun `invalid restored explicit size safely returns to approximate grid`() {
        val controller = StudioCanvasSizingController()

        controller.restore(StudioCanvasSizeSource.DEVICE_CAPTURED, Float.NaN, 100f)

        assertEquals(StudioCanvasSizeSource.APPROXIMATE_GRID, controller.source)
        assertEquals(StudioCanvasSize(72f, 72f), controller.sizeFor(WidgetVariantId.Standard(1, 1)))
    }
}

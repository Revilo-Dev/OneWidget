package dev.revilodev.onewidget.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSurfaceGeometryTest {
    @Test fun wideTwoRowWidgetsHaveSmallerCornersAcrossGlassSizes() {
        for (columns in 2..7) for (rows in 2..7) {
            val expected = if (columns >= 3 && rows == 2) 20f else 24f
            assertEquals(expected, WidgetSurfaceGeometry.radiusDp(
                (70 * columns - 30).toFloat(), (70 * rows - 30).toFloat(), rows, columns
            ), 0f)
        }
    }

    @Test fun compactSurfacesRoundTheirEndsWithoutExceedingTheirBounds() {
        assertEquals(20f, WidgetSurfaceGeometry.radiusDp(110f, 40f), 0f)
        assertEquals(20f, WidgetSurfaceGeometry.radiusDp(40f, 40f), 0f)
        assertEquals(10f, WidgetSurfaceGeometry.radiusDp(20f, 180f), 0f)
    }

    @Test fun launcherSpanKeepsShortTwoRowWidgetsFromBecomingPills() {
        assertEquals(20f, WidgetSurfaceGeometry.radiusDp(220f, 80f, rowSpan = 2, columnSpan = 4), 0f)
        assertEquals(48f, WidgetSurfaceGeometry.radiusDp(220f, 96f, rowSpan = 1), 0f)
        assertEquals(20f, WidgetSurfaceGeometry.radiusDp(220f, 96f), 0f)
        assertEquals(24f, WidgetSurfaceGeometry.radiusDp(110f, 110f), 0f)
    }

    @Test fun borderStrokeStaysInsideItsBounds() {
        assertEquals(2f, WidgetSurfaceGeometry.strokeInset(4f), 0f)
        assertEquals(8f, WidgetSurfaceGeometry.strokeInset(16f), 0f)
    }

    @Test fun invalidSavedBorderValuesAreBounded() {
        assertEquals(WidgetBorderStyle(false, 1, 100), WidgetBorderStyle(false, -5, 200).normalized())
    }
}

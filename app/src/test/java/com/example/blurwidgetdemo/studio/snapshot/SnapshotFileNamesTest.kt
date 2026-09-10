package dev.revilodev.onewidget.studio.snapshot

import dev.revilodev.onewidget.studio.model.WidgetVariantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotFileNamesTest {
    @Test fun `filename is deterministic safe and includes revision`() {
        val first = SnapshotFileNames.image(
            "digital_clock",
            WidgetVariantId.Standard(4, 2),
            17L
        )
        val second = SnapshotFileNames.image(
            "digital_clock",
            WidgetVariantId.Standard(4, 2),
            17L
        )

        assertEquals("digital_clock__4x2__rev_17.png", first)
        assertEquals(first, second)
        assertTrue(SnapshotFileNames.metadata("digital_clock", WidgetVariantId.Standard(4, 2), 17L)
            .endsWith("__rev_17.json"))
    }

    @Test fun `custom variant is converted to a safe filename segment`() {
        assertEquals(
            "digital_clock__custom_gallery__rev_3.png",
            SnapshotFileNames.image("digital_clock", WidgetVariantId.Custom("gallery"), 3L)
        )
    }

    @Test fun `unsafe widget ID and negative revision are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            SnapshotFileNames.image("../clock", WidgetVariantId.Standard(2, 1), 1L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SnapshotFileNames.image("digital_clock", WidgetVariantId.Standard(2, 1), -1L)
        }
    }
}

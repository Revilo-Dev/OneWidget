package com.example.blurwidgetdemo.studio.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperWidgetHostSizeTest {
    @Test fun `portrait chooses narrow width and tall height from launcher range`() {
        val read = DeveloperWidgetHostSizeResolver.fromValues(246, 118, 492, 236)

        assertTrue(read.launcherValuesPresent)
        assertEquals(246, read.size.minWidthDp)
        assertEquals(118, read.size.minHeightDp)
        assertEquals(492, read.size.maxWidthDp)
        assertEquals(236, read.size.maxHeightDp)
        assertEquals(246f, read.size.preferredWidthDp)
        assertEquals(236f, read.size.preferredHeightDp)
    }

    @Test fun `landscape chooses wide width and short height`() {
        val read = DeveloperWidgetHostSizeResolver.fromValues(
            246,
            118,
            492,
            236,
            isPortrait = false
        )

        assertEquals(492f, read.size.preferredWidthDp)
        assertEquals(118f, read.size.preferredHeightDp)
    }

    @Test fun `missing and nonpositive launcher values normalize to positive defaults`() {
        val read = DeveloperWidgetHostSizeResolver.fromValues(0, -4, 0, 0)

        assertFalse(read.launcherValuesPresent)
        assertEquals(DeveloperWidgetHostSize.DEFAULT_WIDTH_DP, read.size.minWidthDp)
        assertEquals(DeveloperWidgetHostSize.DEFAULT_HEIGHT_DP, read.size.minHeightDp)
        assertTrue(read.size.preferredWidthDp > 0f)
        assertTrue(read.size.preferredHeightDp > 0f)
    }

    @Test fun `one valid bound supplies a missing opposite bound`() {
        val size = DeveloperWidgetHostSize.normalize(0, 70, 310, 0)

        assertEquals(310, size.minWidthDp)
        assertEquals(310, size.maxWidthDp)
        assertEquals(70, size.minHeightDp)
        assertEquals(70, size.maxHeightDp)
    }
}

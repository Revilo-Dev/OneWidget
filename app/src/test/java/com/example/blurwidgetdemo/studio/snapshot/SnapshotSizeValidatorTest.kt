package com.example.blurwidgetdemo.studio.snapshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotSizeValidatorTest {
    @Test fun `normal dp size becomes exact density-scaled pixels`() {
        val result = SnapshotSizeValidator.validate(312f, 152f, 3f)

        assertTrue(result is SnapshotSizeValidation.Valid)
        val pixels = (result as SnapshotSizeValidation.Valid).pixelSize
        assertEquals(936, pixels.widthPx)
        assertEquals(456, pixels.heightPx)
        assertEquals(936L * 456L * 4L, pixels.estimatedBytes)
    }

    @Test fun `zero negative and nonfinite inputs are rejected`() {
        assertTrue(SnapshotSizeValidator.validate(0f, 100f, 3f) is SnapshotSizeValidation.Invalid)
        assertTrue(SnapshotSizeValidator.validate(100f, -1f, 3f) is SnapshotSizeValidation.Invalid)
        assertTrue(
            SnapshotSizeValidator.validate(100f, Float.NaN, 3f) is SnapshotSizeValidation.Invalid
        )
        assertTrue(
            SnapshotSizeValidator.validate(100f, 100f, Float.POSITIVE_INFINITY) is
                SnapshotSizeValidation.Invalid
        )
    }

    @Test fun `extreme bitmap is rejected before allocation`() {
        val result = SnapshotSizeValidator.validate(10_000f, 10_000f, 4f)

        assertTrue(result is SnapshotSizeValidation.Invalid)
    }
}

package com.example.blurwidgetdemo.studio.production

import com.example.blurwidgetdemo.studio.model.WidgetElementOverride
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import com.example.blurwidgetdemo.studio.model.WidgetVariantOverride
import com.example.blurwidgetdemo.studio.studioTestDesign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDesignDiffTest {
    private val variant = WidgetVariantId.Standard(2, 1)
    private val base = studioTestDesign()

    @Test fun `identical designs have no difference`() {
        assertTrue(WidgetDesignDiff.compare(base, base, listOf(variant)).isEmpty())
    }

    @Test fun `visibility position padding and radius changes are reported`() {
        val changed = base.copy(
            variants = mapOf(
                variant to WidgetVariantOverride(
                    mapOf(
                        "test.primary" to WidgetElementOverride(
                            visible = false,
                            xDp = 9f,
                            yDp = 10f,
                            paddingTopDp = 11f,
                            cornerRadiusDp = 12f
                        )
                    )
                )
            )
        )

        val properties = WidgetDesignDiff.compare(base, changed, listOf(variant)).map { it.property }.toSet()

        assertTrue("visible" in properties)
        assertTrue("xDp" in properties)
        assertTrue("yDp" in properties)
        assertTrue("paddingTopDp" in properties)
        assertTrue("cornerRadiusDp" in properties)
        assertEquals(5, properties.size)
    }
}

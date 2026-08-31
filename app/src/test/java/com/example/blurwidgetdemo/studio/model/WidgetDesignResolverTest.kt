package com.example.blurwidgetdemo.studio.model

import com.example.blurwidgetdemo.studio.studioTestDesign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDesignResolverTest {
    private val variant = WidgetVariantId.Standard(2, 1)

    @Test fun `base design resolves unchanged without an override`() {
        val source = studioTestDesign()
        val result = WidgetDesignResolver.resolve(source, variant)
        assertTrue(result.isSuccess)
        assertEquals(source.baseVariant.elements, result.design?.elements)
    }

    @Test fun `one visibility override is applied and other fields inherit`() {
        val source = studioTestDesign().copy(
            variants = mapOf(
                variant to WidgetVariantOverride(
                    mapOf("test.primary" to WidgetElementOverride(visible = false))
                )
            )
        )
        val resolved = WidgetDesignResolver.resolve(source, variant).design
        val primary = resolved?.elements?.first { it.elementId == "test.primary" }
        assertNotNull(primary)
        assertFalse(primary!!.visible)
        assertEquals(4f, primary.xDp)
        assertEquals(120f, primary.widthDp)
    }

    @Test fun `multiple geometry and padding overrides are applied`() {
        val source = studioTestDesign().copy(
            variants = mapOf(
                variant to WidgetVariantOverride(
                    mapOf(
                        "test.primary" to WidgetElementOverride(xDp = 20f, yDp = 30f, paddingTopDp = 9f),
                        "test.secondary" to WidgetElementOverride(anchor = WidgetAnchor.BOTTOM_END)
                    )
                )
            )
        )
        val resolved = WidgetDesignResolver.resolve(source, variant).design!!
        val primary = resolved.elements.first { it.elementId == "test.primary" }
        val secondary = resolved.elements.first { it.elementId == "test.secondary" }
        assertEquals(20f, primary.xDp)
        assertEquals(30f, primary.yDp)
        assertEquals(9f, primary.paddingTopDp)
        assertEquals(WidgetAnchor.BOTTOM_END, secondary.anchor)
    }

    @Test fun `unknown element override is reported and rejected`() {
        val source = studioTestDesign().copy(
            variants = mapOf(
                variant to WidgetVariantOverride(
                    mapOf("missing.element" to WidgetElementOverride(visible = false))
                )
            )
        )
        val result = WidgetDesignResolver.resolve(source, variant)
        assertNull(result.design)
        assertEquals(1, result.issues.size)
        assertTrue(result.issues.single().message.contains("missing.element"))
    }
}

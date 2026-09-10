package dev.revilodev.onewidget.studio.model

import dev.revilodev.onewidget.studio.studioTestDesign
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDesignValidatorTest {
    @Test fun `valid design passes validation`() {
        assertTrue(WidgetDesignValidator.validate(studioTestDesign(), setOf("test.primary")).isValid)
    }

    @Test fun `duplicate element IDs fail validation`() {
        val design = studioTestDesign()
        val duplicate = design.baseVariant.elements.first().copy()
        assertInvalid(design.copy(baseVariant = WidgetVariantSpec(design.baseVariant.elements + duplicate)), "Duplicate")
    }

    @Test fun `NaN and infinity fail validation`() {
        assertInvalid(withPrimary { it.copy(xDp = Float.NaN) }, "finite")
        assertInvalid(withPrimary { it.copy(yDp = Float.POSITIVE_INFINITY) }, "finite")
    }

    @Test fun `negative fixed dimensions fail validation`() {
        assertInvalid(withPrimary { it.copy(widthMode = DimensionMode.FIXED, widthDp = -1f) }, "width")
        assertInvalid(withPrimary { it.copy(heightMode = DimensionMode.FIXED, heightDp = -1f) }, "height")
    }

    @Test fun `negative padding and radius fail validation`() {
        assertInvalid(withPrimary { it.copy(paddingStartDp = -1f) }, "paddingStartDp")
        assertInvalid(withPrimary { it.copy(cornerRadiusDp = -1f) }, "Corner radius")
    }

    @Test fun `unknown override element fails validation`() {
        val variant = WidgetVariantId.Standard(2, 1)
        val design = studioTestDesign().copy(
            variants = mapOf(
                variant to WidgetVariantOverride(
                    mapOf("missing.element" to WidgetElementOverride(xDp = 2f))
                )
            )
        )
        assertInvalid(design, "unknown element")
    }

    @Test fun `missing required descriptor element fails validation`() {
        val result = WidgetDesignValidator.validate(studioTestDesign(), setOf("test.primary", "test.required"))
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.message.contains("test.required") })
    }

    @Test fun `invalid variant and z-index fail validation`() {
        val invalidVariant = WidgetVariantId.Standard(0, 1)
        val design = withPrimary { it.copy(zIndex = -1) }.copy(
            variants = mapOf(invalidVariant to WidgetVariantOverride())
        )
        val result = WidgetDesignValidator.validate(design)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.message.contains("Invalid variant") })
        assertTrue(result.issues.any { it.message.contains("Z-index") })
    }

    private fun withPrimary(transform: (WidgetElementSpec) -> WidgetElementSpec): WidgetDesignSpec {
        val design = studioTestDesign()
        return design.copy(
            baseVariant = WidgetVariantSpec(
                design.baseVariant.elements.map { if (it.elementId == "test.primary") transform(it) else it }
            )
        )
    }

    private fun assertInvalid(design: WidgetDesignSpec, messagePart: String) {
        val result = WidgetDesignValidator.validate(design)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.message.contains(messagePart, ignoreCase = true) || it.field.contains(messagePart, ignoreCase = true) })
    }
}

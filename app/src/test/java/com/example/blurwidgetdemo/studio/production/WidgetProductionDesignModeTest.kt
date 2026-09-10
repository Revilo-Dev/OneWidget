package dev.revilodev.onewidget.studio.production

import dev.revilodev.onewidget.studio.clock.ClockStudioDescriptor
import dev.revilodev.onewidget.studio.storage.StorageStudioDescriptor
import dev.revilodev.onewidget.studio.calendar.CalendarStudioDescriptor
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.verification.WidgetVerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetProductionDesignModeTest {
    private val store = FakeModeStore()
    private val repository = WidgetProductionDesignModeRepository(store)

    @Test fun `existing appWidgetId remains legacy when no mode was initialized`() {
        assertEquals(
            WidgetProductionDesignMode.LEGACY_DEFAULT,
            repository.mode(ClockStudioDescriptor.WIDGET_ID, 41)
        )
    }

    @Test fun `new widget after promotion follows configurable promoted default`() {
        repository.setUsePromotedForNewWidgets(ClockStudioDescriptor.WIDGET_ID, true)

        assertEquals(
            WidgetProductionDesignMode.PROMOTED_DEFAULT,
            repository.initializeNewWidget(ClockStudioDescriptor.WIDGET_ID, 42, promotedDesignExists = true)
        )
        repository.setUsePromotedForNewWidgets(ClockStudioDescriptor.WIDGET_ID, false)
        assertEquals(
            WidgetProductionDesignMode.PROMOTED_DEFAULT,
            repository.initializeNewWidget(ClockStudioDescriptor.WIDGET_ID, 42, promotedDesignExists = true)
        )
    }

    @Test fun `legacy passes null and promoted resolves mapped design`() {
        val design = ClockStudioDescriptor.defaultDesign().copy(revision = 7L)
        val promoted = PromotedWidgetDesign(
            PromotedWidgetDesignMetadata(
                widgetId = design.widgetId,
                designRevision = 7L,
                promotedAt = 1L,
                verificationStatus = WidgetVerificationStatus.VERIFIED,
                sourceDesignHash = "hash"
            ),
            design
        )

        assertNull(
            ProductionWidgetDesignResolver.resolve(
                WidgetProductionDesignMode.LEGACY_DEFAULT,
                promoted,
                design.widgetId,
                180f,
                80f
            )
        )
        val resolved = ProductionWidgetDesignResolver.resolve(
            WidgetProductionDesignMode.PROMOTED_DEFAULT,
            promoted,
            design.widgetId,
            180f,
            80f
        )
        assertNotNull(resolved)
        assertEquals(WidgetVariantId.Standard(2, 1), resolved?.variantId)
    }

    @Test fun `compact medium large and unknown sizes use centralized mapping`() {
        assertEquals(
            ProductionSizeCategory.COMPACT,
            ProductionVariantResolver.resolve("digital_clock", 180f, 80f).category
        )
        assertEquals(
            ProductionSizeCategory.MEDIUM,
            ProductionVariantResolver.resolve("digital_clock", 280f, 120f).category
        )
        assertEquals(
            ProductionSizeCategory.LARGE,
            ProductionVariantResolver.resolve("digital_clock", 350f, 180f).category
        )
        val fallback = ProductionVariantResolver.resolve("digital_clock", 0f, Float.NaN)
        assertTrue(fallback.usedUnknownSizeFallback)
        assertEquals(WidgetVariantId.Standard(2, 2), fallback.variantId)

        assertEquals(
            ProductionSizeCategory.COMPACT,
            ProductionVariantResolver.resolve(StorageStudioDescriptor.WIDGET_ID, 400f, 100f).category
        )
        assertEquals(
            ProductionSizeCategory.MEDIUM,
            ProductionVariantResolver.resolve(StorageStudioDescriptor.WIDGET_ID, 280f, 120f).category
        )
        assertEquals(
            ProductionSizeCategory.LARGE,
            ProductionVariantResolver.resolve(StorageStudioDescriptor.WIDGET_ID, 350f, 180f).category
        )
        assertEquals(
            ProductionSizeCategory.COMPACT,
            ProductionVariantResolver.resolve(CalendarStudioDescriptor.WIDGET_ID, 400f, 100f).category
        )
        assertEquals(
            ProductionSizeCategory.LARGE,
            ProductionVariantResolver.resolve(CalendarStudioDescriptor.WIDGET_ID, 330f, 120f).category
        )
    }

    private class FakeModeStore : ProductionDesignModeStore {
        private val strings = mutableMapOf<String, String>()
        private val booleans = mutableMapOf<String, Boolean>()
        override fun getString(key: String): String? = strings[key]
        override fun putString(key: String, value: String) { strings[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = booleans[key] ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
        override fun remove(key: String) { strings.remove(key) }
    }
}

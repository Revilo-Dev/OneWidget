package com.example.blurwidgetdemo.studio.developer

import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperWidgetLoadCoordinatorTest {
    @Test fun `load saves active spec and updates every distinct developer widget`() {
        val repository = DeveloperWidgetStateRepository(FakeStateStore())
        val updates = mutableListOf<Pair<Int, DeveloperWidgetUpdateReason>>()
        val coordinator = DeveloperWidgetLoadCoordinator(
            stateRepository = repository,
            currentTimeMillis = { 500L },
            listWidgetIds = { intArrayOf(8, 3, 8, 12) },
            updateWidget = { id, reason -> updates += id to reason }
        )

        val result = coordinator.load(
            widgetId = "digital_clock",
            variantId = WidgetVariantId.Standard(2, 1),
            designRevision = 9L
        )

        assertEquals(listOf(3, 8, 12), result.updatedWidgetIds)
        assertEquals(
            listOf(3, 8, 12).map { it to DeveloperWidgetUpdateReason.LOAD_FROM_STUDIO },
            updates
        )
        assertEquals(
            result.activeSpec,
            (repository.loadActive() as DeveloperStateResult.Value).value
        )
    }

    @Test fun `load with no placed widgets still saves active selection`() {
        val repository = DeveloperWidgetStateRepository(FakeStateStore())
        val coordinator = DeveloperWidgetLoadCoordinator(
            stateRepository = repository,
            currentTimeMillis = { 600L },
            listWidgetIds = { intArrayOf() },
            updateWidget = { _, _ -> error("No widget should be updated") }
        )

        val result = coordinator.load(
            widgetId = "digital_clock",
            variantId = WidgetVariantId.Standard(5, 5),
            designRevision = 11L,
            customWidthDp = 900f,
            customHeightDp = 600f
        )

        assertTrue(result.updatedWidgetIds.isEmpty())
        assertEquals(
            result.activeSpec,
            (repository.loadActive() as DeveloperStateResult.Value).value
        )
    }

    @Test fun `same provider switches active clock to battery`() {
        val repository = DeveloperWidgetStateRepository(FakeStateStore())
        val updates = mutableListOf<Int>()
        val coordinator = DeveloperWidgetLoadCoordinator(
            stateRepository = repository,
            currentTimeMillis = { 700L },
            listWidgetIds = { intArrayOf(21) },
            updateWidget = { id, _ -> updates += id }
        )
        coordinator.load("digital_clock", WidgetVariantId.Standard(2, 1), 4L)
        coordinator.load("battery", WidgetVariantId.Standard(2, 2), 6L)

        val active = (repository.loadActive() as DeveloperStateResult.Value).value
        assertEquals("battery", active.widgetId)
        assertEquals(WidgetVariantId.Standard(2, 2), active.variantId)
        assertEquals(listOf(21, 21), updates)
    }

    private class FakeStateStore : DeveloperWidgetStateStore {
        private val values = mutableMapOf<String, String>()
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }
}

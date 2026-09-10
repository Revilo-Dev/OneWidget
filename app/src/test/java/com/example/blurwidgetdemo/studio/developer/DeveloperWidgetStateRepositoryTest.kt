package dev.revilodev.onewidget.studio.developer

import dev.revilodev.onewidget.studio.model.WidgetVariantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperWidgetStateRepositoryTest {
    @Test fun `active selection saves loads and clears with revision and custom size`() {
        val repository = DeveloperWidgetStateRepository(FakeStateStore())
        val expected = ActiveDeveloperWidgetSpec(
            widgetId = "digital_clock",
            variantId = WidgetVariantId.Standard(4, 2),
            designRevision = 17L,
            loadedAtEpochMillis = 1234L,
            customWidthDp = 640.5f,
            customHeightDp = 240f
        )

        repository.saveActive(expected)

        assertEquals(expected, (repository.loadActive() as DeveloperStateResult.Value).value)
        repository.clearActive()
        assertTrue(repository.loadActive() is DeveloperStateResult.Missing)
    }

    @Test fun `active selection reports missing and corrupt state`() {
        val store = FakeStateStore()
        val repository = DeveloperWidgetStateRepository(store)
        assertTrue(repository.loadActive() is DeveloperStateResult.Missing)

        store.putString("active_spec", "active-v1|digital_clock|4x2|7|9|not-a-size|")

        assertTrue(repository.loadActive() is DeveloperStateResult.Corrupt)
    }

    @Test fun `different widget IDs retain their own measurements and newest wins`() {
        val repository = DeveloperWidgetStateRepository(FakeStateStore())
        val first = DeveloperWidgetMeasurement(
            appWidgetId = 41,
            hostSize = DeveloperWidgetHostSize.normalize(120, 50, 220, 160),
            measuredAtEpochMillis = 100L
        )
        val second = DeveloperWidgetMeasurement(
            appWidgetId = 92,
            hostSize = DeveloperWidgetHostSize.normalize(260, 80, 480, 210),
            measuredAtEpochMillis = 200L
        )

        repository.saveMeasurement(first)
        repository.saveMeasurement(second)

        assertEquals(first, (repository.loadMeasurement(41) as DeveloperStateResult.Value).value)
        assertEquals(second, (repository.loadMeasurement(92) as DeveloperStateResult.Value).value)
        assertEquals(second, repository.mostRecentMeasurement())
    }

    @Test fun `persisted measurement retains preferred render dimensions`() {
        val repository = DeveloperWidgetStateRepository(FakeStateStore())
        val landscape = DeveloperWidgetMeasurement(
            appWidgetId = 7,
            hostSize = DeveloperWidgetHostSize.normalize(
                minWidthDp = 100,
                minHeightDp = 45,
                maxWidthDp = 240,
                maxHeightDp = 130,
                isPortrait = false
            ),
            measuredAtEpochMillis = 300L
        )

        repository.saveMeasurement(landscape)
        val restored = (repository.loadMeasurement(7) as DeveloperStateResult.Value).value

        assertEquals(240f, restored.hostSize.preferredWidthDp)
        assertEquals(45f, restored.hostSize.preferredHeightDp)
        assertNull((repository.loadMeasurement(999) as? DeveloperStateResult.Value)?.value)
    }

    private class FakeStateStore : DeveloperWidgetStateStore {
        private val values = mutableMapOf<String, String>()

        override fun getString(key: String): String? = values[key]

        override fun putString(key: String, value: String) {
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }
    }
}

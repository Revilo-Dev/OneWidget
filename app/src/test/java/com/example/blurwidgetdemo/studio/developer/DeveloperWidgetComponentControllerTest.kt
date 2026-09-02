package com.example.blurwidgetdemo.studio.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperWidgetComponentControllerTest {
    @Test fun `developer provider is disabled by default`() {
        val gateway = FakeComponentGateway()

        assertFalse(DeveloperWidgetComponentController(gateway).isEnabled())
        assertEquals(0, gateway.transitions.size)
    }

    @Test fun `enabling developer mode enables only the developer provider`() {
        val store = FakeDeveloperModeStore()
        val gateway = FakeComponentGateway()
        val productionProviderState = true
        val coordinator = DeveloperModeCoordinator(
            DeveloperModeRepository(store),
            DeveloperWidgetComponentController(gateway)
        )

        coordinator.setEnabled(true)

        assertTrue(DeveloperModeRepository(store).isEnabled())
        assertTrue(gateway.currentEnabled)
        assertEquals(listOf(true), gateway.transitions)
        assertTrue(productionProviderState)
    }

    @Test fun `disabling developer mode disables provider without touching production`() {
        val store = FakeDeveloperModeStore().apply { putBoolean(DeveloperModeRepository.KEY_ENABLED, true) }
        val gateway = FakeComponentGateway(currentEnabled = true)
        val productionProviderState = true
        val coordinator = DeveloperModeCoordinator(
            DeveloperModeRepository(store),
            DeveloperWidgetComponentController(gateway)
        )

        coordinator.setEnabled(false)

        assertFalse(DeveloperModeRepository(store).isEnabled())
        assertFalse(gateway.currentEnabled)
        assertEquals(listOf(false), gateway.transitions)
        assertTrue(productionProviderState)
    }

    @Test fun `synchronization avoids redundant component transitions`() {
        val gateway = FakeComponentGateway(currentEnabled = true)
        val controller = DeveloperWidgetComponentController(gateway)

        controller.synchronizeWithDeveloperMode(true)

        assertTrue(gateway.transitions.isEmpty())
    }

    private class FakeComponentGateway(
        var currentEnabled: Boolean = false
    ) : DeveloperWidgetComponentGateway {
        val transitions = mutableListOf<Boolean>()

        override fun isEnabled(): Boolean = currentEnabled

        override fun setEnabled(enabled: Boolean) {
            currentEnabled = enabled
            transitions += enabled
        }
    }

    private class FakeDeveloperModeStore : DeveloperModeStore {
        private val values = mutableMapOf<String, Boolean>()

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
            values[key] ?: defaultValue

        override fun putBoolean(key: String, value: Boolean) {
            values[key] = value
        }
    }
}

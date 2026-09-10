package dev.revilodev.onewidget.studio.developer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperModeRepositoryTest {
    @Test fun `developer mode defaults to disabled`() {
        assertFalse(DeveloperModeRepository(FakeDeveloperModeStore()).isEnabled())
    }

    @Test fun `developer mode can be enabled and disabled`() {
        val repository = DeveloperModeRepository(FakeDeveloperModeStore())
        repository.setEnabled(true)
        assertTrue(repository.isEnabled())
        repository.setEnabled(false)
        assertFalse(repository.isEnabled())
    }

    @Test fun `developer mode persists across repository instances`() {
        val store = FakeDeveloperModeStore()
        DeveloperModeRepository(store).setEnabled(true)
        assertTrue(DeveloperModeRepository(store).isEnabled())
    }

    @Test fun `developer code accepts only the exact current code`() {
        assertTrue(DeveloperAccessCode.isCorrect("41244110"))
        assertFalse(DeveloperAccessCode.isCorrect("41244111"))
        assertFalse(DeveloperAccessCode.isCorrect("4124411"))
        assertFalse(DeveloperAccessCode.isCorrect("412441100"))
        assertFalse(DeveloperAccessCode.isCorrect("4124A110"))
        assertFalse(DeveloperAccessCode.hasValidFormat("4124A110"))
    }

    private class FakeDeveloperModeStore : DeveloperModeStore {
        private val values = mutableMapOf<String, Boolean>()
        override fun getBoolean(key: String, defaultValue: Boolean) = values[key] ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    }
}

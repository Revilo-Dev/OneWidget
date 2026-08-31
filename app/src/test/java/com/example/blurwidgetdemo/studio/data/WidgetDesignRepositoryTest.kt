package com.example.blurwidgetdemo.studio.data

import com.example.blurwidgetdemo.studio.studioTestDesign
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WidgetDesignRepositoryTest {
    private lateinit var temporaryRoot: File
    private lateinit var designsDirectory: File
    private lateinit var store: FakeStudioStateStore
    private lateinit var repository: WidgetDesignRepository

    @Before fun setUp() {
        temporaryRoot = Files.createTempDirectory("widget-design-repository").toFile()
        designsDirectory = File(temporaryRoot, "designs")
        store = FakeStudioStateStore()
        repository = WidgetDesignRepository(designsDirectory, store)
    }

    @After fun tearDown() {
        temporaryRoot.deleteRecursively()
    }

    @Test fun `save load and list preserve a design`() {
        val saved = success(repository.save(studioTestDesign()))
        assertEquals(1L, saved.revision)
        assertEquals(saved, success(repository.load(saved.widgetId)))
        assertEquals(listOf(saved), success(repository.listAvailableDesigns()))
        assertEquals(saved.widgetId, repository.selectedWidgetId())
        assertEquals(saved.revision, repository.activeDesignRevision())
    }

    @Test fun `overwrite increments the revision`() {
        val first = success(repository.save(studioTestDesign()))
        val second = success(repository.save(first.copy(revision = 0L)))
        assertEquals(1L, first.revision)
        assertEquals(2L, second.revision)
    }

    @Test fun `selected variant persists in the state store`() {
        val variant = WidgetVariantId.Standard(5, 5)
        success(repository.setSelectedVariantId(variant))
        val reopened = WidgetDesignRepository(designsDirectory, store)
        assertEquals(variant, reopened.selectedVariantId())
    }

    @Test fun `delete removes a saved design`() {
        val saved = success(repository.save(studioTestDesign()))
        assertTrue(success(repository.delete(saved.widgetId)))
        assertFailure(repository.load(saved.widgetId), StudioRepositoryErrorKind.NOT_FOUND)
        assertFalse(success(repository.delete(saved.widgetId)))
    }

    @Test fun `missing design returns not found`() {
        assertFailure(repository.load("missing_widget"), StudioRepositoryErrorKind.NOT_FOUND)
    }

    @Test fun `corrupt JSON is reported and a later save recovers safely`() {
        designsDirectory.mkdirs()
        File(designsDirectory, "test_widget.json").writeText("{corrupt", Charsets.UTF_8)
        assertFailure(repository.load("test_widget"), StudioRepositoryErrorKind.CORRUPT_DATA)
        val recovered = success(repository.save(studioTestDesign()))
        assertEquals(1L, recovered.revision)
        assertEquals(recovered, success(repository.load("test_widget")))
    }

    private fun <T> success(result: StudioRepositoryResult<T>): T {
        assertTrue("Expected success but was $result", result is StudioRepositoryResult.Success)
        return (result as StudioRepositoryResult.Success).value
    }

    private fun assertFailure(result: StudioRepositoryResult<*>, kind: StudioRepositoryErrorKind) {
        assertTrue("Expected failure but was $result", result is StudioRepositoryResult.Failure)
        assertEquals(kind, (result as StudioRepositoryResult.Failure).error.kind)
    }

    private class FakeStudioStateStore : StudioStateStore {
        private val strings = mutableMapOf<String, String>()
        private val longs = mutableMapOf<String, Long>()
        override fun getString(key: String): String? = strings[key]
        override fun putString(key: String, value: String?) {
            if (value == null) strings.remove(key) else strings[key] = value
        }
        override fun getLong(key: String, defaultValue: Long): Long = longs[key] ?: defaultValue
        override fun putLong(key: String, value: Long) { longs[key] = value }
    }
}

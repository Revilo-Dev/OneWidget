package com.example.blurwidgetdemo.studio.production

import com.example.blurwidgetdemo.studio.data.StudioRepositoryErrorKind
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.studioTestDesign
import com.example.blurwidgetdemo.studio.verification.WidgetVerificationStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProductionWidgetDesignRepositoryTest {
    private lateinit var root: File
    private lateinit var repository: ProductionWidgetDesignRepository

    @Before fun setUp() {
        root = Files.createTempDirectory("production-designs").toFile()
        repository = ProductionWidgetDesignRepository(root)
    }

    @After fun tearDown() {
        root.deleteRecursively()
    }

    @Test fun `promotion stores immutable copy hash and active revision`() {
        val source = studioTestDesign("digital_clock").copy(revision = 7L)
        val promoted = success(repository.promote(source, WidgetVerificationStatus.VERIFIED, 100L))
        val editedDeveloperCopy = source.copy(revision = 8L)

        assertEquals(7L, promoted.metadata.designRevision)
        assertEquals(64, promoted.metadata.sourceDesignHash.length)
        assertEquals(promoted, success(repository.loadActive("digital_clock")))
        assertNotEquals(editedDeveloperCopy.revision, success(repository.loadActive("digital_clock"))?.design?.revision)
    }

    @Test fun `integrity hash is stable SHA-256`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ProductionDesignIntegrity.sha256("abc")
        )
    }

    @Test fun `history is bounded and prior revision can be reactivated`() {
        (1L..12L).forEach { revision ->
            repository.promote(
                studioTestDesign("digital_clock").copy(revision = revision),
                WidgetVerificationStatus.VERIFIED,
                revision
            )
        }

        assertEquals(10, success(repository.history("digital_clock")).size)
        assertEquals(5L, success(repository.activateRevision("digital_clock", 5L)).metadata.designRevision)
        assertEquals(5L, success(repository.loadActive("digital_clock"))?.metadata?.designRevision)
    }

    @Test fun `revert to legacy retains history`() {
        repository.promote(
            studioTestDesign("battery").copy(revision = 2L),
            WidgetVerificationStatus.PARTIALLY_VERIFIED,
            20L
        )

        assertTrue(success(repository.revertToLegacy("battery")))
        assertNull(success(repository.loadActive("battery")))
        assertEquals(1, success(repository.history("battery")).size)
        assertFalse(success(repository.revertToLegacy("battery")))
    }

    @Test fun `corrupt promoted design fails its integrity check`() {
        repository.promote(
            studioTestDesign("digital_clock").copy(revision = 3L),
            WidgetVerificationStatus.VERIFIED,
            30L
        )
        File(root, "digital_clock/revisions/rev_3/design.json").appendText(" ")

        val result = repository.loadRevision("digital_clock", 3L)
        assertTrue(result is StudioRepositoryResult.Failure)
        assertEquals(StudioRepositoryErrorKind.CORRUPT_DATA, (result as StudioRepositoryResult.Failure).error.kind)
    }

    @Test fun `missing design and revision are safe`() {
        assertNull(success(repository.loadActive("missing")))
        val missing = repository.loadRevision("digital_clock", 99L)
        assertTrue(missing is StudioRepositoryResult.Failure)
        assertEquals(StudioRepositoryErrorKind.NOT_FOUND, (missing as StudioRepositoryResult.Failure).error.kind)
    }

    private fun <T> success(result: StudioRepositoryResult<T>): T {
        assertTrue("Expected success but was $result", result is StudioRepositoryResult.Success)
        return (result as StudioRepositoryResult.Success).value
    }
}

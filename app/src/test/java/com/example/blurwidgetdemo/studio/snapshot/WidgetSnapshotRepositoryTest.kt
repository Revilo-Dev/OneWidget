package com.example.blurwidgetdemo.studio.snapshot

import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WidgetSnapshotRepositoryTest {
    private lateinit var temporaryRoot: File
    private lateinit var metadataDirectory: File
    private lateinit var imageFile: File
    private lateinit var repository: WidgetSnapshotRepository
    private val variant = WidgetVariantId.Standard(4, 2)

    @Before fun setUp() {
        temporaryRoot = Files.createTempDirectory("widget-snapshot-repository").toFile()
        metadataDirectory = File(temporaryRoot, "metadata")
        imageFile = File(temporaryRoot, "digital_clock_4x2.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        repository = WidgetSnapshotRepository(metadataDirectory)
    }

    @After fun tearDown() {
        temporaryRoot.deleteRecursively()
    }

    @Test fun `register and lookup return available snapshot metadata`() {
        val metadata = metadata()
        val registered = repository.register(metadata)
        assertTrue(registered is StudioRepositoryResult.Success)
        val lookup = repository.findLatest(metadata.widgetId, metadata.variantId)
        assertTrue(lookup is SnapshotLookupResult.Available)
        assertEquals(metadata, (lookup as SnapshotLookupResult.Available).metadata)
        assertEquals(imageFile, lookup.file)
    }

    @Test fun `missing image is detected and stale metadata is deleted`() {
        repository.register(metadata())
        imageFile.delete()
        assertTrue(repository.findLatest("digital_clock", variant) is SnapshotLookupResult.MissingFile)
        val deleted = repository.deleteStaleMetadata()
        assertTrue(deleted is StudioRepositoryResult.Success)
        assertEquals(1, (deleted as StudioRepositoryResult.Success).value)
        assertTrue(repository.findLatest("digital_clock", variant) is SnapshotLookupResult.NotFound)
    }

    @Test fun `metadata can be removed explicitly`() {
        repository.register(metadata())
        val removed = repository.removeMetadata("digital_clock", variant)
        assertTrue(removed is StudioRepositoryResult.Success)
        assertTrue((removed as StudioRepositoryResult.Success).value)
        assertTrue(repository.findLatest("digital_clock", variant) is SnapshotLookupResult.NotFound)
    }

    private fun metadata() = WidgetSnapshotMetadata(
        widgetId = "digital_clock",
        variantId = variant,
        widthDp = 320f,
        heightDp = 160f,
        density = 3f,
        designRevision = 7L,
        createdAt = 1_787_000_000_000L,
        snapshotPath = imageFile.absolutePath
    )
}

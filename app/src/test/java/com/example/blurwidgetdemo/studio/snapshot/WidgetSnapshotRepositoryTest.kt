package dev.revilodev.onewidget.studio.snapshot

import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WidgetSnapshotRepositoryTest {
    private lateinit var temporaryRoot: File
    private lateinit var metadataDirectory: File
    private lateinit var imagesDirectory: File
    private lateinit var repository: WidgetSnapshotRepository
    private val variant = WidgetVariantId.Standard(4, 2)

    @Before fun setUp() {
        temporaryRoot = Files.createTempDirectory("widget-snapshot-repository").toFile()
        metadataDirectory = File(temporaryRoot, "metadata")
        imagesDirectory = File(temporaryRoot, "images").apply { mkdirs() }
        repository = WidgetSnapshotRepository(metadataDirectory)
    }

    @After fun tearDown() {
        temporaryRoot.deleteRecursively()
    }

    @Test fun `register and lookup preserve complete revision-aware metadata`() {
        val metadata = metadata(revision = 7L, createdAt = 100L)

        assertTrue(repository.register(metadata) is StudioRepositoryResult.Success)
        val lookup = repository.latest(metadata.widgetId, metadata.variantId)

        assertTrue(lookup is SnapshotLookupResult.Available)
        assertEquals(metadata, (lookup as SnapshotLookupResult.Available).metadata)
        assertEquals(960, lookup.metadata.widthPx)
        assertEquals(480, lookup.metadata.heightPx)
        assertEquals(SnapshotScope.DEVELOPER, lookup.metadata.scope)
        assertEquals(SnapshotRenderMode.LOCAL_REMOTEVIEWS, lookup.metadata.renderMode)
        assertEquals(2, lookup.metadata.rendererVersion)
    }

    @Test fun `latest prefers highest design revision then newest creation time`() {
        repository.register(metadata(revision = 7L, createdAt = 900L))
        repository.register(metadata(revision = 9L, createdAt = 100L))

        val latest = repository.latest("digital_clock", variant) as SnapshotLookupResult.Available

        assertEquals(9L, latest.metadata.designRevision)
    }

    @Test fun `status distinguishes current stale and missing revisions`() {
        assertTrue(repository.status("digital_clock", variant, 7L) is SnapshotStatus.Missing)
        repository.register(metadata(revision = 5L, createdAt = 100L))
        val stale = repository.status("digital_clock", variant, 7L)
        assertTrue(stale is SnapshotStatus.Stale)
        assertEquals(5L, (stale as SnapshotStatus.Stale).snapshot.metadata.designRevision)

        repository.register(metadata(revision = 7L, createdAt = 200L))
        val current = repository.status("digital_clock", variant, 7L)
        assertTrue(current is SnapshotStatus.Current)
        assertEquals(7L, (current as SnapshotStatus.Current).snapshot.metadata.designRevision)
    }

    @Test fun `missing PNG is reported and cleanup removes its metadata`() {
        val metadata = metadata(revision = 4L, createdAt = 100L)
        repository.register(metadata)
        File(metadata.snapshotPath).delete()

        assertTrue(repository.latest("digital_clock", variant) is SnapshotLookupResult.MissingFile)
        val cleanup = repository.cleanup() as StudioRepositoryResult.Success

        assertEquals(1, cleanup.value.removedMetadata)
        assertTrue(repository.latest("digital_clock", variant) is SnapshotLookupResult.NotFound)
    }

    @Test fun `corrupt metadata is surfaced when no valid revision exists`() {
        metadataDirectory.mkdirs()
        File(metadataDirectory, "digital_clock__4x2__rev_3.json").writeText("{broken")

        assertTrue(
            repository.latest("digital_clock", variant) is SnapshotLookupResult.CorruptMetadata
        )
    }

    @Test fun `delete removes selected PNG and corresponding metadata only`() {
        val old = metadata(revision = 5L, createdAt = 100L)
        val current = metadata(revision = 6L, createdAt = 200L)
        repository.register(old)
        repository.register(current)

        val deleted = repository.delete(current)

        assertTrue(deleted is StudioRepositoryResult.Success)
        assertFalse(File(current.snapshotPath).exists())
        val remaining = repository.latest("digital_clock", variant) as SnapshotLookupResult.Available
        assertEquals(5L, remaining.metadata.designRevision)
    }

    @Test fun `cleanup removes corrupt metadata temporary files and orphan PNGs`() {
        val valid = metadata(revision = 8L, createdAt = 100L)
        repository.register(valid)
        metadataDirectory.mkdirs()
        File(metadataDirectory, "corrupt.json").writeText("not-json")
        File(imagesDirectory, ".writing.tmp").writeText("temporary")
        File(imagesDirectory, "orphan.png").writeBytes(byteArrayOf(8, 8))

        val cleanup = repository.cleanup() as StudioRepositoryResult.Success

        assertEquals(SnapshotCleanupResult(1, 1, 1), cleanup.value)
        assertTrue(repository.latest("digital_clock", variant) is SnapshotLookupResult.Available)
    }

    @Test fun `phase one schema remains decodable`() {
        val image = imageFile("legacy.png")
        metadataDirectory.mkdirs()
        File(metadataDirectory, "digital_clock__4x2.json").writeText(
            """
            {
              "schemaVersion": 1,
              "widgetId": "digital_clock",
              "variantId": "4x2",
              "widthDp": 320,
              "heightDp": 160,
              "density": 3,
              "designRevision": 2,
              "createdAt": 50,
              "snapshotPath": "${image.absolutePath.replace("\\", "\\\\")}"
            }
            """.trimIndent()
        )

        val lookup = repository.latest("digital_clock", variant) as SnapshotLookupResult.Available

        assertEquals(2L, lookup.metadata.designRevision)
        assertEquals(960, lookup.metadata.widthPx)
        assertEquals(SnapshotScope.DEVELOPER, lookup.metadata.scope)
    }

    private fun metadata(revision: Long, createdAt: Long): WidgetSnapshotMetadata {
        val image = imageFile("digital_clock__4x2__rev_$revision.png")
        return WidgetSnapshotMetadata(
            widgetId = "digital_clock",
            variantId = variant,
            widthDp = 320f,
            heightDp = 160f,
            density = 3f,
            designRevision = revision,
            createdAt = createdAt,
            snapshotPath = image.absolutePath,
            widthPx = 960,
            heightPx = 480,
            rendererVersion = 2
        )
    }

    private fun imageFile(name: String): File = File(imagesDirectory, name).apply {
        writeBytes(byteArrayOf(1, 2, 3))
    }
}

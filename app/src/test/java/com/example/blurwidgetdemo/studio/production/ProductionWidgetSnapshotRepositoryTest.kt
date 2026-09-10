package dev.revilodev.onewidget.studio.production

import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.snapshot.SnapshotScope
import dev.revilodev.onewidget.studio.snapshot.SnapshotStatus
import dev.revilodev.onewidget.studio.snapshot.WidgetSnapshotMetadata
import dev.revilodev.onewidget.studio.snapshot.WidgetSnapshotRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProductionWidgetSnapshotRepositoryTest {
    private lateinit var root: File
    private lateinit var developer: WidgetSnapshotRepository
    private lateinit var production: ProductionWidgetSnapshotRepository
    private val current = WidgetVariantId.Standard(2, 1)
    private val stale = WidgetVariantId.Standard(2, 2)
    private val missing = WidgetVariantId.Standard(4, 2)

    @Before fun setUp() {
        root = Files.createTempDirectory("production-snapshots").toFile()
        developer = WidgetSnapshotRepository(File(root, "developer/metadata"))
        production = ProductionWidgetSnapshotRepository(
            WidgetSnapshotRepository(File(root, "production/metadata"))
        )
    }

    @After fun tearDown() {
        root.deleteRecursively()
    }

    @Test fun `promotion copies only current snapshots into production scope`() {
        register(current, revision = 7L)
        register(stale, revision = 6L)

        val report = success(
            production.promoteCurrent("digital_clock", 7L, listOf(current, stale, missing), developer)
        )

        assertEquals(listOf(current), report.promotedVariants)
        assertEquals(listOf(missing), report.missingVariants)
        assertEquals(listOf(stale), report.staleVariants)
        val status = production.status("digital_clock", current, 7L)
        assertTrue(status is SnapshotStatus.Current)
        assertEquals(
            SnapshotScope.PRODUCTION,
            (status as SnapshotStatus.Current).snapshot.metadata.scope
        )
    }

    private fun register(variant: WidgetVariantId, revision: Long) {
        val image = File(developer.imagesDirectory, "source_${variant.stableId}_$revision.png").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        developer.register(
            WidgetSnapshotMetadata(
                widgetId = "digital_clock",
                variantId = variant,
                widthDp = 180f,
                heightDp = 80f,
                density = 2f,
                designRevision = revision,
                createdAt = 10L,
                snapshotPath = image.absolutePath
            )
        )
    }

    private fun <T> success(result: StudioRepositoryResult<T>): T {
        assertTrue(result is StudioRepositoryResult.Success)
        return (result as StudioRepositoryResult.Success).value
    }
}

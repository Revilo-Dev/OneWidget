package dev.revilodev.onewidget.studio.gallery

import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.snapshot.SnapshotLookupResult
import dev.revilodev.onewidget.studio.snapshot.SnapshotStatus
import dev.revilodev.onewidget.studio.snapshot.WidgetSnapshotMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WidgetGalleryPreviewResolverTest {
    private val variant = WidgetVariantId.Standard(4, 2)
    private val available = SnapshotLookupResult.Available(
        WidgetSnapshotMetadata(
            widgetId = "digital_clock",
            variantId = variant,
            widthDp = 312f,
            heightDp = 152f,
            density = 3f,
            designRevision = 7L,
            createdAt = 100L,
            snapshotPath = "images/digital_clock__4x2__rev_7.png"
        ),
        File("snapshot.png")
    )

    @Test fun `current developer snapshot wins while Developer Mode is enabled`() {
        val resolver = WidgetGalleryPreviewResolver { _, _, _ -> SnapshotStatus.Current(available) }

        val result = resolver.resolve(request(developerModeEnabled = true))

        assertTrue(result is GalleryPreviewSource.GeneratedSnapshot)
        assertFalse((result as GalleryPreviewSource.GeneratedSnapshot).stale)
    }

    @Test fun `stale developer snapshot is the first fallback`() {
        val resolver = WidgetGalleryPreviewResolver { _, _, revision ->
            SnapshotStatus.Stale(available, revision)
        }

        val result = resolver.resolve(request(developerModeEnabled = true))

        assertTrue(result is GalleryPreviewSource.GeneratedSnapshot)
        assertTrue((result as GalleryPreviewSource.GeneratedSnapshot).stale)
    }

    @Test fun `Developer Mode off never exposes developer snapshot`() {
        var snapshotWasRead = false
        val resolver = WidgetGalleryPreviewResolver { _, _, _ ->
            snapshotWasRead = true
            SnapshotStatus.Current(available)
        }

        val result = resolver.resolve(request(developerModeEnabled = false))

        assertFalse(snapshotWasRead)
        assertEquals(GalleryPreviewSource.PackagedPreview(42), result)
    }

    @Test fun `Developer Mode on keeps developer snapshot ahead of production snapshot`() {
        var productionWasRead = false
        val resolver = WidgetGalleryPreviewResolver(
            developerSnapshotStatus = { _, _, _ -> SnapshotStatus.Current(available) },
            productionSnapshotStatus = { _, _, _ ->
                productionWasRead = true
                SnapshotStatus.Current(available)
            }
        )

        val result = resolver.resolve(
            request(developerModeEnabled = true).copy(productionDesignRevision = 7L)
        )

        assertTrue(result is GalleryPreviewSource.GeneratedSnapshot)
        assertFalse(productionWasRead)
    }

    @Test fun `Developer Mode off gives current production snapshot priority`() {
        var developerWasRead = false
        val resolver = WidgetGalleryPreviewResolver(
            developerSnapshotStatus = { _, _, _ ->
                developerWasRead = true
                SnapshotStatus.Current(available)
            },
            productionSnapshotStatus = { _, _, _ -> SnapshotStatus.Current(available) }
        )

        val result = resolver.resolve(
            request(developerModeEnabled = false).copy(productionDesignRevision = 7L)
        )

        assertTrue(result is GalleryPreviewSource.GeneratedSnapshot)
        assertFalse(developerWasRead)
    }

    @Test fun `stale production snapshot is never exposed`() {
        val resolver = WidgetGalleryPreviewResolver(
            developerSnapshotStatus = { _, _, _ -> SnapshotStatus.Missing },
            productionSnapshotStatus = { _, _, revision -> SnapshotStatus.Stale(available, revision) }
        )

        assertEquals(
            GalleryPreviewSource.PackagedPreview(42),
            resolver.resolve(request(false).copy(productionDesignRevision = 8L))
        )
    }

    @Test fun `packaged preview follows a missing developer snapshot`() {
        val resolver = WidgetGalleryPreviewResolver { _, _, _ -> SnapshotStatus.Missing }

        assertEquals(
            GalleryPreviewSource.PackagedPreview(42),
            resolver.resolve(request(developerModeEnabled = true))
        )
    }

    @Test fun `legacy preview and generic fallback remain available`() {
        val resolver = WidgetGalleryPreviewResolver { _, _, _ -> SnapshotStatus.Missing }
        val legacy = request(developerModeEnabled = true).copy(packagedPreviewResId = null)
        val fallback = legacy.copy(legacyPreviewAvailable = false)

        assertEquals(GalleryPreviewSource.LegacyScaledPreview, resolver.resolve(legacy))
        assertEquals(GalleryPreviewSource.Fallback, resolver.resolve(fallback))
    }

    @Test fun `battery snapshot resolves through the generic widget and variant key`() {
        var resolvedWidget = ""
        val batteryAvailable = available.copy(
            metadata = available.metadata.copy(widgetId = "battery")
        )
        val resolver = WidgetGalleryPreviewResolver { widgetId, _, _ ->
            resolvedWidget = widgetId
            SnapshotStatus.Current(batteryAvailable)
        }

        val result = resolver.resolve(
            request(developerModeEnabled = true).copy(widgetId = "battery")
        )

        assertEquals("battery", resolvedWidget)
        assertTrue(result is GalleryPreviewSource.GeneratedSnapshot)
    }

    private fun request(developerModeEnabled: Boolean) = GalleryPreviewRequest(
        developerModeEnabled = developerModeEnabled,
        widgetId = "digital_clock",
        variantId = variant,
        currentDesignRevision = 7L,
        packagedPreviewResId = 42,
        legacyPreviewAvailable = true
    )
}

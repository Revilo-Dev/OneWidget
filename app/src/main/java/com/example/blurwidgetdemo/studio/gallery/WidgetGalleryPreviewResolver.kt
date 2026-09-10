package dev.revilodev.onewidget.studio.gallery

import android.graphics.Bitmap
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.snapshot.SnapshotLookupResult
import dev.revilodev.onewidget.studio.snapshot.SnapshotScope
import dev.revilodev.onewidget.studio.snapshot.SnapshotStatus

data class GalleryPreviewRequest(
    val developerModeEnabled: Boolean,
    val widgetId: String?,
    val variantId: WidgetVariantId,
    val currentDesignRevision: Long?,
    val productionDesignRevision: Long? = null,
    val packagedPreviewResId: Int?,
    val legacyPreviewAvailable: Boolean
)

sealed class GalleryPreviewSource {
    data class GeneratedSnapshot(
        val snapshot: SnapshotLookupResult.Available,
        val stale: Boolean
    ) : GalleryPreviewSource()
    data class PackagedPreview(val resourceId: Int) : GalleryPreviewSource()
    data object LegacyScaledPreview : GalleryPreviewSource()
    data object Fallback : GalleryPreviewSource()
}

class WidgetGalleryPreviewResolver(
    private val developerSnapshotStatus: (String, WidgetVariantId, Long) -> SnapshotStatus,
    private val productionSnapshotStatus: (String, WidgetVariantId, Long) -> SnapshotStatus = { _, _, _ ->
        SnapshotStatus.Missing
    }
) {
    constructor(
        snapshotStatus: (String, WidgetVariantId, Long) -> SnapshotStatus
    ) : this(snapshotStatus, { _, _, _ -> SnapshotStatus.Missing })

    fun resolve(request: GalleryPreviewRequest): GalleryPreviewSource {
        if (
            request.developerModeEnabled && request.widgetId != null &&
            request.currentDesignRevision != null
        ) {
            when (
                val status = developerSnapshotStatus(
                    request.widgetId,
                    request.variantId,
                    request.currentDesignRevision
                )
            ) {
                is SnapshotStatus.Current -> return GalleryPreviewSource.GeneratedSnapshot(
                    status.snapshot,
                    stale = false
                )
                is SnapshotStatus.Stale -> return GalleryPreviewSource.GeneratedSnapshot(
                    status.snapshot,
                    stale = true
                )
                SnapshotStatus.Missing -> Unit
            }
        }
        if (request.widgetId != null && request.productionDesignRevision != null) {
            when (
                val status = productionSnapshotStatus(
                    request.widgetId,
                    request.variantId,
                    request.productionDesignRevision
                )
            ) {
                is SnapshotStatus.Current -> return GalleryPreviewSource.GeneratedSnapshot(
                    status.snapshot,
                    stale = false
                )
                // A production snapshot is never shown against a different active revision.
                is SnapshotStatus.Stale, SnapshotStatus.Missing -> Unit
            }
        }
        request.packagedPreviewResId?.let { return GalleryPreviewSource.PackagedPreview(it) }
        if (request.legacyPreviewAvailable) return GalleryPreviewSource.LegacyScaledPreview
        return GalleryPreviewSource.Fallback
    }
}

data class SnapshotCacheKey(
    val widgetId: String,
    val variantId: WidgetVariantId,
    val designRevision: Long,
    val snapshotPath: String,
    val scope: SnapshotScope = SnapshotScope.DEVELOPER
)

class SnapshotMemoryCache<T>(private val maxEntries: Int) {
    init {
        require(maxEntries > 0) { "Snapshot cache capacity must be positive" }
    }

    private val values = object : LinkedHashMap<SnapshotCacheKey, T>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SnapshotCacheKey, T>?): Boolean =
            size > maxEntries
    }

    @Synchronized fun get(key: SnapshotCacheKey): T? = values[key]

    @Synchronized fun put(key: SnapshotCacheKey, value: T): T? = values.put(key, value)

    @Synchronized fun invalidate(widgetId: String, variantId: WidgetVariantId) {
        values.keys.removeAll { it.widgetId == widgetId && it.variantId == variantId }
    }

    @Synchronized fun clear() = values.clear()

    @Synchronized fun size(): Int = values.size
}

object WidgetGalleryBitmapCache {
    private const val MAX_ENTRIES = 12
    private val cache = SnapshotMemoryCache<Bitmap>(MAX_ENTRIES)

    fun get(key: SnapshotCacheKey): Bitmap? = cache.get(key)

    fun put(key: SnapshotCacheKey, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun invalidate(widgetId: String, variantId: WidgetVariantId) {
        cache.invalidate(widgetId, variantId)
    }
}

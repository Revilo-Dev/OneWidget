package dev.revilodev.onewidget.studio.gallery

import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.snapshot.SnapshotScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotMemoryCacheTest {
    private val variant = WidgetVariantId.Standard(4, 2)

    @Test fun `cache stores and replaces matching snapshot key`() {
        val cache = SnapshotMemoryCache<String>(2)
        val key = key(7L)

        cache.put(key, "first")
        assertEquals("first", cache.get(key))
        cache.put(key, "replacement")

        assertEquals("replacement", cache.get(key))
        assertEquals(1, cache.size())
    }

    @Test fun `snapshot update invalidates all cached revisions for variant`() {
        val cache = SnapshotMemoryCache<String>(4)
        cache.put(key(7L), "old")
        cache.put(key(8L), "current")
        cache.put(
            SnapshotCacheKey("digital_clock", WidgetVariantId.Standard(2, 1), 8L, "other"),
            "other variant"
        )

        cache.invalidate("digital_clock", variant)

        assertNull(cache.get(key(7L)))
        assertNull(cache.get(key(8L)))
        assertEquals(1, cache.size())
    }

    @Test fun `least recently used entry is evicted at capacity`() {
        val cache = SnapshotMemoryCache<String>(2)
        val first = key(1L)
        val second = key(2L)
        val third = key(3L)
        cache.put(first, "first")
        cache.put(second, "second")
        cache.get(first)

        cache.put(third, "third")

        assertNull(cache.get(second))
        assertEquals("first", cache.get(first))
        assertEquals("third", cache.get(third))
    }

    @Test fun `developer and production snapshots never share a cache entry`() {
        val cache = SnapshotMemoryCache<String>(2)
        val developer = key(7L)
        val production = developer.copy(scope = SnapshotScope.PRODUCTION)

        cache.put(developer, "mutable developer")
        cache.put(production, "immutable production")

        assertEquals("mutable developer", cache.get(developer))
        assertEquals("immutable production", cache.get(production))
    }

    private fun key(revision: Long) = SnapshotCacheKey(
        "digital_clock",
        variant,
        revision,
        "images/rev_$revision.png"
    )
}

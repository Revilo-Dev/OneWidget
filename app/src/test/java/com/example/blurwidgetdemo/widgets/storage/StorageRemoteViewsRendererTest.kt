package dev.revilodev.onewidget.widgets.storage

import android.os.Build
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.model.WidgetDesignResolver
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.storage.StorageStudioDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageRemoteViewsRendererTest {
    @Test fun `design null preserves production compact and large layout selection`() {
        val compact = StorageRemoteViewsRenderer.plan(StorageRenderSize(400f, 100f), design = null)
        val large = StorageRemoteViewsRenderer.plan(StorageRenderSize(180f, 101f), design = null)

        assertEquals(R.layout.widget_storage_compact, compact.appliedLayoutResId)
        assertEquals(R.layout.widget_storage_large, large.appliedLayoutResId)
        assertNull(large.geometry)
    }

    @Test fun `studio and snapshot use one deterministic storage route`() {
        val design = resolved(WidgetVariantId.Standard(4, 2))
        val actual = StorageRemoteViewsRenderer.plan(
            StorageRenderSize(312f, 152f),
            design,
            StorageRenderMode.STUDIO_ACTUAL_PREVIEW,
            apiLevel = Build.VERSION_CODES.S
        )
        val snapshot = StorageRemoteViewsRenderer.plan(
            StorageRenderSize(312f, 152f),
            design,
            StorageRenderMode.SNAPSHOT,
            apiLevel = Build.VERSION_CODES.S
        )

        assertEquals(R.layout.widget_studio_storage, actual.appliedLayoutResId)
        assertEquals(actual.appliedLayoutResId, snapshot.appliedLayoutResId)
        assertEquals(4, snapshot.geometry?.elements?.size)
        assertFalse(snapshot.installTapActions)
    }

    @Test fun `sample data and detail formatting are stable`() {
        val sample = StorageSamples.runtime()
        assertEquals(62, sample.storage.percent)
        assertEquals(
            "Storage  62% • 79GB/128GB",
            StorageContentFormatter.detail(
                "Storage",
                sample.storage,
                showTitle = true,
                detailMode = StorageWidget.DetailMode.BOTH
            )
        )
    }

    @Test fun `api 31 geometry includes visibility padding alignment and radius`() {
        val plan = StorageRemoteViewsGeometry.plan(
            resolved(WidgetVariantId.Standard(4, 2)),
            StorageRenderSize(312f, 152f),
            density = 2f,
            apiLevel = Build.VERSION_CODES.S
        )
        val storage = plan.elements.single { it.elementId == StorageStudioDescriptor.STORAGE }
        assertTrue(storage.visible)
        assertEquals(312f, storage.widthDp)
        assertEquals(32, storage.padding[0])
        assertEquals(15f, storage.cornerRadiusDp)
        assertEquals(0.5f, StorageRemoteViewsGeometry.progressRadiusFraction(storage))
        assertTrue(plan.diagnostics.isEmpty())
    }

    @Test fun `pre 31 retains XML geometry and reports radius fallback`() {
        val plan = StorageRemoteViewsGeometry.plan(
            resolved(WidgetVariantId.Standard(4, 2)),
            StorageRenderSize(312f, 152f),
            density = 3f,
            apiLevel = Build.VERSION_CODES.R
        )
        val storage = plan.elements.single { it.elementId == StorageStudioDescriptor.STORAGE }
        assertFalse(storage.dynamicGeometrySupported)
        assertEquals(0.5f, StorageRemoteViewsGeometry.progressRadiusFraction(storage))
        assertTrue(plan.diagnostics.any { it.property == "cornerRadius" })
    }

    @Test fun `every storage element has a view binding`() {
        assertEquals(
            StorageStudioDescriptor.create().elements.map { it.elementId }.toSet(),
            StorageElementViewMap.bindings.keys
        )
    }

    private fun resolved(variant: WidgetVariantId.Standard) = requireNotNull(
        WidgetDesignResolver.resolve(StorageStudioDescriptor.defaultDesign(), variant).design
    )
}

package com.example.blurwidgetdemo.studio.developer

import com.example.blurwidgetdemo.studio.clock.ClockStudioDescriptor
import com.example.blurwidgetdemo.studio.clock.ClockStudioRemoteViewsRenderer
import com.example.blurwidgetdemo.studio.battery.BatteryStudioDescriptor
import com.example.blurwidgetdemo.studio.battery.BatteryStudioRemoteViewsRenderer
import com.example.blurwidgetdemo.studio.storage.StorageStudioDescriptor
import com.example.blurwidgetdemo.studio.storage.StorageStudioRemoteViewsRenderer
import com.example.blurwidgetdemo.studio.calendar.CalendarStudioDescriptor
import com.example.blurwidgetdemo.studio.calendar.CalendarStudioRemoteViewsRenderer
import com.example.blurwidgetdemo.studio.data.StudioRepositoryError
import com.example.blurwidgetdemo.studio.data.StudioRepositoryErrorKind
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import com.example.blurwidgetdemo.studio.registry.DefaultWidgetStudioRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperWidgetRenderResolverTest {
    private val active = ActiveDeveloperWidgetSpec(
        widgetId = ClockStudioDescriptor.WIDGET_ID,
        variantId = WidgetVariantId.Standard(4, 2),
        designRevision = 7L,
        loadedAtEpochMillis = 100L
    )

    @Test fun `no active design selects the safe placeholder path`() {
        val resolver = resolverWithDesign()

        assertTrue(
            resolver.resolve(
                DeveloperStateResult.Missing,
                DeveloperWidgetHostSize.normalize(100, 40, 200, 100)
            ) is DeveloperWidgetRenderResolution.NoActiveDesign
        )
    }

    @Test fun `missing saved design selects the developer error path`() {
        val resolver = DeveloperWidgetRenderResolver(DefaultWidgetStudioRegistry.create()) {
            StudioRepositoryResult.Failure(
                StudioRepositoryError(StudioRepositoryErrorKind.NOT_FOUND, "missing")
            )
        }

        val result = resolver.resolve(
            DeveloperStateResult.Value(active),
            DeveloperWidgetHostSize.normalize(100, 40, 200, 100)
        )

        assertTrue(result is DeveloperWidgetRenderResolution.Error)
        assertTrue((result as DeveloperWidgetRenderResolution.Error).message.startsWith("Missing design"))
    }

    @Test fun `valid clock uses registered clock adapter selected variant and host size`() {
        val result = resolverWithDesign().resolve(
            DeveloperStateResult.Value(active),
            DeveloperWidgetHostSize.normalize(281, 55, 520, 126)
        ) as DeveloperWidgetRenderResolution.Ready

        assertSame(ClockStudioRemoteViewsRenderer, result.descriptor.renderer)
        assertEquals(WidgetVariantId.Standard(4, 2), result.design.variantId)
        assertEquals(281f, result.renderSize.widthDp)
        assertEquals(126f, result.renderSize.heightDp)
    }

    @Test fun `saved revision change requires an explicit Studio reload`() {
        val resolver = DeveloperWidgetRenderResolver(DefaultWidgetStudioRegistry.create()) {
            StudioRepositoryResult.Success(ClockStudioDescriptor.defaultDesign().copy(revision = 8L))
        }

        val result = resolver.resolve(
            DeveloperStateResult.Value(active),
            DeveloperWidgetHostSize.normalize(100, 40, 200, 100)
        )

        assertTrue(result is DeveloperWidgetRenderResolution.Error)
        assertTrue((result as DeveloperWidgetRenderResolution.Error).message.contains("Load it again"))
    }

    @Test fun `active battery uses the same developer provider resolution path`() {
        val battery = ActiveDeveloperWidgetSpec(
            widgetId = BatteryStudioDescriptor.WIDGET_ID,
            variantId = WidgetVariantId.Standard(2, 2),
            designRevision = 3L,
            loadedAtEpochMillis = 200L
        )
        val resolver = DeveloperWidgetRenderResolver(DefaultWidgetStudioRegistry.create()) {
            StudioRepositoryResult.Success(BatteryStudioDescriptor.defaultDesign().copy(revision = 3L))
        }

        val result = resolver.resolve(
            DeveloperStateResult.Value(battery),
            DeveloperWidgetHostSize.normalize(250, 101, 500, 202)
        ) as DeveloperWidgetRenderResolution.Ready

        assertSame(BatteryStudioRemoteViewsRenderer, result.descriptor.renderer)
        assertEquals(BatteryStudioDescriptor.WIDGET_ID, result.design.widgetId)
        assertEquals(250f, result.renderSize.widthDp)
        assertEquals(202f, result.renderSize.heightDp)
    }

    @Test fun `active storage uses the generic developer provider path`() {
        val storage = ActiveDeveloperWidgetSpec(
            widgetId = StorageStudioDescriptor.WIDGET_ID,
            variantId = WidgetVariantId.Standard(4, 2),
            designRevision = 5L,
            loadedAtEpochMillis = 300L
        )
        val resolver = DeveloperWidgetRenderResolver(DefaultWidgetStudioRegistry.create()) {
            StudioRepositoryResult.Success(StorageStudioDescriptor.defaultDesign().copy(revision = 5L))
        }

        val result = resolver.resolve(
            DeveloperStateResult.Value(storage),
            DeveloperWidgetHostSize.normalize(312, 80, 620, 152)
        ) as DeveloperWidgetRenderResolution.Ready

        assertSame(StorageStudioRemoteViewsRenderer, result.descriptor.renderer)
        assertEquals(StorageStudioDescriptor.WIDGET_ID, result.design.widgetId)
    }

    @Test fun `active calendar uses the generic developer provider path`() {
        val calendar = ActiveDeveloperWidgetSpec(
            widgetId = CalendarStudioDescriptor.WIDGET_ID,
            variantId = WidgetVariantId.Standard(2, 2),
            designRevision = 6L,
            loadedAtEpochMillis = 400L
        )
        val resolver = DeveloperWidgetRenderResolver(DefaultWidgetStudioRegistry.create()) {
            StudioRepositoryResult.Success(CalendarStudioDescriptor.defaultDesign().copy(revision = 6L))
        }

        val result = resolver.resolve(
            DeveloperStateResult.Value(calendar),
            DeveloperWidgetHostSize.normalize(280, 101, 560, 152)
        ) as DeveloperWidgetRenderResolution.Ready

        assertSame(CalendarStudioRemoteViewsRenderer, result.descriptor.renderer)
        assertEquals(CalendarStudioDescriptor.WIDGET_ID, result.design.widgetId)
    }

    @Test fun `two instances resolve same active variant at independent host dimensions`() {
        val repository = DeveloperWidgetStateRepository(FakeStateStore())
        repository.saveMeasurement(
            DeveloperWidgetMeasurement(
                17,
                DeveloperWidgetHostSize.normalize(120, 40, 240, 90),
                10L
            )
        )
        repository.saveMeasurement(
            DeveloperWidgetMeasurement(
                18,
                DeveloperWidgetHostSize.normalize(300, 75, 600, 190),
                20L
            )
        )
        val resolver = resolverWithDesign()
        val firstSize = (repository.loadMeasurement(17) as DeveloperStateResult.Value).value.hostSize
        val secondSize = (repository.loadMeasurement(18) as DeveloperStateResult.Value).value.hostSize

        val first = resolver.resolve(
            DeveloperStateResult.Value(active),
            firstSize
        ) as DeveloperWidgetRenderResolution.Ready
        val second = resolver.resolve(
            DeveloperStateResult.Value(active),
            secondSize
        ) as DeveloperWidgetRenderResolution.Ready

        assertEquals(first.design.variantId, second.design.variantId)
        assertEquals(120f, first.renderSize.widthDp)
        assertEquals(90f, first.renderSize.heightDp)
        assertEquals(300f, second.renderSize.widthDp)
        assertEquals(190f, second.renderSize.heightDp)
    }

    private fun resolverWithDesign() = DeveloperWidgetRenderResolver(
        DefaultWidgetStudioRegistry.create()
    ) {
        StudioRepositoryResult.Success(ClockStudioDescriptor.defaultDesign().copy(revision = 7L))
    }

    private class FakeStateStore : DeveloperWidgetStateStore {
        private val values = mutableMapOf<String, String>()
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }
}

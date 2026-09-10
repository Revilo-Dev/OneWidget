package dev.revilodev.onewidget.widgets.calendar

import android.os.Build
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.calendar.CalendarStudioDescriptor
import dev.revilodev.onewidget.studio.model.WidgetDesignResolver
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRemoteViewsRendererTest {
    @Test fun `design null preserves production compact and large layout selection`() {
        val compact = CalendarRemoteViewsRenderer.plan(CalendarRenderSize(400f, 100f), design = null)
        val large = CalendarRemoteViewsRenderer.plan(CalendarRenderSize(180f, 101f), design = null)

        assertEquals(R.layout.widget_calendar_compact, compact.appliedLayoutResId)
        assertEquals(R.layout.widget_calendar_large, large.appliedLayoutResId)
        assertNull(large.geometry)
        assertTrue(large.installTapActions)
    }

    @Test fun `studio and snapshot use deterministic action-free route`() {
        val design = resolved(WidgetVariantId.Standard(4, 2))
        val actual = CalendarRemoteViewsRenderer.plan(
            CalendarRenderSize(312f, 152f),
            design,
            CalendarRenderMode.STUDIO_ACTUAL_PREVIEW,
            apiLevel = Build.VERSION_CODES.S
        )
        val snapshot = CalendarRemoteViewsRenderer.plan(
            CalendarRenderSize(312f, 152f),
            design,
            CalendarRenderMode.SNAPSHOT,
            apiLevel = Build.VERSION_CODES.S
        )

        assertEquals(R.layout.widget_studio_calendar, actual.appliedLayoutResId)
        assertEquals(actual.appliedLayoutResId, snapshot.appliedLayoutResId)
        assertEquals(2, snapshot.geometry?.elements?.size)
        assertFalse(snapshot.installTapActions)
    }

    @Test fun `content states are explicit and stable`() {
        val event = CalendarContentResolver.event("", 100L) { "4 Sep, 10:00 AM" }
        assertEquals(CalendarContentState.EVENT, event.state)
        assertEquals("Untitled event", event.title)
        assertEquals("Starts 4 Sep, 10:00 AM", event.details)
        assertEquals(CalendarContentState.EMPTY, CalendarContentResolver.empty().state)
        assertEquals(
            CalendarContentState.PERMISSION_REQUIRED,
            CalendarContentResolver.permissionRequired().state
        )
        assertEquals(CalendarContentState.UNAVAILABLE, CalendarContentResolver.unavailable().state)
        assertEquals(CalendarContentState.ERROR, CalendarContentResolver.error().state)
    }

    @Test fun `geometry maps both required calendar elements`() {
        val plan = CalendarRemoteViewsGeometry.plan(
            resolved(WidgetVariantId.Standard(4, 2)),
            CalendarRenderSize(312f, 152f),
            density = 2f,
            apiLevel = Build.VERSION_CODES.S
        )
        assertEquals(CalendarElementViewMap.bindings.keys, plan.elements.map { it.elementId }.toSet())
        assertTrue(plan.diagnostics.isEmpty())
        assertEquals(24, plan.elements.first().padding[0])
    }

    @Test fun `pre 31 reports safe XML geometry fallback`() {
        val plan = CalendarRemoteViewsGeometry.plan(
            resolved(WidgetVariantId.Standard(4, 2)),
            CalendarRenderSize(312f, 152f),
            density = 2f,
            apiLevel = Build.VERSION_CODES.R
        )
        assertFalse(plan.elements.first().dynamicGeometrySupported)
        assertTrue(plan.diagnostics.any { it.minimumApi == Build.VERSION_CODES.S })
    }

    private fun resolved(variant: WidgetVariantId.Standard) = requireNotNull(
        WidgetDesignResolver.resolve(CalendarStudioDescriptor.defaultDesign(), variant).design
    )
}

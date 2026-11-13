package dev.revilodev.onewidget.widgets.battery

import android.os.Build
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.battery.BatteryStudioDescriptor
import dev.revilodev.onewidget.studio.model.WidgetDesignResolver
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.rendering.RemoteViewsGeometryCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryRemoteViewsRendererTest {
    @Test fun `production selects strips and both multi-device layouts from host bounds`() {
        val compact = BatteryRemoteViewsRenderer.plan(BatteryRenderSize(110f, 40f), design = null)
        val square = BatteryRemoteViewsRenderer.plan(BatteryRenderSize(110f, 110f), design = null)
        val short = BatteryRemoteViewsRenderer.plan(BatteryRenderSize(400f, 100f), design = null)
        val large = BatteryRemoteViewsRenderer.plan(BatteryRenderSize(250f, 101f), design = null)

        assertEquals(R.layout.widget_battery_compact, compact.appliedLayoutResId)
        assertEquals(R.layout.widget_battery_compact, short.appliedLayoutResId)
        assertEquals(R.layout.widget_battery_square, square.appliedLayoutResId)
        assertEquals(R.layout.widget_battery_large, large.appliedLayoutResId)
        assertNull(large.geometry)
    }

    @Test fun `studio and snapshot use deterministic battery design route`() {
        val design = resolved(WidgetVariantId.Standard(4, 2))
        val actual = BatteryRemoteViewsRenderer.plan(
            BatteryRenderSize(312f, 152f),
            design,
            BatteryRenderMode.STUDIO_ACTUAL_PREVIEW,
            apiLevel = Build.VERSION_CODES.S
        )
        val snapshot = BatteryRemoteViewsRenderer.plan(
            BatteryRenderSize(312f, 152f),
            design,
            BatteryRenderMode.SNAPSHOT,
            apiLevel = Build.VERSION_CODES.S
        )

        assertEquals(R.layout.widget_studio_battery, actual.appliedLayoutResId)
        assertEquals(actual.appliedLayoutResId, snapshot.appliedLayoutResId)
        assertEquals(4, snapshot.geometry?.elements?.size)
        assertFalse(snapshot.installTapActions)
    }

    @Test fun `sample runtime resolves fixed percentage charging and progress`() {
        val runtime = BatterySamples.runtime(R.drawable.phone)
        val content = BatteryContentResolver.resolve(runtime, BatteryWidget.PHONE)
        assertEquals("72%", content.percentageText)
        assertEquals("Charging", content.statusText)
        assertEquals(72, content.progressPercent)
        assertTrue(content.charging)
    }

    @Test fun `api 31 geometry includes visibility padding alignment and radius`() {
        val plan = BatteryRemoteViewsGeometry.plan(
            resolved(WidgetVariantId.Standard(4, 2)),
            BatteryRenderSize(312f, 152f),
            density = 2f,
            apiLevel = Build.VERSION_CODES.S
        )
        val progress = plan.elements.single { it.elementId == BatteryStudioDescriptor.PROGRESS }
        assertTrue(progress.visible)
        assertEquals(312f, progress.widthDp)
        assertEquals(32, progress.padding[0])
        assertEquals(13f, progress.cornerRadiusDp)
        assertEquals(0.5f, BatteryRemoteViewsGeometry.progressRadiusFraction(progress))
        assertTrue(plan.diagnostics.isEmpty())
    }

    @Test fun `pre 31 keeps XML radius and reports dynamic geometry fallback`() {
        val plan = BatteryRemoteViewsGeometry.plan(
            resolved(WidgetVariantId.Standard(4, 2)),
            BatteryRenderSize(312f, 152f),
            density = 3f,
            apiLevel = Build.VERSION_CODES.R
        )
        val progress = plan.elements.single { it.elementId == BatteryStudioDescriptor.PROGRESS }
        assertFalse(progress.dynamicGeometrySupported)
        assertEquals(0.5f, BatteryRemoteViewsGeometry.progressRadiusFraction(progress))
        assertTrue(plan.diagnostics.any { it.property == "cornerRadius" && it.minimumApi == Build.VERSION_CODES.S })
        assertFalse(RemoteViewsGeometryCompat.radiusPlan(9f, Build.VERSION_CODES.R).applied)
        assertTrue(RemoteViewsGeometryCompat.radiusPlan(9f, Build.VERSION_CODES.S).applied)
    }

    @Test fun `every battery element has a view binding`() {
        assertEquals(
            BatteryStudioDescriptor.create().elements.map { it.elementId }.toSet(),
            BatteryElementViewMap.bindings.keys
        )
    }

    private fun resolved(variant: WidgetVariantId.Standard) = requireNotNull(
        WidgetDesignResolver.resolve(BatteryStudioDescriptor.defaultDesign(), variant).design
    )
}

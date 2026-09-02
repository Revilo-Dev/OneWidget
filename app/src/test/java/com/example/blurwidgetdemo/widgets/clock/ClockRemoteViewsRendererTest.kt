package com.example.blurwidgetdemo.widgets.clock

import android.os.Build
import android.view.Gravity
import com.example.blurwidgetdemo.studio.clock.ClockStudioDescriptor
import com.example.blurwidgetdemo.studio.model.WidgetDesignResolver
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockRemoteViewsRendererTest {
    @Test fun `production plan preserves every existing font and category layout`() {
        val sizes = mapOf(
            ClockWidgetLayout.Category.COMPACT to ClockRenderSize(152f, 72f),
            ClockWidgetLayout.Category.MEDIUM to ClockRenderSize(296f, 128f),
            ClockWidgetLayout.Category.LARGE to ClockRenderSize(360f, 168f)
        )
        ClockFont.entries.forEach { font ->
            sizes.forEach { (category, size) ->
                val plan = ClockRemoteViewsRenderer.plan(
                    configuration = ClockWidgetSettings(font = font),
                    renderSize = size,
                    design = null,
                    mode = ClockRenderMode.PRODUCTION
                )
                val expected = ClockLayoutResolver.resolve(category, font)
                assertEquals(expected, plan.productionLayoutResId)
                assertEquals(expected, plan.appliedLayoutResId)
                assertEquals(category, plan.category)
                assertTrue(plan.installTapActions)
                assertEquals(
                    setOf(
                        android.R.id.background,
                        com.example.blurwidgetdemo.R.id.clock_content,
                        com.example.blurwidgetdemo.R.id.clock_time,
                        com.example.blurwidgetdemo.R.id.clock_date
                    ),
                    plan.tapActionViewIds
                )
                assertNull(plan.geometry)
                assertTrue(plan.diagnostics.isEmpty())
            }
        }
    }

    @Test fun `studio plan uses font-specific Studio layout and omits tap actions`() {
        val design = resolvedDesign(WidgetVariantId.Standard(4, 2))
        ClockFont.entries.forEach { font ->
            val plan = ClockRemoteViewsRenderer.plan(
                configuration = ClockWidgetSettings(font = font),
                renderSize = ClockRenderSize(312f, 152f),
                design = design,
                mode = ClockRenderMode.STUDIO_ACTUAL_PREVIEW,
                apiLevel = Build.VERSION_CODES.S
            )
            assertEquals(ClockStudioLayoutResolver.resolve(font), plan.appliedLayoutResId)
            assertNotEquals(plan.productionLayoutResId, plan.appliedLayoutResId)
            assertFalse(plan.installTapActions)
            assertTrue(plan.tapActionViewIds.isEmpty())
        }
    }

    @Test fun `snapshot plan preserves geometry while selecting deterministic sample data`() {
        val design = resolvedDesign(WidgetVariantId.Standard(4, 2))
        val plan = ClockRemoteViewsRenderer.plan(
            configuration = ClockWidgetConfigurationRepository.studioSample(),
            renderSize = ClockRenderSize(312f, 152f),
            design = design,
            mode = ClockRenderMode.SNAPSHOT,
            apiLevel = Build.VERSION_CODES.S
        )

        assertEquals(ClockRenderMode.SNAPSHOT, plan.mode)
        assertEquals(ClockSnapshotSamples.GALLERY, plan.snapshotSampleData)
        assertEquals("10:08", plan.snapshotSampleData?.time)
        assertEquals("'10:08'", ClockSnapshotSamples.literalPattern("10:08"))
        assertFalse(plan.installTapActions)
        assertTrue(plan.geometry?.elements?.isNotEmpty() == true)
    }

    @Test fun `time format combinations match legacy renderer`() {
        assertEquals(
            ClockTimePatterns("h:mm a", "HH:mm"),
            ClockTimeFormatResolver.resolve(ClockTimeFormat.SYSTEM, showAmPm = false, showSeconds = false)
        )
        assertEquals(
            ClockTimePatterns("h:mm:ss a", "HH:mm:ss"),
            ClockTimeFormatResolver.resolve(ClockTimeFormat.SYSTEM, showAmPm = true, showSeconds = true)
        )
        assertEquals(
            ClockTimePatterns("h:mm:ss", "h:mm:ss"),
            ClockTimeFormatResolver.resolve(ClockTimeFormat.TWELVE_HOUR, showAmPm = false, showSeconds = true)
        )
        assertEquals(
            ClockTimePatterns("HH:mm:ss", "HH:mm:ss"),
            ClockTimeFormatResolver.resolve(ClockTimeFormat.TWENTY_FOUR_HOUR, showAmPm = true, showSeconds = true)
        )
    }

    @Test fun `every registered clock element has a stable view binding`() {
        val descriptor = ClockStudioDescriptor.create()
        assertEquals(descriptor.elements.map { it.elementId }.toSet(), ClockElementViewMap.bindings.keys)
        ClockElementViewMap.bindings.values.forEach { binding ->
            assertNotEquals(0, binding.containerViewId)
            assertTrue(binding.contentViewIds.isNotEmpty())
            binding.contentViewIds.forEach { assertNotEquals(0, it) }
        }
    }

    @Test fun `api 31 geometry applies position size padding visibility and alignment without warnings`() {
        val plan = ClockRemoteViewsGeometry.plan(
            design = resolvedDesign(WidgetVariantId.Standard(4, 2)),
            renderSize = ClockRenderSize(312f, 152f),
            density = 2f,
            apiLevel = Build.VERSION_CODES.S
        )
        assertTrue(plan.diagnostics.isEmpty())
        assertEquals(4, plan.elements.size)
        assertTrue(plan.elements.all { it.sizeAndPositionSupported })
        val time = plan.elements.single { it.elementId == ClockStudioDescriptor.TIME }
        assertTrue(time.visible)
        assertEquals(312f, time.widthDp)
        assertEquals(0f, time.startMarginDp)
        assertEquals(52, time.paddingStartPx)
        assertEquals(Gravity.CENTER, time.contentGravity)
        val day = plan.elements.single { it.elementId == ClockStudioDescriptor.DAY }
        assertTrue(day.visible)
        val battery = plan.elements.single { it.elementId == ClockStudioDescriptor.BATTERY }
        assertEquals(0f, battery.startMarginDp)
        assertEquals(66f, battery.topMarginDp)
    }

    @Test fun `api 26 to 30 keeps safe geometry and reports unsupported size and position`() {
        val plan = ClockRemoteViewsGeometry.plan(
            design = resolvedDesign(WidgetVariantId.Standard(2, 1)),
            renderSize = ClockRenderSize(152f, 72f),
            density = 3f,
            apiLevel = Build.VERSION_CODES.R
        )
        assertEquals(12, plan.diagnostics.size)
        assertTrue(plan.elements.none { it.sizeAndPositionSupported })
        assertTrue(plan.diagnostics.all { it.minimumApi == Build.VERSION_CODES.S })
        assertEquals(
            setOf("width", "height", "position"),
            plan.diagnostics.map { it.property }.toSet()
        )
        val day = plan.elements.single { it.elementId == ClockStudioDescriptor.DAY }
        assertFalse(day.visible)
        val time = plan.elements.single { it.elementId == ClockStudioDescriptor.TIME }
        assertEquals(54, time.paddingStartPx)
    }

    @Test fun `dp conversion is deterministic`() {
        assertEquals(0, ClockRemoteViewsGeometry.dpToPx(0f, 3f))
        assertEquals(5, ClockRemoteViewsGeometry.dpToPx(2.75f, 2f))
        assertEquals(24, ClockRemoteViewsGeometry.dpToPx(8f, 3f))
    }

    private fun resolvedDesign(variant: WidgetVariantId.Standard) = requireNotNull(
        WidgetDesignResolver.resolve(ClockStudioDescriptor.defaultDesign(), variant).design
    )
}

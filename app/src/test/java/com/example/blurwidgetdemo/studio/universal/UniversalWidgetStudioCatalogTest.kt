package com.example.blurwidgetdemo.studio.universal

import com.example.blurwidgetdemo.studio.model.WidgetDesignResolver
import com.example.blurwidgetdemo.studio.model.WidgetDesignValidator
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import com.example.blurwidgetdemo.studio.production.ProductionSizeCategory
import com.example.blurwidgetdemo.studio.production.ProductionVariantResolver
import com.example.blurwidgetdemo.studio.registry.StudioElementVisualType
import com.example.blurwidgetdemo.studio.rendering.WidgetRenderSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalWidgetStudioCatalogTest {
    @Test fun `catalog covers every remaining home and system ui widget family`() {
        assertEquals(16, UniversalWidgetStudioCatalog.definitions.size)
        assertEquals(16, UniversalWidgetStudioCatalog.widgetIds.size)
        assertEquals(2, UniversalWidgetStudioCatalog.systemUiWidgetIds.size)
        assertEquals(
            UniversalWidgetIds.WEATHER,
            UniversalWidgetStudioCatalog.definitionForProvider(
                "com.example.blurwidgetdemo.widgets.weather.WeatherWidget"
            )?.widgetId
        )
        assertEquals(
            UniversalWidgetIds.GLASS,
            UniversalWidgetStudioCatalog.definitionForProvider(
                "com.example.blurwidgetdemo.BlurWidget"
            )?.widgetId
        )
    }

    @Test fun `every definition has namespaced unique elements and valid standard variants`() {
        UniversalWidgetStudioCatalog.definitions.forEach { definition ->
            val descriptor = definition.createDescriptor()
            assertEquals(25, descriptor.supportedVariants.size)
            assertNotNull(descriptor.renderer)
            assertEquals(
                descriptor.elements.size,
                descriptor.elements.map { it.elementId }.distinct().size
            )
            assertTrue(descriptor.elements.all { it.elementId.startsWith("${definition.widgetId}.") })
            assertTrue(
                WidgetDesignValidator.validate(
                    descriptor.defaultDesign,
                    descriptor.requiredElementIds
                ).isValid
            )
            WidgetVariantId.standardVariants.forEach { variant ->
                assertNotNull(WidgetDesignResolver.resolve(descriptor.defaultDesign, variant).design)
            }
        }
    }

    @Test fun `compact visibility rules preserve action icons and hide secondary content`() {
        val openApp = requireNotNull(UniversalWidgetStudioCatalog.definition(UniversalWidgetIds.OPEN_APP))
        val oneByOne = requireNotNull(
            WidgetDesignResolver.resolve(openApp.defaultDesign(), WidgetVariantId.Standard(1, 1)).design
        )
        assertTrue(oneByOne.elements.single { it.elementId == "open_app.icon" }.visible)
        assertEquals(0f, oneByOne.elements.single { it.elementId == "open_app.icon" }.xDp)
        assertFalse(oneByOne.elements.single { it.elementId == "open_app.label" }.visible)

        val media = requireNotNull(UniversalWidgetStudioCatalog.definition(UniversalWidgetIds.MEDIA_PLAYER))
        val twoByOne = requireNotNull(
            WidgetDesignResolver.resolve(media.defaultDesign(), WidgetVariantId.Standard(2, 1)).design
        )
        assertFalse(twoByOne.elements.single { it.elementId == "media_player.artist" }.visible)
        assertFalse(twoByOne.elements.single { it.elementId == "media_player.progress" }.visible)
        assertTrue(twoByOne.elements.single { it.elementId == "media_player.title" }.visible)
    }

    @Test fun `default geometry keeps every visible element within compact canvases`() {
        UniversalWidgetStudioCatalog.definitions.forEach { definition ->
            WidgetVariantId.standardVariants.forEach { variant ->
                val design = requireNotNull(
                    WidgetDesignResolver.resolve(definition.defaultDesign(), variant).design
                )
                val availableWidthDp = 56f + (variant.columns - 1) * 90f
                val availableHeightDp = 60f + (variant.rows - 1) * 100f
                design.elements.filter { it.visible }.forEach { element ->
                    assertTrue(
                        "${definition.widgetId}/${variant.stableId}/${element.elementId} exceeds width",
                        kotlin.math.abs(element.xDp) + element.widthDp / 2f <= availableWidthDp / 2f + 0.01f
                    )
                    assertTrue(
                        "${definition.widgetId}/${variant.stableId}/${element.elementId} exceeds height",
                        kotlin.math.abs(element.yDp) + element.heightDp / 2f <= availableHeightDp / 2f + 0.01f
                    )
                }
            }
        }
    }

    @Test fun `universal size profiles select their documented production variants`() {
        assertEquals(
            WidgetVariantId.Standard(1, 1),
            ProductionVariantResolver.resolve(UniversalWidgetIds.TOGGLE, 72f, 76f).variantId
        )
        assertEquals(
            WidgetVariantId.Standard(4, 1),
            ProductionVariantResolver.resolve(UniversalWidgetIds.STEP_COUNTER, 360f, 76f).variantId
        )
        assertEquals(
            ProductionSizeCategory.LARGE,
            ProductionVariantResolver.resolve(UniversalWidgetIds.CALENDAR_DISPLAY, 360f, 160f).category
        )
        assertEquals(
            WidgetVariantId.Standard(1, 1),
            ProductionVariantResolver.resolve(UniversalWidgetIds.LOCKSCREEN_MESSAGE_SMALL, 300f, 200f).variantId
        )
    }

    @Test fun `geometry plans every media slot and reports pre 31 fallback`() {
        val media = requireNotNull(UniversalWidgetStudioCatalog.definition(UniversalWidgetIds.MEDIA_PLAYER))
        val design = requireNotNull(
            WidgetDesignResolver.resolve(media.defaultDesign(), WidgetVariantId.Standard(4, 2)).design
        )
        val plan = UniversalRemoteViewsRenderer.plan(
            definition = media,
            design = design,
            renderSize = WidgetRenderSize(340f, 180f),
            mode = UniversalRenderMode.SNAPSHOT,
            density = 2f,
            apiLevel = 30
        )
        assertEquals(8, plan.elements.size)
        assertEquals(8, plan.elements.map { it.binding.wrapperId }.distinct().size)
        assertTrue(plan.diagnostics.any { it.contains("below API 31") })
        assertTrue(media.elements.any { it.visualType == StudioElementVisualType.PROGRESS })
        assertFalse(plan.installActions)
    }

    @Test fun `production is the only universal mode that installs actions`() {
        val alarm = requireNotNull(UniversalWidgetStudioCatalog.definition(UniversalWidgetIds.ALARM))
        val design = requireNotNull(
            WidgetDesignResolver.resolve(alarm.defaultDesign(), WidgetVariantId.Standard(2, 2)).design
        )
        val size = WidgetRenderSize(250f, 150f)
        UniversalRenderMode.entries.forEach { mode ->
            assertEquals(
                mode == UniversalRenderMode.PRODUCTION,
                UniversalRemoteViewsRenderer.plan(alarm, design, size, mode).installActions
            )
        }
    }
}

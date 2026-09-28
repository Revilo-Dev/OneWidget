package dev.revilodev.onewidget.widgets.variants

import android.appwidget.AppWidgetProvider
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.widgets.battery.BatteryWidget
import dev.revilodev.onewidget.widgets.calendar.CalendarDisplayWidget
import dev.revilodev.onewidget.widgets.calendar.CalendarWidget
import dev.revilodev.onewidget.widgets.clock.DigitalClockWidget
import dev.revilodev.onewidget.widgets.extra.AlarmWidget
import dev.revilodev.onewidget.widgets.extra.CounterWidget
import dev.revilodev.onewidget.widgets.extra.GoogleSearchWidget
import dev.revilodev.onewidget.widgets.extra.MediaPlayerWidget
import dev.revilodev.onewidget.widgets.extra.OpenAppWidget
import dev.revilodev.onewidget.widgets.extra.SamsungBrowserSearchWidget
import dev.revilodev.onewidget.widgets.extra.StepCounterWidget
import dev.revilodev.onewidget.widgets.extra.TextWidget
import dev.revilodev.onewidget.widgets.extra.TodoWidget
import dev.revilodev.onewidget.widgets.extra.ToggleWidget
import dev.revilodev.onewidget.widgets.extra.YouTubeSearchWidget
import dev.revilodev.onewidget.widgets.hydration.HydrationWidget
import dev.revilodev.onewidget.widgets.storage.StorageWidget
import dev.revilodev.onewidget.widgets.weather.WeatherWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class ExactWidgetVariantsTest {
    private val expectedSizes: Map<Class<out AppWidgetProvider>, List<Pair<Int, Int>>> = linkedMapOf(
        BlurWidget::class.java to listOf(2 to 1, 2 to 2, 4 to 2, 4 to 4, 4 to 5),
        DigitalClockWidget::class.java to listOf(2 to 1, 3 to 2, 4 to 2, 4 to 4),
        AlarmWidget::class.java to listOf(2 to 1, 3 to 2, 4 to 2),
        BatteryWidget::class.java to listOf(2 to 1, 4 to 1, 2 to 2, 4 to 2),
        WeatherWidget::class.java to listOf(2 to 1, 3 to 1, 4 to 1, 4 to 2),
        CalendarWidget::class.java to listOf(2 to 1, 3 to 1, 4 to 1, 2 to 2, 4 to 2),
        CalendarDisplayWidget::class.java to listOf(2 to 2, 4 to 2),
        StorageWidget::class.java to listOf(2 to 2, 4 to 2),
        HydrationWidget::class.java to listOf(2 to 1, 3 to 1, 4 to 1, 4 to 2),
        StepCounterWidget::class.java to listOf(2 to 1, 3 to 1, 4 to 1),
        GoogleSearchWidget::class.java to listOf(3 to 1, 4 to 1, 5 to 1, 6 to 1, 7 to 1),
        SamsungBrowserSearchWidget::class.java to listOf(3 to 1, 4 to 1, 5 to 1, 6 to 1, 7 to 1),
        YouTubeSearchWidget::class.java to listOf(3 to 1, 4 to 1, 5 to 1, 6 to 1, 7 to 1),
        MediaPlayerWidget::class.java to listOf(3 to 1, 2 to 2, 3 to 2),
        OpenAppWidget::class.java to listOf(2 to 1, 2 to 2, 4 to 2),
        TextWidget::class.java to listOf(2 to 1, 2 to 2, 4 to 2),
        TodoWidget::class.java to listOf(2 to 2, 4 to 2),
        CounterWidget::class.java to listOf(2 to 1, 2 to 2, 4 to 2),
        ToggleWidget::class.java to listOf(1 to 1, 2 to 1)
    )

    @Test
    fun `every app size resolves to its own native provider`() {
        expectedSizes.forEach { (base, sizes) ->
            sizes.forEach { (columns, rows) ->
                val provider = requireNotNull(ExactWidgetVariants.providerFor(base, columns, rows)) {
                    "Missing actual provider for ${base.simpleName} ${columns}x$rows"
                }
                assertNotEquals("$base $columns x $rows fell back to its legacy provider", base, provider)
                assertEquals(base, ExactWidgetVariants.baseFor(provider))
                assertEquals(base.name, ExactWidgetVariants.baseClassName(provider.name))
            }
            assertEquals(sizes.size + 1, ExactWidgetVariants.providersFor(base).size)
        }
    }

    @Test
    fun `unlisted sizes never select a fallback widget`() {
        expectedSizes.forEach { (base, sizes) ->
            for (columns in 1..8) for (rows in 1..6) {
                if (columns to rows !in sizes) {
                    assertNull("${base.simpleName} must not offer ${columns}x$rows",
                        ExactWidgetVariants.providerFor(base, columns, rows))
                }
            }
        }
    }

    @Test
    fun `every size has an enabled Android receiver and exact provider metadata`() {
        val receivers = xml("AndroidManifest.xml").elements("receiver")
        val registeredProviders = mutableSetOf<String>()
        expectedSizes.forEach { (base, sizes) ->
            sizes.forEach { (columns, rows) ->
                val provider = requireNotNull(ExactWidgetVariants.providerFor(base, columns, rows))
                assertTrue("Duplicate provider ${provider.name}", registeredProviders.add(provider.name))
                val receiver = receivers.single { receiver ->
                    receiver.android("name").let { name ->
                        (if (name.startsWith('.')) "dev.revilodev.onewidget$name" else name) == provider.name
                    }
                }
                assertNotEquals(provider.name, "false", receiver.android("enabled"))
                assertEquals(provider.name, "true", receiver.android("exported"))
                assertTrue(receiver.elements("action").any {
                    it.android("name") == "android.appwidget.action.APPWIDGET_UPDATE"
                })
                val info = receiver.elements("meta-data").single {
                    it.android("name") == "android.appwidget.provider"
                }.android("resource")
                val metadata = xml("res/xml/${info.substringAfter("@xml/")}.xml")
                assertEquals(provider.name, "appwidget-provider", metadata.tagName)
                assertEquals(provider.name, columns.toString(), metadata.android("targetCellWidth"))
                assertEquals(provider.name, rows.toString(), metadata.android("targetCellHeight"))
                // Native blur and resize support must survive sizing changes.
                val samsungNamespace = "http://schemas.android.com/apk/res-auto"
                assertEquals(provider.name, "colorful", metadata.getAttributeNS(samsungNamespace, "widgetStyle"))
                val resizeClasses = metadata.getAttributeNS(samsungNamespace, "widgetSize").split('|').toSet()
                assertTrue("${provider.name} must retain native One UI sizing", resizeClasses.none { it.isBlank() })
                if (sizes.any { it.second == 1 }) assertTrue(provider.name, "small" in resizeClasses)
                if (sizes.any { it.second >= 2 }) assertTrue(provider.name, "medium" in resizeClasses)
                if (sizes.any { it.first >= 3 && it.second >= 2 }) assertTrue(provider.name, "large" in resizeClasses)
                if (sizes.any { it.second >= 4 }) assertTrue(provider.name, "extraLarge" in resizeClasses)
                if (sizes.any { it.first >= 3 && it.second == 1 }) assertTrue(provider.name, "wideSmall" in resizeClasses)
                assertTrue(provider.name, metadata.getAttributeNS(samsungNamespace, "previewSize") in resizeClasses)
                val layout = metadata.android("initialLayout").removePrefix("@layout/")
                assertTrue("Missing live widget layout for ${provider.name}",
                    File(mainDir, "res/layout/$layout.xml").isFile)
                assertTrue("${provider.name} is hidden from the launcher picker",
                    "hide_from_picker" !in metadata.android("widgetFeatures"))

                // Every entry in the family must resize back to the smallest and up to the largest.
                val minColumns = sizes.minOf { it.first }
                val maxColumns = sizes.maxOf { it.first }
                val minRows = sizes.minOf { it.second }
                val maxRows = sizes.maxOf { it.second }
                assertEquals(provider.name, "${70 * columns - 30}dp", metadata.android("minWidth"))
                assertEquals(provider.name, "${70 * rows - 30}dp", metadata.android("minHeight"))
                assertEquals(provider.name, "${70 * minColumns - 30}dp", metadata.android("minResizeWidth"))
                assertEquals(provider.name, "${70 * maxColumns - 30}dp", metadata.android("maxResizeWidth"))
                assertEquals(provider.name, "${70 * minRows - 30}dp", metadata.android("minResizeHeight"))
                assertEquals(provider.name, "${70 * maxRows - 30}dp", metadata.android("maxResizeHeight"))
                assertTrue(provider.name, "horizontal" in metadata.android("resizeMode"))
                if (minRows != maxRows) assertTrue(provider.name, "vertical" in metadata.android("resizeMode"))

                val samsungInfo = receiver.elements("meta-data").single {
                    it.android("name") == "com.sec.android.appwidget.widgetinfo"
                }.android("resource").substringAfter("@xml/")
                val arrayName = xml("res/xml/$samsungInfo.xml")
                    .getAttribute("supportCellSizes").substringAfter("@array/")
                val allowedSizes = xml("res/values/widget_sizes.xml").elements("string-array")
                    .single { it.getAttribute("name") == arrayName }
                    .elements("item").map { it.textContent.trim() }
                assertEquals(provider.name, sizes.map { "${it.first}x${it.second}" }.toSet(), allowedSizes.toSet())
                assertEquals(provider.name, sizes.size, allowedSizes.size)
            }
        }
        assertEquals(67, registeredProviders.size) // 57 requested entries; Search has three providers.
    }

    private val mainDir = listOf(File("src/main"), File("app/src/main"))
        .first { File(it, "AndroidManifest.xml").isFile }

    private fun xml(path: String): Element = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder().parse(File(mainDir, path)).documentElement

    private fun Element.android(name: String) = getAttributeNS("http://schemas.android.com/apk/res/android", name)

    private fun Element.elements(tag: String): List<Element> = getElementsByTagName(tag).let { nodes ->
        (0 until nodes.length).map { nodes.item(it) as Element }
    }
}

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
import org.junit.Test

class ExactWidgetVariantsTest {
    private val expectedSizes: Map<Class<out AppWidgetProvider>, List<Pair<Int, Int>>> = linkedMapOf(
        BlurWidget::class.java to listOf(2 to 1, 2 to 2, 4 to 2, 4 to 4, 4 to 5),
        DigitalClockWidget::class.java to listOf(2 to 1, 3 to 2, 4 to 2, 4 to 4),
        AlarmWidget::class.java to listOf(2 to 1, 3 to 2, 4 to 2),
        BatteryWidget::class.java to listOf(4 to 1, 2 to 2, 4 to 2),
        WeatherWidget::class.java to listOf(2 to 1, 3 to 1, 4 to 1, 4 to 2),
        CalendarWidget::class.java to listOf(2 to 1, 3 to 1, 4 to 1, 2 to 2, 4 to 2),
        CalendarDisplayWidget::class.java to listOf(2 to 2, 4 to 2),
        StorageWidget::class.java to listOf(4 to 1, 2 to 2, 4 to 2),
        HydrationWidget::class.java to listOf(2 to 1, 3 to 1, 4 to 1, 4 to 2),
        StepCounterWidget::class.java to listOf(2 to 1, 3 to 1, 4 to 1),
        GoogleSearchWidget::class.java to listOf(4 to 1),
        SamsungBrowserSearchWidget::class.java to listOf(4 to 1),
        YouTubeSearchWidget::class.java to listOf(4 to 1),
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
                val provider = ExactWidgetVariants.providerFor(base, columns, rows)
                assertNotEquals("$base $columns x $rows fell back to its legacy provider", base, provider)
                assertEquals(base, ExactWidgetVariants.baseFor(provider))
                assertEquals(base.name, ExactWidgetVariants.baseClassName(provider.name))
            }
            assertEquals(sizes.size + 1, ExactWidgetVariants.providersFor(base).size)
        }
    }

    @Test
    fun `unsupported sizes retain the legacy provider only for compatibility`() {
        expectedSizes.keys.forEach { base ->
            assertEquals(base, ExactWidgetVariants.providerFor(base, 99, 99))
        }
    }
}

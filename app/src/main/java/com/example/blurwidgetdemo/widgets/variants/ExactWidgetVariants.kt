package dev.revilodev.onewidget.widgets.variants

import android.appwidget.AppWidgetProvider
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.widgets.battery.BatteryWidget
import dev.revilodev.onewidget.widgets.calendar.CalendarDisplayWidget
import dev.revilodev.onewidget.widgets.calendar.CalendarWidget
import dev.revilodev.onewidget.widgets.clock.DigitalClock2x1Widget
import dev.revilodev.onewidget.widgets.clock.DigitalClock3x2Widget
import dev.revilodev.onewidget.widgets.clock.DigitalClock4x2Widget
import dev.revilodev.onewidget.widgets.clock.DigitalClock4x4Widget
import dev.revilodev.onewidget.widgets.clock.DigitalClockWidget
import dev.revilodev.onewidget.widgets.extra.AlarmWidget
import dev.revilodev.onewidget.widgets.extra.CounterWidget
import dev.revilodev.onewidget.widgets.extra.GoogleSearchWidget
import dev.revilodev.onewidget.widgets.extra.MediaPlayer2x2Widget
import dev.revilodev.onewidget.widgets.extra.MediaPlayer3x1Widget
import dev.revilodev.onewidget.widgets.extra.MediaPlayer3x2Widget
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

class Glass2x1Widget : BlurWidget()
class Glass2x2Widget : BlurWidget()
class Glass4x2Widget : BlurWidget()
class Glass4x4Widget : BlurWidget()
class Glass4x5Widget : BlurWidget()

class Alarm2x1Widget : AlarmWidget()
class Alarm3x2Widget : AlarmWidget()
class Alarm4x2Widget : AlarmWidget()

class Battery4x1Widget : BatteryWidget()
class Battery2x2Widget : BatteryWidget()
class Battery4x2Widget : BatteryWidget()

class Weather2x1Widget : WeatherWidget()
class Weather3x1Widget : WeatherWidget()
class Weather4x1Widget : WeatherWidget()
class Weather4x2Widget : WeatherWidget()

class NextEvent2x1Widget : CalendarWidget()
class NextEvent3x1Widget : CalendarWidget()
class NextEvent4x1Widget : CalendarWidget()
class NextEvent2x2Widget : CalendarWidget()
class NextEvent4x2Widget : CalendarWidget()

class CalendarDisplay2x2Widget : CalendarDisplayWidget()
class CalendarDisplay4x2Widget : CalendarDisplayWidget()

class Storage4x1Widget : StorageWidget()
class Storage2x2Widget : StorageWidget()
class Storage4x2Widget : StorageWidget()

class Hydration2x1Widget : HydrationWidget()
class Hydration3x1Widget : HydrationWidget()
class Hydration4x1Widget : HydrationWidget()
class Hydration4x2Widget : HydrationWidget()

class StepCounter2x1Widget : StepCounterWidget()
class StepCounter3x1Widget : StepCounterWidget()
class StepCounter4x1Widget : StepCounterWidget()

class GoogleSearch4x1Widget : GoogleSearchWidget()
class SamsungBrowserSearch4x1Widget : SamsungBrowserSearchWidget()
class YouTubeSearch4x1Widget : YouTubeSearchWidget()

class OpenApp2x1Widget : OpenAppWidget()
class OpenApp2x2Widget : OpenAppWidget()
class OpenApp4x2Widget : OpenAppWidget()

class Text2x1Widget : TextWidget()
class Text2x2Widget : TextWidget()
class Text4x2Widget : TextWidget()

class Todo2x2Widget : TodoWidget()
class Todo4x2Widget : TodoWidget()

class Counter2x1Widget : CounterWidget()
class Counter2x2Widget : CounterWidget()
class Counter4x2Widget : CounterWidget()

class Toggle1x1Widget : ToggleWidget()
class Toggle2x1Widget : ToggleWidget()

/** Exact provider identities keep Android and One UI picker sizes aligned with the app gallery. */
private typealias Provider = Class<out AppWidgetProvider>

object ExactWidgetVariants {

    private val variantsByBase: Map<Provider, Map<Pair<Int, Int>, Provider>> = linkedMapOf(
        BlurWidget::class.java to mapOf(
            (2 to 1) to Glass2x1Widget::class.java,
            (2 to 2) to Glass2x2Widget::class.java,
            (4 to 2) to Glass4x2Widget::class.java,
            (4 to 4) to Glass4x4Widget::class.java,
            (4 to 5) to Glass4x5Widget::class.java
        ),
        DigitalClockWidget::class.java to mapOf(
            (2 to 1) to DigitalClock2x1Widget::class.java,
            (3 to 2) to DigitalClock3x2Widget::class.java,
            (4 to 2) to DigitalClock4x2Widget::class.java,
            (4 to 4) to DigitalClock4x4Widget::class.java
        ),
        AlarmWidget::class.java to mapOf(
            (2 to 1) to Alarm2x1Widget::class.java,
            (3 to 2) to Alarm3x2Widget::class.java,
            (4 to 2) to Alarm4x2Widget::class.java
        ),
        BatteryWidget::class.java to mapOf(
            (4 to 1) to Battery4x1Widget::class.java,
            (2 to 2) to Battery2x2Widget::class.java,
            (4 to 2) to Battery4x2Widget::class.java
        ),
        WeatherWidget::class.java to mapOf(
            (2 to 1) to Weather2x1Widget::class.java,
            (3 to 1) to Weather3x1Widget::class.java,
            (4 to 1) to Weather4x1Widget::class.java,
            (4 to 2) to Weather4x2Widget::class.java
        ),
        CalendarWidget::class.java to mapOf(
            (2 to 1) to NextEvent2x1Widget::class.java,
            (3 to 1) to NextEvent3x1Widget::class.java,
            (4 to 1) to NextEvent4x1Widget::class.java,
            (2 to 2) to NextEvent2x2Widget::class.java,
            (4 to 2) to NextEvent4x2Widget::class.java
        ),
        CalendarDisplayWidget::class.java to mapOf(
            (2 to 2) to CalendarDisplay2x2Widget::class.java,
            (4 to 2) to CalendarDisplay4x2Widget::class.java
        ),
        StorageWidget::class.java to mapOf(
            (4 to 1) to Storage4x1Widget::class.java,
            (2 to 2) to Storage2x2Widget::class.java,
            (4 to 2) to Storage4x2Widget::class.java
        ),
        HydrationWidget::class.java to mapOf(
            (2 to 1) to Hydration2x1Widget::class.java,
            (3 to 1) to Hydration3x1Widget::class.java,
            (4 to 1) to Hydration4x1Widget::class.java,
            (4 to 2) to Hydration4x2Widget::class.java
        ),
        StepCounterWidget::class.java to mapOf(
            (2 to 1) to StepCounter2x1Widget::class.java,
            (3 to 1) to StepCounter3x1Widget::class.java,
            (4 to 1) to StepCounter4x1Widget::class.java
        ),
        GoogleSearchWidget::class.java to mapOf((4 to 1) to GoogleSearch4x1Widget::class.java),
        SamsungBrowserSearchWidget::class.java to mapOf((4 to 1) to SamsungBrowserSearch4x1Widget::class.java),
        YouTubeSearchWidget::class.java to mapOf((4 to 1) to YouTubeSearch4x1Widget::class.java),
        MediaPlayerWidget::class.java to mapOf(
            (3 to 1) to MediaPlayer3x1Widget::class.java,
            (2 to 2) to MediaPlayer2x2Widget::class.java,
            (3 to 2) to MediaPlayer3x2Widget::class.java
        ),
        OpenAppWidget::class.java to mapOf(
            (2 to 1) to OpenApp2x1Widget::class.java,
            (2 to 2) to OpenApp2x2Widget::class.java,
            (4 to 2) to OpenApp4x2Widget::class.java
        ),
        TextWidget::class.java to mapOf(
            (2 to 1) to Text2x1Widget::class.java,
            (2 to 2) to Text2x2Widget::class.java,
            (4 to 2) to Text4x2Widget::class.java
        ),
        TodoWidget::class.java to mapOf(
            (2 to 2) to Todo2x2Widget::class.java,
            (4 to 2) to Todo4x2Widget::class.java
        ),
        CounterWidget::class.java to mapOf(
            (2 to 1) to Counter2x1Widget::class.java,
            (2 to 2) to Counter2x2Widget::class.java,
            (4 to 2) to Counter4x2Widget::class.java
        ),
        ToggleWidget::class.java to mapOf(
            (1 to 1) to Toggle1x1Widget::class.java,
            (2 to 1) to Toggle2x1Widget::class.java
        )
    )

    val allProviderClasses: List<Provider> = variantsByBase.entries
        .flatMap { (base, variants) -> listOf(base) + variants.values }
        .distinct()

    fun providerFor(base: Provider, columns: Int, rows: Int): Provider =
        variantsByBase[base]?.get(columns to rows) ?: base

    fun providersFor(base: Provider): List<Provider> =
        listOf(base) + variantsByBase[base].orEmpty().values

    fun baseFor(provider: Provider): Provider = variantsByBase.entries
        .firstOrNull { (base, variants) -> provider == base || provider in variants.values }
        ?.key ?: provider

    fun baseClassName(className: String?): String? {
        if (className == null) return null
        return variantsByBase.entries.firstOrNull { (base, variants) ->
            className == base.name || variants.values.any { it.name == className }
        }?.key?.name ?: className
    }

    fun isProviderFor(className: String?, base: Provider): Boolean =
        baseClassName(className) == base.name
}

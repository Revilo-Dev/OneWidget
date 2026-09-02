# Digital Clock rendering — Phase 3

## Pre-change rendering path

The production Digital Clock is updated by `DigitalClockWidget` from four entry paths:

- `onUpdate` for normal provider updates;
- `onAppWidgetOptionsChanged` for launcher resize events;
- time, date, locale, timezone, and boot broadcasts handled by `onReceive`;
- `DigitalClockConfigActivity` and `WidgetConfigActivity` after preferences are saved.

Every path reaches `DigitalClockWidget.updateWidget`. That method reads the existing per-widget values from `ClockWidgetPreferences`, derives the compact/medium/large category from the widget option minimum width and height, resolves one of the existing 42 font/category layouts through `ClockLayoutResolver`, configures its `RemoteViews`, and dispatches it with `AppWidgetManager.updateAppWidget`.

The size policy is unchanged: a height of 100 dp or less or a width below 250 dp is compact; otherwise a width of at least 330 dp or height of at least 150 dp is large; remaining sizes are medium. Missing launcher options retain the existing 110 × 40 dp defaults.

`ClockWidgetPreferences` reads the existing per-widget tint HSV/alpha, background enabled state, date/day/date-above flags, time format, AM/PM, seconds, alignment, tap action, font, text colour, text scale, and phone-battery flag. No key is migrated. The provider reads battery capacity from `BatteryManager` only when the battery row is enabled.

The production renderer configures a live `TextClock`. System format uses `h:mm[:ss] a` and `HH:mm[:ss]`; forced 12-hour uses the 12-hour pattern for both device modes, optionally without AM/PM; forced 24-hour uses `HH:mm[:ss]` for both. Time sizes remain 28/42/52 sp and secondary sizes remain 14/15/16 sp for compact/medium/large, scaled by the stored 50–150 percent setting. Date-above/date-below visibility is mutually exclusive. Day remains restricted to the large category. Compact or date-free content remains horizontally centred; otherwise the stored start/centre/end alignment is used while the full content stays vertically centred.

Tap actions retain the same intent resolution: Clock prefers Samsung Clock and falls back to `AlarmClock.ACTION_SHOW_ALARMS`; Calendar prefers the calendar time content URI and falls back to the platform calendar app category. The same immutable/update-current `PendingIntent` is attached to the background, content, time, and date targets.

The font/category resource matrix preserved by `ClockLayoutResolver` is:

| Font | Compact | Medium | Large |
|---|---|---|---|
| System | `widget_digital_clock_compact` | `widget_digital_clock_medium` | `widget_digital_clock_large` |
| Samsung Default | `widget_digital_clock_compact_samsung_default` | `widget_digital_clock_medium_samsung_default` | `widget_digital_clock_large_samsung_default` |
| Samsung Default Bold | `widget_digital_clock_compact_samsung_default_bold` | `widget_digital_clock_medium_samsung_default_bold` | `widget_digital_clock_large_samsung_default_bold` |
| Samsung Default Thin | `widget_digital_clock_compact_samsung_default_thin` | `widget_digital_clock_medium_samsung_default_thin` | `widget_digital_clock_large_samsung_default_thin` |
| Samsung Mono | `widget_digital_clock_compact_samsung_mono` | `widget_digital_clock_medium_samsung_mono` | `widget_digital_clock_large_samsung_mono` |
| Clock Bold Serif | `widget_digital_clock_compact_clock_bold_serif` | `widget_digital_clock_medium_clock_bold_serif` | `widget_digital_clock_large_clock_bold_serif` |
| Clock Stripe | `widget_digital_clock_compact_clock_stripe` | `widget_digital_clock_medium_clock_stripe` | `widget_digital_clock_large_clock_stripe` |
| Clock Stamp | `widget_digital_clock_compact_clock_stamp` | `widget_digital_clock_medium_clock_stamp` | `widget_digital_clock_large_clock_stamp` |
| Alatsi | `widget_digital_clock_compact_alatsi` | `widget_digital_clock_medium_alatsi` | `widget_digital_clock_large_alatsi` |
| Capriola | `widget_digital_clock_compact_capriola` | `widget_digital_clock_medium_capriola` | `widget_digital_clock_large_capriola` |
| Fredericka | `widget_digital_clock_compact_fredericka` | `widget_digital_clock_medium_fredericka` | `widget_digital_clock_large_fredericka` |
| Lato | `widget_digital_clock_compact_lato` | `widget_digital_clock_medium_lato` | `widget_digital_clock_large_lato` |
| Stardos Stencil | `widget_digital_clock_compact_stardos_stencil` | `widget_digital_clock_medium_stardos_stencil` | `widget_digital_clock_large_stardos_stencil` |
| Modak | `widget_digital_clock_compact_modak` | `widget_digital_clock_medium_modak` | `widget_digital_clock_large_modak` |

The root of every existing clock layout is `@android:id/background` with `widget_clock_background`. `BlurWidget.applySurfaceColor` targets that root. This is the One UI Home native-blur contract and must not be wrapped or replaced.

Before Phase 3, Widget Studio's EDIT canvas was deliberately independent from this provider path. It rendered a manipulable approximation from `WidgetDesignSpec`; it did not inflate the production `RemoteViews` and it did not alter production preferences.

## Phase 3 boundary

Phase 3 introduces one `ClockRemoteViewsRenderer` used by both callers:

- production passes its persisted `ClockWidgetSettings`, launcher-derived `ClockRenderSize`, and `design = null`;
- Studio ACTUAL passes an isolated sample configuration, its selected canvas size, and the resolved selected variant design.

With `design = null`, the renderer continues to use `ClockLayoutResolver` and the unchanged production layouts. With a design, it selects a Studio-capable font layout containing stable element wrappers and applies supported `RemoteViews` geometry. These Studio-only layouts retain the same blur-compatible root. They do not replace production layouts.

Studio ACTUAL displays the returned object with `RemoteViews.apply`. Studio EDIT remains the custom interactive canvas. Production never reads Widget Studio storage, and Studio preview never writes widget preferences or calls `AppWidgetManager.updateAppWidget`.

Geometry that requires API 31 is planned explicitly. On API 26–30 the renderer still applies safe properties such as visibility, text, colour, padding, and alignment, skips unsupported size/position calls, and reports structured diagnostics instead of failing.

Studio padding is converted from dp to host pixels and applied once to each element wrapper; the Studio XML starts with zero wrapper padding. Text gravity is applied to the wrapper's full-size content child, independently from the wrapper anchor. On API 31+, width and height become explicit layout dimensions and the shared Phase 2 anchor calculation determines the wrapper centre. Start/top margins are calculated as offsets from the layouts' safe centred XML fallback, producing that exact centre. `MATCH` resolves to the selected canvas dimension and `FIXED` resolves to its stored dp value. Because `RemoteViews` cannot measure a `WRAP` child before building its actions, `WRAP` uses a deterministic sample-content estimate; diagnostics show the resulting bounds.

On API 26–30 the Studio layouts retain safe centred XML dimensions. Visibility, padding, gravity, live `TextClock` content, colour, and tint are still applied. Width, height, anchor, and x/y remain XML-driven, with one structured diagnostic per ignored property and element. No reflection is used.

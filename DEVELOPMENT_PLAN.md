# Development plan

## Current architecture

The app is a single Android module using Kotlin and classic `AppWidgetProvider` +
`RemoteViews`. `BlurWidget` owns the current resizable home-screen widget and
selects one of three layouts from the widget's allocated size. `WidgetConfigActivity`
stores per-widget tint HSV values and opacity in `SharedPreferences`, then asks the
provider to redraw the affected widget. The remaining activities provide onboarding
and attribution. Separate receivers demonstrate Samsung lock-screen messages and
are intentionally outside the home-screen blur widget's update path.

No packages are split yet: the present codebase has one home-screen widget and a
configuration screen, so moving classes into `widgets/`, `common/`, `configuration/`,
and `data/` would add churn without reducing complexity. Introduce those packages
only as a second widget or shared widget state/update contract actually needs them.

## Essential Samsung native-blur requirements

For the current home-screen widget, keep all of the following intact:

1. `AndroidManifest.xml` registers `BlurWidget` as an app-widget receiver and
   points it at `@xml/widget_provider_blur`.
2. `widget_provider_blur.xml` declares `app:widgetStyle="colorful"` and a real,
   non-empty `app:widgetSize` value.
3. `values/attrs.xml` declares those Samsung attribute names and flags so the
   provider XML can compile and One UI Home can read its metadata.
4. Every layout `BlurWidget` can select has a root view with the exact ID
   `@android:id/background` and a background whose alpha remains in the range
   1–254. The provider updates that view through `RemoteViews`.

`widget_supported_cell_size_info.xml`, picker previews, and resize metadata improve
One UI picker and resize behaviour, but are not themselves the blur trigger.
`WidgetConfigActivity` is required for the current user-controlled tint and opacity,
but not for the launcher to apply blur.

## Compatibility risks

- Samsung does not document this behavior; One UI Home may change it by device,
  firmware, launcher version, or future update.
- Native blur requires Samsung One UI Home on One UI 7.0+; other launchers should
  display the ordinary translucent RemoteViews background.
- A fully opaque or fully transparent background, a missing background ID, or a
  missing Samsung metadata attribute can silently remove the effect.
- Android preview tools and emulators cannot prove wallpaper compositing. Validate
  on physical Samsung hardware before release.

## Proposed widget architecture

When the second widget is added, move only shared, demonstrated concerns into:

- `widgets/`: each provider, its layouts, metadata, and widget-specific renderer.
- `common/`: small RemoteViews-safe helpers such as the native-blur surface and
  shared size classification, once two providers use them.
- `configuration/`: configuration activities and shared configuration UI code.
- `data/`: per-widget preference keys/repositories and future data sources.

Keep providers on `AppWidgetProvider` and layouts on `RemoteViews`; do not migrate
the Samsung blur path to Glance.

## Digital Clock (Stage 2)

`DigitalClockWidget` is a second, independent `AppWidgetProvider`. It selects
compact, medium, or large RemoteViews layouts through `ClockWidgetLayout`, while
`ClockWidgetPreferences` stores appearance and content choices per app-widget ID.
Each layout uses `TextClock`: Android keeps its time and date text current without
a WorkManager loop or exact alarms. The provider reapplies settings after boot and
time, time-zone, locale, and date broadcasts. The clock's forced 12/24-hour modes
set both TextClock format branches to the selected format; system mode retains
different 12- and 24-hour patterns.

Known limitations: the final look of launcher blur, rounded clipping, and clock or
calendar intent resolution varies by launcher and must be confirmed on device.
The date/day controls are intentionally omitted in compact layouts where space is
limited; no seconds or background network updates are used.

### Clock fonts and RemoteViews

The configuration preview is a local Activity view, so it can load a `Typeface`
directly. A home-screen widget is inflated in the launcher process, and
`RemoteViews` cannot send an arbitrary Typeface object to `TextClock`. The selected
font is therefore encoded in the full XML layout selected by `ClockLayoutResolver`;
each font/size variant sets `android:fontFamily` directly on every visible
`TextClock`. Adding a font requires a bundled, lowercase `res/font` resource, three
matching compact/medium/large layouts retaining the same IDs and blur root, and an
entry in the resolver. Validate each font on a physical launcher after resizing and
reconfiguration. This repository does not document the redistribution licences of
the bundled fonts, so confirm commercial-use terms before a Play Store release.

## Recommended next five stages

1. Extract tested shared widget appearance/preferences helpers while preserving the
   existing provider's component name and XML metadata.
2. Add a digital-clock widget with the same native-blur contract and a lightweight
   periodic update strategy.
3. Add a date widget and shared responsive layout rules.
4. Add battery and device-storage widgets, including permission-free storage data
   handling and clear unavailable states.
5. Add calendar/upcoming-events widgets with explicit runtime permission UX and
   robust empty/error states.

## Manual tests on a Samsung One UI 7.0+ phone

1. Install the debug APK, add the widget from One UI Home, and confirm the
   configuration activity opens.
2. Set a distinctive wallpaper; compare the widget over multiple wallpaper areas
   and confirm native wallpaper blur follows the area behind the widget.
3. Change colour and each opacity preset, save, remove/re-add, and reboot the phone;
   confirm per-widget settings remain correct.
4. Resize horizontally and vertically through each compact and large threshold;
   confirm the layout changes without losing the blur surface or tint.
5. Add two instances with different tint/opacity values and verify they remain
   independent.
6. Repeat a basic placement test on a non-Samsung launcher/device and verify the
   translucent fallback remains readable and stable.
7. For Digital Clock: test system 12/24-hour settings and forced format choices,
   all three resize categories, two differently configured instances, cancellation,
   removal/re-addition, reboot, time-zone and locale changes, and light/dark
   wallpapers on both Samsung One UI Home and a non-Samsung launcher.

# OneWidget

Configurable home screen widgets by [Revilo Dev](https://github.com/Revilo-Dev), version **1.0.0**.

[![Android CI](https://github.com/Revilo-Dev/OneWidget/actions/workflows/android.yml/badge.svg)](https://github.com/Revilo-Dev/OneWidget/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

OneWidget combines Samsung One UI wallpaper blur with clocks, battery, storage, weather, calendar, media, search, app shortcuts, text, toggles, steps, hydration, counters and to-do widgets. Android 8.0 or newer is required. Native wallpaper blur depends on a compatible Samsung One UI Home version; other launchers display the configured background without Samsung blur.

## Appearance

Every home screen widget uses the same configuration screen, with its own complication controls alongside shared tint, background opacity, presets, borders and background images.

- The supplied 2×1, 2×2, 3×2, 4×2, 4×4 and 4×5 artwork defines the widget silhouettes for those sizes. Tint, opacity and border settings control their finish. Samsung Home’s reported cell spans take priority over size estimates.
- The app follows the device palette by default. Turn off **Use system color palette** in Widget appearance to use OneWidget’s fixed accent.
- Borders default to white at **20% opacity and 4 physical pixels**. The inset stroke fades from white at the top to transparent at the bottom and updates when appearance settings change.
- The widget gallery has animated category and widget dropdowns with dedicated subcategory icons, including each toggle. Calendar display uses the Calendar category icon. Glass’s Custom size row opens the size picker directly. The bottom-right search button expands into a field that filters by category or widget name.
- Settings provides default tint, opacity, border thickness and border opacity. Individual widgets can override border settings or return to the app defaults.
- Appearance presets can be added, renamed, edited and deleted in Settings. A preset stores its colour, opacity and border style. Widgets linked to an edited preset update with it; changing their appearance manually detaches the preset.
- Custom background images are available for every home screen widget. Images are copied into private app storage after bounded decoding and cropped to the widget shape. Tint and opacity control the colour wash above the image. Replacing an image does not depend on the original photo remaining available.
- Clock, weather, battery, storage, calendar, text and other complication settings remain separate from the shared surface style.

## Resizing

Glass supports all **42 combinations from 2 × 1 through 7 × 7**. Expand Glass in the gallery and choose **Custom size**, or resize a placed widget with the launcher handles.

Providers in each family share resize bounds and Samsung supported cell sizes, including intermediate dimensions. Widgets read current host dimensions instead of treating their original gallery selection as a fixed size. Android 12 and newer can receive separate surface renderings for reported host sizes so border shapes remain correct across orientations.

The launcher controls placement, the available home screen grid and resize snapping. A 7 × 7 widget requires a grid with that much space. Samsung preset labels and custom grids may differ from the app gallery. See [One UI widget sizing](ONE_UI_WIDGET_SIZING.md) for the native blur requirements and hardware verification procedure.

## Live information and resource use

Media elapsed time uses a native Chronometer during normal speed playback.

Clocks use Android TextClock with packaged font layouts, including seconds when enabled. They advance without per-second provider alarms or bitmap clock redraws.

Storage data usage shows measured traffic since restart rather than assuming a mobile plan allowance.

Active information widgets share a non-waking, inexact five minute refresh schedule. Battery and calendar events and screen activation also request updates while the app process is alive. Boot, package updates, date, time zone and locale changes restore or refresh widget state. Background scheduling can be delayed by Android power management; the app does not request exact alarm access to refresh widgets.

Weather requests are shared across widget instances, cached for thirty minutes and bounded by connection, read and location timeouts. Failed requests are throttled, cached conditions remain visible, and old forecasts are marked outdated. Calendar permission, location access, Bluetooth access and activity recognition are requested only for the complications that need them.

Images and surface bitmaps use bounded decoding, memory budgets and caches. The to-do widget uses modern collection updates on Android 12 and newer, with a legacy adapter for older Android versions. Daily clearing begins at the next configured reset boundary rather than clearing a list immediately when enabled.

## Build and verify

Open the project in Android Studio with SDK 36 and the bundled JDK. The project uses Android Gradle Plugin 9.3.1, Kotlin 2.2.20 and Java 17 source compatibility.

The One UI library is published through GitHub Packages. Copy [github.properties.example](github.properties.example) to a gitignored github.properties file and provide a GitHub username and token with read:packages access. CI uses GH_PACKAGES_USER and GH_PACKAGES_TOKEN. Never commit credentials.

Run these Gradle tasks from the repository root:

~~~text
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
~~~

On Windows use gradlew.bat. Connected tests require a running emulator or Android device. The debug APK is written to app/build/outputs/apk/debug/app-debug.apk. Install it with the installDebug Gradle task.

Tests cover provider registration, resize metadata, renderer plans, geometry, appearance storage and Android RemoteViews rendering. Verify native Samsung blur, launcher masks and placement on a physical Samsung device before releasing; a Pixel emulator cannot establish those behaviors.

For release signing, use the existing gitignored keystore.properties configuration or RELEASE_* CI secrets. An unsigned release build is not a published release. The repository workflows provide CI and release packaging.

## Contributing

Follow [AGENTS.md](AGENTS.md) for widget architecture, validation and commenting rules. Comments are single line, lowercase, without punctuation or emojis, and explain useful intent without repeating the code. Studio geometry and sample preview data are isolated from production complication state; see [Widget Studio architecture](WIDGET_STUDIO_ARCHITECTURE.md).

## Credits and links

- [OneWidget repository](https://github.com/Revilo-Dev/OneWidget)
- [Revilo Dev on GitHub](https://github.com/Revilo-Dev)
- [X · @revilodev](https://x.com/revilodev)
- [Original blur widget project by Josh Skinner](https://github.com/thatjoshguy67/blur-widget-demo) and [thatjoshguy](https://tjg.gg)
- [oneui-design](https://github.com/tribalfs/oneui-design) and [One UI Project](https://github.com/OneUIProject) for interface components and icons
- [Open-Meteo](https://open-meteo.com/) for weather data

[MIT](LICENSE). Original copyright and dependency credits are preserved.

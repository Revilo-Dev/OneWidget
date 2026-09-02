# Widget Studio Phase 8

Phase 8 registers Storage as the third complete Widget Studio widget. It reuses the
Phase 7 verification, immutable promotion, per-instance production mode, snapshot,
gallery, and Developer Test Widget paths without weakening their isolation.

## Shared Storage renderer

`StorageWidget` now adapts existing preferences and live device values, then calls
`StorageRemoteViewsRenderer`. With `design = null`, the renderer keeps the original
compact/large layouts, the height-based compact threshold, text/detail settings,
metric colours, gradients, native-blur surface, and optional Manage Storage action.

The active configuration preview also uses this renderer from unsaved in-memory
controls. It does not write `widget_prefs`; if local `RemoteViews.apply()` fails, the
existing legacy preview remains the diagnosed fallback. External actions are
installed only in production mode.

Runtime values remain outside `WidgetDesignSpec`:

- storage uses app-visible filesystem totals from `StatFs`;
- memory uses `ActivityManager.MemoryInfo`;
- data usage keeps the existing device traffic-since-boot value against the existing
  nominal 10 GB display scale and is not carrier billing-cycle usage;
- Studio, snapshots, and the Developer Test Widget use deterministic 62% / 58% /
  34% sample values.

## Studio geometry

Storage has four stable composite metric-row elements:

- `storage.primary`: the configured compact metric;
- `storage.storage`: the large Storage row;
- `storage.memory`: the large Memory row;
- `storage.data`: the large Data usage row, or the Manage Storage replacement when
  the user's Hide data setting is enabled.

A row is the honest RemoteViews editing unit because its icon, composite progress
bitmap, and overlaid label are rendered together. Every row exposes visibility,
position, size, padding, alignment, and corner radius. One-row variants show the
compact element; taller variants show the three large rows. All 25 standard variants
remain resolvable.

Dynamic size, position, and outline radius use the existing API 31 compatibility
boundary. Older Android versions retain safe XML geometry and report diagnostics.
The progress bitmap itself is still produced by `WidgetProgressBarRenderer`.

## Promotion and production safety

Storage uses the same reviewed production mapping as Clock and Battery:

| Category | Studio variant |
| --- | --- |
| Compact | 2x1 |
| Medium | 2x2 |
| Large | 4x2 |

Promotion requires a saved valid design, renderer bindings, mapped variants,
revision-matched verification/snapshot review, diff review, and explicit
confirmation. Storage verification additionally requires progress rendering and,
on API 31+, progress corner radius.

An absent per-instance mode remains `LEGACY_DEFAULT`. Existing Storage instances
therefore keep built-in geometry after an app upgrade. Only a newly configured
instance can initialize to `PROMOTED_DEFAULT`, and only when an active immutable
promotion exists and the developer has enabled promoted designs for new Storage
widgets. Deletion removes only that instance's mode key.

## Gallery and scope

Storage gallery tiles now resolve developer and production snapshots through the
generic Phase 7 priority rules. Developer artifacts remain unavailable when
Developer Mode is off, and stale production snapshots are never shown.

No Weather, Calendar, Toggle, Counter, Media, Search, Hydration, or lock-screen
widget was added to Studio. No new permission was introduced. Samsung native blur
metadata and root application are unchanged and still require physical One UI Home
verification.

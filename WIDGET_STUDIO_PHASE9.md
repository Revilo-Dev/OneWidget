# Widget Studio Phase 9

Phase 9 registers the existing Next event Calendar widget as the fourth complete
Widget Studio widget. Calendar Display remains separate and unchanged.

## Shared Calendar renderer

`CalendarWidget` now reads its existing appearance settings and launcher dimensions,
loads one explicit runtime content state, and delegates all `RemoteViews` work to
`CalendarRemoteViewsRenderer`.

With `design = null`, the renderer preserves the existing height-based compact
threshold, compact/large XML layouts, text sizing and alignment, native-blur surface,
30-minute provider refresh metadata, and Calendar launch action. External actions
are installed only in production mode.

Calendar content is now modeled as one of five states:

- event;
- no upcoming event in the seven-day window;
- calendar permission required;
- provider unavailable;
- temporary query error.

Missing titles become `Untitled event`. A denied or revoked permission is detected
before the provider query and remains separately distinguishable from empty data.
The configuration activity owns the runtime permission prompt; no permission was
added. Denial produces explicit guidance and still allows configuration to finish,
so the widget can expose its permission-required state instead of silently cancelling
the add action. Keeping the request at this single launcher boundary avoids duplicate
prompts when adding from the in-app gallery.

Configuration preview uses the same shared renderer with unsaved appearance controls
and deterministic sample content. Studio, snapshots, and the Developer Test Widget
also use `Team catch-up` / `Starts 10:00 AM`, so personal calendar data never enters
developer images or design files.

## Studio geometry

Calendar exposes two stable, required elements:

- `calendar.title`;
- `calendar.details`.

Both expose visibility, position, size, padding, and text alignment. One-row variants
hide Details and center Title. Taller variants show both. All 25 standard variants
remain resolvable.

API 31+ applies dynamic RemoteViews size and position. Android 8-11 retain a safe XML
fallback that centers the title and keeps details separately positioned; diagnostics
identify ignored dynamic geometry.

## Verification schema and promotion

Device-verification schema 2 adds `contentStatesVerified`. Schema 1 Clock, Battery,
and Storage records migrate in memory with this new check unset and remain usable.
Only Calendar requires the new event/empty/permission/unavailable device check.

Calendar uses the existing reviewed production mapping:

| Category | Studio variant |
| --- | --- |
| Compact | 2x1 |
| Medium | 2x2 |
| Large | 4x2 |

Promotion continues to require a saved valid design, renderer bindings, mapped
variants, revision-matched verification and snapshot review, model diff, compatibility
review, and explicit confirmation. Warning overrides do not bypass invalid designs.

Existing Calendar widget IDs have no production-mode key and therefore remain legacy.
A configuration completion initializes a new instance to promoted geometry only when
an active immutable Calendar promotion exists and promoted defaults are enabled.
Deleting the widget removes only its instance-mode key.

## Scope

Calendar Display, Weather, Toggle, Counter, Media, Search, Hydration, and lock-screen
widgets are not onboarded. Samsung native blur behavior and metadata are unchanged;
physical One UI Home verification remains mandatory.

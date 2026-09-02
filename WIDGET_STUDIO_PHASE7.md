# Widget Studio Phase 7

Phase 7 adds device-parity records and an explicit reviewed path from mutable Widget Studio work to immutable production defaults. Digital Clock and Battery are the only supported Studio widgets.

## Isolation boundaries

- Mutable developer designs remain in `files/widget_studio/designs/`.
- Revision-specific device checks are stored as human-readable JSON in `files/widget_studio/verifications/`.
- Promoted designs are copied to `files/widget_designs/production/` and checked with a SHA-256 integrity hash.
- Production snapshots are copied to `files/widget_designs/production_snapshots/`; they never alias the developer PNG/metadata store.
- Production providers read only `ProductionWidgetDesignRepository`. The Developer Test Widget continues to read `WidgetDesignRepository`.

## Verification policy

Verification is keyed by widget, variant, and exact design revision. A record for revision 12 is stale for revision 13. No check is completed automatically.

Clock promotion requires Developer Test Widget, geometry, visibility, padding, alignment, native blur, and snapshot checks for every production-mapped variant. Battery additionally requires progress rendering and, on API 31 or later, progress corner radius. An explicit failed state remains distinct from partial and not-tested states.

Device capture records only manufacturer, model, Android release/API, the public One UI Home package version when visible, and an optional manually entered One UI version. No new broad permission is used.

## Promotion and production instances

The production mapping is centralized and reviewable:

| Category | Studio variant |
| --- | --- |
| Compact | 2x1 |
| Medium | 2x2 |
| Large | 4x2 |

Promotion requires a saved, valid design, a registered renderer, resolvable mapped variants, a verification review, a current-snapshot review, a model-level diff, and explicit confirmation. Incomplete verification and missing/stale snapshots require the warning override in the confirmation dialog. Compatibility fallbacks, including pre-API-31 Battery radius behavior, are shown before confirmation.

Each production app-widget ID has an independent mode:

- `LEGACY_DEFAULT` passes `design = null` and preserves built-in behavior.
- `PROMOTED_DEFAULT` resolves the active immutable production copy for the launcher dimensions.

An absent key is always legacy, protecting widgets installed before Phase 7. Completing configuration for a newly added widget initializes its mode. After promotion, the developer toggle defaults to using promoted geometry for new widgets. Existing instances are not updated or opted in automatically.

Production history retains the latest ten revisions. A prior revision can be reactivated, or the active pointer can be cleared to return to the legacy built-in design without deleting history.

## Gallery order

Developer Mode ON:

1. current developer snapshot;
2. stale developer snapshot;
3. current production snapshot;
4. packaged preview;
5. legacy scaled preview;
6. generic fallback.

Developer Mode OFF:

1. current production snapshot;
2. packaged preview;
3. legacy scaled preview;
4. generic fallback.

Stale production snapshots are never shown. Developer and production entries also use distinct memory-cache keys.

## Scope boundary

No Weather, Storage, Calendar, Toggle, Counter, Media, Search, or lock-screen widget was added to Studio. Samsung native blur application remains unchanged and must be verified on real One UI Home.

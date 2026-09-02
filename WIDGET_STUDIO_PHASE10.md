# Widget Studio Phase 10

Phase 10 completes Widget Studio coverage for the app. The default registry now has
20 descriptors: all 18 Home-screen widget families and both Samsung lock-screen
message sizes.

## Complete registry

Purpose-built renderers remain in place for Digital clock, Battery, Storage, and Next
event. The reusable Phase 10 renderer adds:

- Glass and Alarm;
- Weather and Calendar display;
- Step counter and Hydration;
- Google, Samsung Browser, and YouTube search;
- Media player;
- Open app, Text, and Counter;
- the shared Toggle family;
- lock-screen message 1×1 and 2×1.

Search providers intentionally have separate widget IDs because they launch different
targets. All nine toggle choices share one layout design because they are runtime
configurations of the same provider.

## Reusable renderer

Each descriptor exposes up to eight widget-namespaced semantic elements. A universal
RemoteViews layout contains independent text/image slots, so visibility, position,
fixed or inherited size, padding, text alignment, z-order metadata, and applicable
corner radius can follow the same design schema as the first four widgets.

API 31+ applies dynamic slot dimensions and margins. Android 8-11 intentionally uses
a vertical non-overlapping fallback and reports that exact geometry was unavailable.
Samples are deterministic and actions are removed in Studio, snapshot, and Developer
Test Widget modes. Default variant geometry is scaled and bounded to the selected
canvas, including the compact 1×1 and 2×1 presets; saved user geometry remains the
source of truth after editing.

## Production behavior

Every Home-screen provider now checks its per-instance production mode. Legacy mode
continues to inflate the original widget layouts without behavior changes. Promoted
mode uses the immutable production design while supplying the same live values and
actions:

- weather conditions/location and explicit permission state;
- alarm, hydration, counter, and toggle commands;
- step permission and sensor values;
- calendar month/grid content;
- media metadata, artwork, playback progress, and transport commands;
- configured app, text, and search destinations.

Configuration completion initializes only the new/reconfigured instance. The in-app
pin success callback covers providers without a former configuration boundary. Next
event and Media now use the shared configuration activity as well, so direct launcher
placement has a safe initialization boundary. Deletion removes only the matching
per-instance production-mode key.

Calendar, Weather, and Step counter allow placement after permission denial and render
an explicit permission-needed state. Permission prompts remain in configuration
activities to avoid duplicate requests.

## Verification schema 3

Runtime-state widgets expose the content-state verification check. Media also requires
progress rendering and radius checks. Schema 3 adds `systemUiHostVerified`; schema 1
and 2 records load with the new field unset.

The two Samsung lock-screen descriptors support editing, ACTUAL preview, Developer
Test Widget rendering, snapshots, and host-specific verification. Production promotion
is blocked because Samsung System UI uses monotone lock/AOD/cover ServiceBox contracts
and does not provide the Home-screen per-instance opt-in boundary. This prevents a
Home-oriented promoted layout from being silently shipped to System UI.

Physical Samsung verification remains mandatory before claiming parity.

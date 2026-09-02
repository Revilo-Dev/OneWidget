# Widget Studio foundation

Widget Studio is a developer-only geometry system layered alongside the existing production
`AppWidgetProvider` and `RemoteViews` architecture. Phase 1 does not alter production rendering.

## Schema and design model

The current design schema is `schemaVersion = 1`. `WidgetDesignSpec` contains:

- a stable `widgetId`;
- a monotonically increasing `revision`;
- one `baseVariant` containing the complete element list;
- sparse `variants` containing only fields that differ from the base.

`WidgetElementSpec` stores layout-only values: visibility, position, dimension modes, fixed
dimensions, padding, anchor, alignment, z-index, and optional corner radius. It deliberately does
not store clock text, battery values, fonts, colours, tints, opacity, tap actions, or other runtime
and user-configuration state.

Stable element identities are semantic strings rather than Android resource IDs. The first
descriptor uses:

- `clock.time`
- `clock.date`
- `clock.day`
- `clock.battery`

`clock.date` is the logical date element. The current production implementation's `clock_date` and
`clock_date_above` views remain an internal rendering detail for a later renderer migration.

## Variants and inheritance

Standard variant IDs range from `1x1` through `5x5`. A `WidgetVariantId.Custom` identity is also
available for later custom/device-size canvases. A grid ID identifies a design variant only: it does
not imply a fixed width or height in dp because launcher cell sizes vary by device and grid.

Resolution starts with every base element, then applies the selected variant's nullable field
overrides. Unmentioned elements and fields inherit unchanged. Overrides referencing an unknown
element produce a structured resolution error rather than a partial design.

The initial Digital Clock design is a conservative approximation of the current compact, medium,
and large XML geometry. Only existing gallery variants that need different geometry have sparse
overrides; the remaining standard variants inherit the base design. Phase 1 data does not influence
the production clock.

## Capabilities and registry

`ElementCapability` currently defines visibility, position, size, padding, alignment, corner
radius, and z-order metadata. Descriptors declare capabilities per element so a later editor can
hide unsupported controls.

`WidgetStudioRegistry` is generic and rejects duplicate widget and element IDs. Only Digital Clock
is registered in Phase 1. Renderer and sample-data interfaces exist as extension seams, but no
production renderer implements them yet.

## Validation and JSON

`WidgetDesignValidator` returns structured issues and checks schema version, widget ID, duplicate
elements, required elements, variant identities, finite numeric values, fixed dimensions, padding,
corner radius, z-index, and variant element references.

`WidgetDesignJsonCodec` writes human-readable JSON with stable enum names and sorted variant and
override entries. Unknown fields are ignored. Malformed input, invalid values, and unsupported
future schema versions produce explicit decode errors. Future versions are never silently treated
as schema 1.

## Storage

Developer Mode uses a separate SharedPreferences file:

```text
widget_studio_developer_mode
  developer_mode_enabled
```

Studio selection state uses:

```text
widget_studio_state
  selected_widget_id
  selected_variant_id
  active_design_revision
```

Designs are stored as app-private JSON files:

```text
files/widget_studio/designs/<widgetId>.json
```

Each successful save writes a temporary file, replaces the final file, and increments the revision.
The repository reports missing, corrupt, invalid, unsupported-schema, and I/O states without writing
anything to `widget_prefs`.

Snapshot image generation is not implemented. Metadata foundations live under:

```text
files/widget_studio/snapshots/metadata/
```

The repository can register metadata, return the latest entry for a widget and variant, detect a
missing referenced PNG, remove an entry, and delete stale or corrupt metadata.

## Phase 2 editor

`WidgetStudioEditorController` owns the stored design, mutable working design, selected standard
variant, selected element, validation state, dirty state, and a 50-operation undo/redo history.
Activities issue explicit commands for movement, visibility, padding, anchors, alignment,
dimensions, copying, and resets. A drag transaction applies live movement but commits only one
history operation when the pointer is released.

`WidgetVariantOverrideEditor` always maps an edited resolved element back against its base element.
Fields equal to the base are omitted, empty element overrides are removed, and empty variant
overrides are removed. Switching variants therefore retains independent unsaved changes without
duplicating complete designs.

The custom `WidgetStudioCanvasView` uses central `StudioPreviewMetrics` values. Standard grid IDs
produce approximate editor-only canvas dimensions from cell width, cell height, and gaps. Canvas
coordinates remain dp-based; pixels are used only for display scale and touch conversion. The
canvas supports fit/100%/incremental zoom, panning, selectable element bounds, hidden placeholders,
snapped dragging, and centre/edge guides.

Anchor changes preserve the element centre. In this editor, x/y are offsets from the selected
anchor's point on the canvas to the element centre. Changing the anchor recalculates those offsets
so the absolute centre does not jump.

The inspector reads each `StudioElementDescriptor` capability set before exposing visibility,
position, size, padding, or alignment controls. Clock elements do not expose corner radius or
z-order. Sample strings such as `12:45` and `84%` are editor-only and never enter the design model.

Unsaved working and stored designs are encoded into Activity saved state for ordinary recreation;
only an explicit Save writes through `WidgetDesignRepository`. Leaving while dirty offers Save,
Discard, and Cancel.

## Current limitations and next phase

The current editor supports only the Digital Clock and uses representative drawn content. There is
still no actual `RemoteViews` preview, developer test provider, snapshot renderer, or gallery
migration. Samsung blur roots, widget layouts, provider metadata, existing renderer flows, and
per-widget preference keys remain unchanged.

Phase 3 should begin with a shared Digital Clock `RemoteViews` renderer that can consume a resolved
design without changing provider identity or preference compatibility. Actual launcher sizing and
the Developer Test Widget should remain separate follow-up work.

## Phase 8 status

Phases 3-7 supplied the shared rendering, actual preview, developer host, snapshots,
device verification, immutable promotion, per-instance opt-in, and gallery isolation
described in their phase documents. Phase 8 adds Storage to those generic paths.

The default registry now contains Digital Clock, Battery, and Storage. Storage models
compact content as one configured primary metric and large content as three composite
metric rows. Live filesystem, memory, and traffic values remain renderer input rather
than design data. Production providers read only immutable promoted copies when an
instance is explicitly in promoted mode; existing instances with no mode key remain
legacy. Weather and the remaining widgets are still outside Widget Studio.

## Phase 9 status

The default registry now also contains Next event Calendar. Its design contains only
title/details geometry; event data and permission/provider states are runtime renderer
input. Developer render modes use deterministic non-personal sample content. Device
verification schema 2 retains schema-1 compatibility and adds a Calendar-only content
state check. Calendar Display and Weather remain outside Widget Studio.

## Phase 10 status

The registry now contains all 18 Home-screen widget families and both Samsung
lock-screen message variants. Four widgets retain purpose-built shared renderers;
the remaining families use one reusable eight-slot RemoteViews renderer with stable,
widget-namespaced element IDs. API 31+ applies exact geometry while Android 8-11 uses
a non-overlapping vertical fallback.

Every Home-screen provider preserves its legacy layout for existing instances and
uses immutable promoted geometry only for explicitly opted-in instances. Live data,
permissions, artwork, calendar values, counters, and PendingIntent actions remain
runtime inputs. Gallery previews, configuration samples, ACTUAL previews, Developer
Test Widget rendering, snapshots, verification, and promotion now resolve across the
complete Home-screen registry.

Verification schema 3 adds a Samsung System UI host check with schema-1/2 migration.
Lock-screen designs can be edited, snapshotted, and verified, but production promotion
is intentionally blocked: the lock/AOD/cover ServiceBox path has different monotone
contracts and no safe per-instance creation boundary.

# Widget Studio Phase 5

Phase 5 creates revision-aware developer PNG snapshots from the exact local
`RemoteViews` rendering path and uses them as the preferred Digital Clock gallery
preview only while Developer Mode is enabled.

## Capture architecture

`WidgetSnapshotRenderer` receives a widget ID, variant, saved design revision,
canvas dp dimensions, and density. It resolves the registered descriptor and saved
design, requires an exact revision match, resolves the variant, and invokes the
descriptor's `StudioRemoteViewsRenderer.buildForSnapshot()` hook.

On the main thread it then:

1. calls `RemoteViews.apply(context, temporaryParent)`;
2. adds the applied hierarchy to the isolated parent;
3. measures and lays it out with exact density-derived pixel dimensions;
4. draws the hierarchy into a transparent `ARGB_8888` bitmap.

It never reads or draws `WidgetStudioCanvasView`, selection outlines, grids,
diagnostics, or editor controls.

PNG compression and file/metadata commits run on a single background executor.
The PNG is written to a temporary file and moved atomically where supported. If a
same-revision snapshot already exists it is backed up until metadata registration
succeeds. A failed generation retains the previous snapshot and removes incomplete
temporary output.

## Clock snapshot mode

Digital Clock registers the first snapshot-capable adapter and continues to call
the shared `ClockRemoteViewsRenderer`. `ClockRenderMode.SNAPSHOT` uses the same
font-specific Studio layout, geometry, visibility, padding, alignment, tint, and
sizing as ACTUAL preview and the Developer Test Widget.

Only runtime sample values change. The existing `TextClock` hierarchy is retained;
snapshot mode assigns literal date/time patterns so the captured artwork is stable:

- time: `10:08`;
- date: `24 August`;
- day: `Monday`;
- battery: `72%`.

Production mode remains live, installs its normal tap actions, and continues to use
`design = null`.

## Size and memory rules

The active Studio canvas source—approximate grid, captured device, or custom—defines
the snapshot dp bounds. Pixel dimensions are rounded from `dp × current density` and
recorded alongside the dp values and density.

Allocation is rejected before bitmap creation if a dimension exceeds 8192 pixels or
estimated ARGB memory exceeds 64 MiB. Normal Studio variants and reasonable custom
sizes are not downscaled.

## Storage and revisions

Developer snapshots live under:

`files/widget_studio/snapshots/images/`

Revision-specific files use:

`<widget>__<variant>__rev_<revision>.png`

Metadata schema 2 records widget, variant, revision, dp dimensions, pixel
dimensions, density, creation time, relative path, developer scope, local
RemoteViews render mode, and renderer version. Schema 1 metadata remains readable.

Metadata is stored per revision, so an older valid file remains available after a
design save. Status compares the selected design revision with available files and
reports Current, Stale, or Missing. Cleanup removes corrupt/missing metadata,
temporary files, and unreferenced PNGs.

## Gallery resolution

The main gallery resolves every widget-size tile in this order:

1. current developer snapshot, when Developer Mode is enabled;
2. stale developer snapshot;
3. existing packaged preview layout;
4. legacy `ScaledWidgetPreview`;
5. generic icon fallback.

Digital Clock maps each gallery size to its matching Studio variant. Developer Mode
off never queries or displays developer snapshots. PNGs are decoded directly into
an `ImageView` and retained in a modest 12-entry memory cache. Snapshot creation and
deletion invalidate all cached revisions for that widget/variant.

`ScaledWidgetPreview` remains intact as the verified fallback. Configuration-screen
preview migration and other widget renderers are outside Phase 5.

## Export and blur limitation

Studio previews the saved PNG and can export it with a read-only content URI from a
scoped `FileProvider`. Delete requires confirmation and removes only the chosen PNG
and metadata, never the design.

Local snapshots contain the widget's real local fallback surface and content but
cannot contain Samsung One UI Home's launcher-only native wallpaper blur. Home-screen
provider metadata and blur-root code are unchanged.

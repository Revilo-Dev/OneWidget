# Widget Studio Phase 4

Phase 4 adds a developer-only launcher widget and an explicit Studio-to-home-screen
load workflow. It does not apply Studio geometry to production widgets and does not
add PNG snapshots.

## Runtime flow

`DeveloperWidgetProvider` is a generic host. For every placed instance it follows:

1. load `ActiveDeveloperWidgetSpec` from developer-only preferences;
2. find the widget descriptor in `WidgetStudioRegistry`;
3. load the referenced saved design from `WidgetDesignRepository`;
4. require the saved revision to match the explicitly loaded revision;
5. resolve the selected variant's sparse overrides;
6. call the descriptor's `StudioRemoteViewsRenderer` adapter;
7. update that instance with a `RemoteViews` built for its own launcher size.

Only Digital Clock registers a developer renderer in Phase 4. Its adapter calls the
shared `ClockRemoteViewsRenderer` with the safe Studio sample configuration,
`ClockRenderMode.DEVELOPER_WIDGET`, and resolved Studio geometry. It never reads a
developer instance from `widget_prefs`.

The production `DigitalClockWidget` continues to call the same shared renderer with
the real user configuration and `design = null`.

## Provider availability

The provider is disabled in the manifest. `DeveloperWidgetComponentController`
enables only that component when Developer Mode becomes active and disables only
that component when Developer Mode is turned off. Design files, active developer
state, measurements, and editor state are retained.

If a developer widget instance is present, Settings warns before disabling because
launcher behavior is host-specific. The actual Samsung One UI behavior after
disabling an installed provider has not yet been device-verified; record it in the
Phase 4 test plan when tested.

## Host-size rule

The provider reads all four legacy launcher bounds for every app-widget ID. Invalid
or absent values are replaced with positive fallbacks. On a portrait device the
preferred render size is the narrower width (`minWidth`) and taller height
(`maxHeight`); on a landscape device it is the wider width (`maxWidth`) and shorter
height (`minHeight`). The chosen width and height are persisted together with the
four bounds, app-widget ID, and measurement time.

The active variant remains an override key only. The launcher-derived preferred size
is the renderer input. Resizing never changes the variant or design. Multiple placed
instances resolve the same active variant and render independently using their own
options.

## Studio behavior

`LOAD TO TEST WIDGET` validates and saves dirty work before persisting the active
selection. It then updates every currently placed developer widget. With no placed
instance, it still saves the selection and tells the developer to add the widget.

The canvas has three editor-only sources: approximate grid, last captured device
size, and custom size. `USE DEVICE SIZE` and custom dimensions affect only the
canvas. They do not alter the selected variant, provider metadata, or
`WidgetDesignSpec`.

No active selection shows a blur-compatible instruction placeholder. Missing,
corrupt, unsupported, or revision-mismatched state shows a blur-compatible error
placeholder instead of falling back to production clock preferences.

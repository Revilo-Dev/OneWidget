# Widget Studio Phase 6 Manual Test Plan

Automated tests cover configuration mapping, preview failure handling, registry and
design resolution, Battery render planning/geometry, corner-radius gating, developer
widget selection, snapshots/gallery resolution, and existing Phase 1–5 behavior.
The following checks require an Android/Samsung device.

## Digital Clock configuration preview

1. Add or reconfigure a Digital Clock widget and confirm the configuration preview is
   a local `RemoteViews` preview.
2. Change each setting without saving: font, 12/24-hour mode, AM/PM, seconds, date,
   day, phone battery, date position, alignment, text size, text colour, tint, opacity,
   and background visibility. Confirm every visible change refreshes immediately.
3. Tap Cancel and confirm the widget and `widget_prefs` retain their previous values.
4. Reopen, make changes, tap Save, and confirm the existing widget updates and the
   Activity returns `RESULT_OK`.
5. Confirm tapping the preview does not launch Clock or Calendar.
6. Confirm the note says native One UI blur is launcher-only.
7. If a deliberately broken debug layout is available, confirm apply failure is
   diagnosed and the legacy preview appears.

## Battery configuration preview

1. Reconfigure compact and large Battery widgets.
2. Change the selected compact device, text size, colour, alignment, tint/opacity,
   gradient setting, and state colours where exposed.
3. Confirm the shared RemoteViews preview changes before Save, Cancel leaves
   production preferences untouched, and Save keeps the existing result/update flow.

## Battery Studio

1. Enable Developer Mode, open Widget Studio, tap the widget name, and select Battery.
2. Check 1x1, 2x1, 2x2, 4x2, and 5x5 variants.
3. Drag Percentage, hide/show Status, reposition/resize the icon, resize/pad/align the
   progress bar, and change its Corner Radius.
4. Confirm Corner Radius is present only for Progress.
5. Exercise undo/redo, save, leave Studio, reopen, and confirm Battery state returns.
6. Make an unsaved edit and switch to Clock. Verify Save/Discard/Cancel all behave as
   labelled and Cancel preserves the Battery edit.
7. Switch back to Battery and confirm the selected element, EDIT canvas, ACTUAL
   preview, diagnostics, snapshot status, and inspector all belong to Battery.

## ACTUAL and Developer Test Widget

1. Compare Battery EDIT and ACTUAL at the test sizes; allow only documented local
   rendering/API differences.
2. Load Battery to the existing Developer Test Widget and confirm 72% / Charging.
3. Resize it and use device-captured size in Studio.
4. Load Clock into the same placed provider, then Battery again. Do not re-add it.
5. On API 31+, verify progress radius changes. On API 26–30, verify the XML fallback
   remains safe and diagnostics identify the limitation.

## Snapshot and gallery

1. Save and snapshot Battery 2x1 and 4x2 variants.
2. Confirm PNG preview/export, exact dimensions, current/stale transitions, and delete.
3. With Developer Mode on, verify the matching Battery gallery variant uses the
   generated snapshot; verify stale and packaged/legacy fallbacks.
4. Turn Developer Mode off and confirm no developer Battery snapshot is exposed.

## Production isolation and blur

1. Place normal Battery and Digital Clock widgets before editing Studio designs.
2. Edit/save both Studio designs and confirm neither normal widget changes.
3. Confirm production Battery compact/large switching, phone/Bluetooth data,
   charging state, progress, colours, refreshes, and resize behavior remain intact.
4. Confirm production Clock behavior remains intact.
5. Confirm production and Developer Test Battery retain native One UI launcher blur;
   local configuration/ACTUAL/snapshot previews are not expected to contain it.

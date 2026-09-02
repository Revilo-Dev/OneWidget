# Widget Studio Phase 9 Manual Test Plan

Automated tests cover Calendar content states, configuration mapping, compact/large
renderer routes, Studio descriptors and geometry, schema-1 verification migration,
registry/developer resolution, verification policy, promotion review, and production
size mapping. These checks require Android hardware.

## Permission and content states

1. Remove Calendar permission, add Next event, deny the configuration request, save,
   and confirm the denial guidance appears and placement continues. Confirm the widget shows
   Permission needed rather than No upcoming events.
2. Reconfigure the widget, grant permission, and confirm a real next event appears.
3. Use an event with a blank title and confirm Untitled event is shown.
4. Use a calendar with no event in the next seven days and confirm the empty state.
5. Temporarily disable calendar-provider access if the test device permits it and
   confirm provider-unavailable or temporary-error text remains readable.
6. Revoke permission after placement and wait for/update the widget; confirm it returns
   to the permission state without crashing.
7. Confirm tapping a production widget opens a Calendar app. Local configuration,
   ACTUAL, snapshot, and Developer Test Widget previews must not launch Calendar.

## Configuration preview

1. Change tint/opacity, text colour, text scale, and alignment without saving.
2. Confirm the local RemoteViews preview updates using deterministic Team catch-up
   content and never displays a personal event.
3. Cancel and confirm preferences/production output are unchanged.
4. Save and confirm the production instance updates with real runtime content.

## Studio and Developer Test Widget

1. Select Next event in Widget Studio and inspect 1x1 through 5x1. Confirm Title is
   centered and Details is hidden.
2. Inspect 2x2, 4x2, and 5x5. Confirm Title and Details are independently selectable.
3. Move, resize, hide, pad, and align both elements. Exercise undo/redo, copy/reset,
   Save/Discard/Cancel, and recreation.
4. Compare EDIT and ACTUAL, then load the revision to the Developer Test Widget.
5. Confirm the deterministic sample is used at every developer size.
6. On API 31+, verify dynamic geometry. On API 26-30, verify the non-overlapping XML
   fallback and diagnostics.

## Snapshot, verification, and promotion

For Compact 2x1, Medium 2x2, and Large 4x2:

1. Save one revision, load it to the Developer Test Widget, and verify native blur,
   geometry, visibility, padding, alignment, and snapshot parity.
2. Exercise and verify event, empty, permission, and unavailable/error states before
   checking Calendar content states.
3. Save the verification record and confirm the exact revision is current.
4. Save another design revision and confirm earlier verification/snapshots are stale.
5. Review the mapping and diff, then promote with explicit confirmation.
6. Add/configure a new Calendar instance and confirm promoted geometry combines with
   real content and its independent appearance preferences.
7. Confirm an instance installed before promotion retains legacy geometry.

## Gallery and regression

1. With Developer Mode on, verify current/stale Calendar developer snapshots follow
   the established priority ahead of production and packaged previews.
2. With Developer Mode off, verify developer snapshots and personal calendar data are
   absent; stale production snapshots must never be shown.
3. Recheck Clock, Battery, and Storage Studio selection, production modes, snapshots,
   and rendering.
4. Confirm Calendar Display remains unchanged and outside Widget Studio.

Record phone model, Android/API, One UI, One UI Home version, permission state,
widget/variant/revision, checklist results, notes, time, and production hash for any
failure. Physical Samsung testing is required before claiming One UI parity.

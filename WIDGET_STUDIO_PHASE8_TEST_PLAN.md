# Widget Studio Phase 8 Manual Test Plan

Automated tests cover Storage descriptors, sparse variants, geometry planning,
configuration mapping, renderer routes, registry/developer resolution, verification
policy, and production size mapping. The following checks require Android hardware.

## Preparation

1. Install the Phase 8 debug APK and keep an existing production Storage instance.
2. Enable Developer Mode, open Widget Studio, select Storage, and capture device info.
3. Add the Developer Test Widget if it is not already placed.

## Configuration preview and legacy safety

1. Add or reconfigure compact and large Storage widgets.
2. Change compact metric, titles, percentage/amount detail, Hide data, text scale,
   text colour/alignment, tint/opacity, gradients, and all three metric colours.
3. Confirm the local RemoteViews preview updates before Save, Cancel changes no
   production preference, and Save updates the placed instance.
4. Confirm the large Hide data state shows Manage Storage and tapping it opens the
   system storage screen. Confirm preview and Developer Test Widget taps do nothing.
5. Before any promotion, confirm the pre-existing Storage instance is unchanged.

## Studio and Developer Test Widget

1. Inspect 1x1 through 5x1 and confirm only Compact metric is shown.
2. Inspect 2x2, 4x2, and 5x5 and confirm Storage, Memory, and Data rows are shown.
3. Move, resize, hide, pad, align, and change the radius of each row. Exercise
   undo/redo, copy/reset variant, Save/Discard/Cancel, and process recreation.
4. Compare EDIT and ACTUAL, then load the design to the Developer Test Widget.
5. Confirm deterministic 62% Storage, 58% Memory, and 34% Data values.
6. Resize across the 100 dp height boundary and confirm compact/large runtime content
   switches without changing the explicitly loaded Studio variant.
7. On API 31+, verify geometry and radius changes. On API 26-30, verify the safe XML
   fallback and diagnostics.

## Snapshot, verification, and promotion

For Compact 2x1, Medium 2x2, and Large 4x2:

1. Save one revision, load it to the Developer Test Widget, and verify native blur,
   row geometry, visibility, padding, alignment, progress rendering, and radius.
2. Capture and compare the current snapshot, then save the verification record.
3. Confirm a later design save makes both earlier evidence types stale.
4. Promote only after reviewing all mappings, compatibility warnings, and the diff.
5. Add a new Storage widget and confirm it uses promoted geometry plus its own live
   values and user configuration.
6. Confirm the pre-existing instance retains legacy built-in geometry.
7. Delete the new instance, add another, and confirm no deleted per-instance mode is
   reused.

## Gallery and isolation

1. With Developer Mode on, confirm current/stale developer Storage snapshots precede
   production and packaged previews.
2. With Developer Mode off, confirm only the current immutable production snapshot
   can precede packaged/legacy previews.
3. Activate a production revision without a matching snapshot and confirm no stale
   production image is shown.
4. Confirm Clock and Battery Studio designs, production modes, snapshots, and widget
   rendering remain unchanged.

Record phone model, Android/API, One UI, One UI Home version, widget/variant/revision,
each checklist result, notes, time, and the production hash for any failure. Physical
Samsung testing is required before claiming One UI parity.

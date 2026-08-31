# Widget Studio Phase 2 manual test plan

This plan verifies the developer-only Digital Clock editor. It does not validate production
`RemoteViews`, launcher sizing, snapshots, or Samsung blur rendering.

## Developer access

1. Open Settings and confirm Widget Studio is hidden while Developer Mode is off.
2. Enable Developer Mode with `41244110` and open Widget Studio.
3. Disable Developer Mode, attempt to launch the Studio Activity directly, and confirm it exits.

## Variants and canvas

Test `1x1`, `2x1`, `2x2`, `4x2`, and `5x5` explicitly, then briefly select every remaining
standard variant.

For each representative size:

1. Confirm the displayed approximate dp dimensions match the selected grid.
2. Confirm the canvas cell guides and boundaries update.
3. Use zoom out, zoom in, 100%, and Fit.
4. Pan a canvas that is larger than the viewport.
5. Confirm zoom and pan do not alter X/Y values.

## Element editing

Repeat across `clock.time`, `clock.date`, `clock.day`, and `clock.battery`:

1. Select the element from the Elements panel and from the canvas; confirm both stay synchronized.
2. Drag the element and verify X/Y update live in dp.
3. Enter X/Y manually and confirm the canvas updates.
4. Try Off, 1 dp, 4 dp, and 8 dp snapping.
5. Drag near the canvas centre and edges and confirm alignment guides appear.
6. Drag toward an edge and confirm the element cannot become completely inaccessible.
7. Hide and show the element.
8. Hide it, enable Show hidden elements, select its placeholder, then disable the toggle.
9. Change width and height between WRAP, MATCH, and FIXED.
10. For FIXED, enter positive dimensions and verify zero/negative input is rejected.
11. Change all four padding values independently.
12. Enable Link padding and confirm one edit changes all four sides.
13. Change all nine anchors and confirm the element preserves its visual centre.
14. Change horizontal and vertical alignment values.

Confirm that no font, text-content, colour, tint, opacity, corner-radius, or runtime-data controls
are present.

## Variant isolation

1. Select `2x1` and move `clock.time` to a recognizable X/Y position.
2. Switch to `4x2` and confirm its time element did not move to the `2x1` position.
3. Make a different `4x2` edit.
4. Return to `2x1` and confirm its unsaved edit remains.
5. Return to `4x2` and confirm its independent edit remains.

## Copy and reset

1. Select `3x2`, choose Copy layout from, and copy the resolved `4x2` layout.
2. Confirm `3x2` visually matches `4x2` while `4x2` remains unchanged.
3. Copy into a target with overrides and confirm the replacement prompt appears.
4. Edit one element, choose Reset element, and confirm only that element inherits the base.
5. Choose Reset variant, confirm the prompt, and verify all selected-variant overrides disappear.
6. Choose Reset entire widget design, confirm the stronger prompt, and verify registered defaults
   return without changing production widget settings.

## History

1. Perform a drag with several pointer-move events and press Undo once; confirm the entire drag is
   reverted in one step.
2. Exercise Undo and Redo for visibility, X/Y, padding, size modes, fixed dimensions, anchor,
   alignment, copy, reset element, and reset variant.
3. Undo an operation, make a new edit, and confirm Redo is disabled.

## Persistence and leaving

1. Make an edit and confirm the status reads Unsaved changes.
2. Press Save and confirm the status reads Saved and the revision increases.
3. Close and reopen Studio; confirm the saved design reloads.
4. Make unsaved changes and leave. Verify Save, Discard, and Cancel each behave as labelled.
5. Rotate or recreate the Activity with unsaved edits and confirm the working design, selection,
   variant, hidden-elements toggle, snapping mode, link-padding toggle, and zoom are restored.

## Production isolation

1. Add or refresh the production Digital Clock widget before and after Studio edits.
2. Confirm its appearance and behaviour do not change from Phase 1.
3. Confirm production clock preferences, gallery previews, configuration previews, provider XML,
   Samsung blur roots, and other widgets remain unchanged.

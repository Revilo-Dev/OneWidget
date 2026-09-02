# Widget Studio Phase 3 manual test plan

## Build identity

- Install the Phase 3 debug APK on a Samsung device running One UI Home.
- Record the device model, Android version, One UI version, launcher version, APK version, and test date.
- Keep at least one pre-Phase-3 Digital Clock screenshot for side-by-side comparison if available.

## Production Digital Clock regression

- Place separate small, medium, and large Digital Clock instances.
- Configure at least three different fonts, including System, a Samsung number font, and Clock Stamp.
- Verify each size/font matches the pre-Phase-3 appearance: root shape, spacing, text size, text colour, tint, and opacity.
- Verify system, forced 12-hour, and forced 24-hour formats. Repeat with seconds and AM/PM toggled.
- Verify date hidden/visible and date above/below the time.
- Verify day-of-week behaviour, including its existing large-layout restriction.
- Verify phone battery hidden/visible and confirm the displayed percentage is plausible.
- Set tap action to Clock and verify the clock/alarm app opens.
- Set tap action to Calendar and verify the calendar app opens.
- Resize through compact, medium, and large thresholds; verify the expected font-specific layout is selected after every resize.
- Keep at least two differently configured instances on the home screen and confirm updating one does not change the other.
- Reboot and confirm all instances return and render correctly.
- Change timezone and confirm the live time and subsequent provider refresh remain correct.
- Change locale if practical and verify TextClock date formatting remains live.

## Widget Studio ACTUAL preview

- Enable Developer Mode, open Widget Studio, and confirm EDIT is still the interactive canvas.
- Select 1x1, 2x1, 4x2, and 5x5 in turn; switch EDIT → ACTUAL for each and verify ACTUAL uses the selected aspect and diagnostics report the selected dp size.
- Confirm ACTUAL shows a live TextClock and not a manually reconstructed static time.
- Move the time in EDIT, release the drag, switch to or observe ACTUAL, and confirm the position changes.
- Hide and show the date; confirm ACTUAL uses real `RemoteViews` visibility.
- Hide and show the battery; confirm ACTUAL follows the design visibility while using the isolated Studio sample configuration.
- Change each padding edge and confirm ACTUAL spacing changes without doubled base padding.
- Change horizontal and vertical content alignment and confirm text moves inside its element bounds.
- Change the anchor, then x/y, and confirm ACTUAL follows the same anchor-relative position used by EDIT.
- Exercise WRAP, MATCH, and FIXED width/height. Confirm diagnostics show the resolved bounds.
- Undo and confirm ACTUAL reverts. Redo and confirm ACTUAL restores.
- Copy a variant, reset an element, and reset a variant; confirm ACTUAL refreshes after each discrete action.
- Tap ACTUAL and confirm it does not launch Clock or Calendar; diagnostics must say `tapActionsApplied=false`.
- Expand diagnostics and verify widget, variant, canvas dp dimensions, production layout, applied font layout, API level, geometry status, unsupported count, and apply success are visible.

## API compatibility

- On Android 12/API 31 or newer, confirm width, height, anchors, and x/y are applied and no geometry compatibility warnings appear for registered elements.
- If an Android 8–11/API 26–30 device or emulator is available, confirm ACTUAL does not crash. Visibility, padding, gravity, text, colour, and tint should still apply; diagnostics should report width, height, and position as ignored below API 31.

## Samsung blur contract

- On One UI Home, place the production clock over detailed wallpaper and confirm Samsung native widget blur still appears behind the translucent tint.
- Confirm rounded background/tint behaviour is unchanged for background enabled and disabled states.
- In Widget Studio ACTUAL, confirm the fallback background appears without simulated launcher blur. This difference is expected: native One UI blur is only visible on the home screen.

## Completion record

The following checks require a physical Samsung device and must remain marked **USER VERIFICATION REQUIRED** until someone records evidence:

- APK installed: USER VERIFICATION REQUIRED
- Production clock placed: USER VERIFICATION REQUIRED
- Native blur verified: USER VERIFICATION REQUIRED
- Production clock resized: USER VERIFICATION REQUIRED
- ACTUAL preview compared with the home-screen clock: USER VERIFICATION REQUIRED

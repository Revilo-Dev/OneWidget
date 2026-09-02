# Widget Studio Phase 5 Test Plan

## Automated verification

- Run `./gradlew test`.
- Run `./gradlew assembleDebug`.
- Run `./gradlew lint`; compare with the accepted 24-error / 622-warning baseline.
- Confirm `DigitalClockWidget` still passes `design = null`.
- Confirm no non-clock production renderer was migrated.

## User device verification required

Use a Samsung device with One UI Home. Record device model, Android version, One UI
version, launcher version, and display density. Local JVM tests cannot verify
`RemoteViews.apply()`, fonts, exported content-URI handling, or visual parity.

### Studio capture

- Open Digital Clock Studio and select 2x1.
- Make a visible geometry edit and save it.
- Press **SNAPSHOT**.
- Confirm the UI shows **Creating snapshot…**, then the created widget/variant/revision.
- Confirm a PNG preview appears and contains no editor grid, selection outline, or
  diagnostics.
- Compare ACTUAL preview and saved snapshot. They should differ only in fixed sample
  values and the absence of launcher-native blur.

### Revisions

- Create a snapshot at revision 5 and confirm status is Current.
- Edit and save revision 6 without creating another snapshot.
- Confirm status reports the revision-5 snapshot as out of date.
- Create a new snapshot and confirm revision 6 becomes Current.
- Confirm the revision-5 PNG remains in private storage until cleanup or explicit
  deletion policy removes it.

### Sizes

For each size, create a snapshot and confirm preview proportions plus decoded pixel
dimensions match `dp × density`:

- 1x1 approximate grid;
- 2x1 approximate grid;
- 4x2 approximate grid;
- 5x5 approximate grid;
- last device-captured size;
- a reasonable custom size larger than the standard grid.

Confirm switching canvas source does not change the selected variant.

### Content and geometry

- Confirm fixed time is `10:08`.
- Confirm visible date, day, and battery show `24 August`, `Monday`, and `72%`.
- Test hidden date, hidden day, and hidden battery.
- Test each supported font, especially Samsung variable fonts and Clock Stamp.
- Test start/center/end alignment, padding, fixed dimensions, anchors, and positive
  and negative offsets.
- Confirm transparent surroundings remain transparent where the widget surface does
  not draw content.

### Gallery

- Enable Developer Mode and open the Clock gallery.
- Confirm only the matching widget/variant tile uses each generated PNG.
- Confirm a current snapshot is preferred.
- Save a newer design and confirm the stale snapshot remains the fallback.
- Delete the visible snapshot repeatedly and confirm the next stale snapshot or
  packaged preview appears safely.
- Confirm scrolling and expanding/collapsing categories remain smooth and images do
  not repeatedly flash or decode.
- Disable Developer Mode and confirm the normal packaged/legacy production preview
  is shown instead of experimental Studio artwork.

### Export and deletion

- Export a current snapshot and open it in at least one external image app.
- Confirm sharing uses a temporary read grant and never exposes a `file://` URI.
- Delete a snapshot, confirm the dialog names widget/variant/revision, and confirm the
  PNG plus matching metadata are removed.
- Confirm the Widget Studio design remains untouched.

### Failure and memory safety

- Enter an extreme custom canvas size that exceeds the pixel or 64 MiB allocation
  limit and press Snapshot.
- Confirm generation fails with a concise safety message and the previous PNG/status
  remains available.
- Interrupt or background the app during PNG encoding and confirm no incomplete
  snapshot is registered.

### Home-screen regression

- Refresh a production Digital Clock and confirm it still uses user configuration
  plus `design = null`.
- Refresh the Developer Test Widget and confirm LOAD TO TEST WIDGET, resize feedback,
  and per-instance sizing remain unchanged.
- Verify native blur on the actual One UI home widget. The exported/local PNG is not
  expected to contain launcher-native blur.

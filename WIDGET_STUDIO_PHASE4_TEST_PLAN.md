# Widget Studio Phase 4 Test Plan

## Automated verification

- Run `./gradlew test`.
- Run `./gradlew assembleDebug`.
- Run `./gradlew lint` and compare the result with the accepted baseline of exactly
  24 errors and 622 warnings.
- Confirm `DigitalClockWidget` still passes `design = null` to
  `ClockRemoteViewsRenderer`.
- Confirm no production provider is enabled, disabled, or migrated by the Phase 4
  component flow.

## User device verification required

Use a Samsung device with One UI Home. Record the device model, One UI version,
launcher version, Android version, and the observed values where noted. Do not mark
native blur, picker visibility, resize reporting, launcher persistence, or
multi-instance behavior verified based only on automated tests.

### Developer Mode and provider availability

- With Developer Mode disabled, open the One UI widget picker and confirm Developer
  Test Widget is absent.
- Enter the developer code, enable Developer Mode, reopen or refresh the picker, and
  confirm Developer Test Widget appears.
- Add a Developer Test Widget, then try disabling Developer Mode. Confirm the warning
  offers **Cancel** and **Disable anyway**.
- Choose Cancel and confirm Developer Mode remains enabled.
- Choose Disable anyway and record whether One UI removes, freezes, or retains the
  existing instance: **Not yet device-tested**.
- Re-enable Developer Mode and confirm Widget Studio and the provider become
  available again. Record whether the widget picker required a launcher refresh.

### Add widget and empty state

- Add Developer Test Widget to One UI Home before loading a Studio design.
- Confirm it shows the developer instruction placeholder and does not crash.
- Confirm the placeholder root participates in native Samsung blur.

### Load and reload

- Open Widget Studio, select Digital Clock and variant 2x1, then press **LOAD TO TEST
  WIDGET**.
- Return home and confirm the placed instance updates without rebuilding,
  reinstalling, or re-adding it.
- Move the time element, press Load again, and confirm the same instance changes.
- Hide the date element, press Load again, and confirm the home widget hides it.
- Save another edit without loading, resize the widget, and confirm the explicit
  revision-mismatch error asks for another Studio load rather than silently using
  an un-loaded revision.

### Resize and device-size feedback

- Resize the test widget horizontally and vertically through several One UI grid
  sizes.
- Confirm the content re-renders after every resize and remains responsive.
- Return to Studio and confirm **DEVICE TEST SIZE** shows the preferred width and
  height, min/max bounds, and the correct app-widget ID.
- Compare Studio's displayed values with the provider's `DeveloperWidgetDebug` log.
- Record the actual One UI option values and confirm the portrait rule uses
  `minWidth × maxHeight` (or `maxWidth × minHeight` in landscape).

### Use Device Size and custom size

- Keep a nontrivial variant selected, such as 4x2, and note it.
- Press **USE DEVICE SIZE** and confirm the editor canvas matches the last device
  measurement while the variant remains 4x2.
- Switch to **Custom size**, enter a size larger than the 5x5 approximation, and
  confirm pan/zoom remains usable.
- Confirm zero, negative, NaN, and infinite values cannot be accepted.
- Switch back to approximate grid size and confirm it follows the selected variant.

### Multiple instances

- Add two Developer Test Widget instances.
- Resize them to visibly different dimensions.
- Load one Digital Clock design from Studio.
- Confirm both instances update immediately.
- Confirm both use the same selected variant geometry but render at their individual
  launcher dimensions.
- Return to Studio and confirm the most recently resized instance is shown as the
  last device measurement.

### Restart and restore

- Force-stop or kill the app process and confirm the launcher widget remains.
- Restart One UI Home and confirm the loaded developer render is restored.
- Reboot the device and confirm the provider restores the persisted active design.
- Install an updated debug APK over the existing app and confirm the provider
  restores after package replacement.

### Samsung blur

- Test the placeholder, error placeholder, and loaded Digital Clock against light,
  dark, and detailed wallpapers.
- Confirm each uses One UI native blur and that no extra radius is applied to the
  protected blur root.

### Production regression

- Add or refresh a normal Digital Clock.
- Make an obvious Studio-only geometry change and load it into the Developer Test
  Widget.
- Confirm the production clock ignores that geometry.
- Change production clock font, time format, seconds, AM/PM, and tint settings and
  confirm they still work.
- Spot-check Battery, Weather, Storage, and Calendar widgets for unchanged behavior.

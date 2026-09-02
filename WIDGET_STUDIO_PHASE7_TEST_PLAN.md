# Widget Studio Phase 7 Device Test Plan

Automated tests validate storage, policy, mapping, promotion gates, history/revert, instance modes, gallery priority, and diff behavior. They cannot establish Samsung One UI visual parity.

## Preparation

1. Install the Phase 7 debug APK on a Samsung phone.
2. Enable Developer Mode and open Widget Studio.
3. Record the phone model, Android version, One UI version, and One UI Home version with **CAPTURE DEVICE INFO**. Enter One UI manually if it is unavailable.
4. Keep at least one Clock and one Battery widget installed from before promotion to verify legacy-instance safety.

## Parity — Digital Clock

For each production mapping (Compact 2x1, Medium 2x2, Large 4x2):

1. Select Digital Clock and the mapped Studio variant.
2. Compare EDIT and ACTUAL RemoteViews preview.
3. Save, then use **LOAD TO TEST WIDGET**.
4. On One UI Home check native blur, element positions, visibility, padding, alignment, and resizing.
5. Capture the current snapshot and compare it with the launcher widget.
6. Complete only the checks actually observed, add compatibility notes, and save verification.
7. Confirm a later Studio save makes the earlier verification show as stale.

## Parity — Battery

Repeat the Clock procedure for Battery and additionally verify:

1. percentage, status, and icon use real runtime state in the production provider;
2. progress value and geometry render correctly;
3. progress corner radius renders on API 31+;
4. the documented XML fallback remains safe on older supported Android versions.

Do not mark the revision verified if any launcher result has not been visually checked. Use the failed flag and notes when parity is wrong.

## Promotion — Clock

1. Verify and snapshot all three production-mapped Clock variants for the same saved revision.
2. Press **PROMOTE TO PRODUCTION**.
3. Review widget, revision, verification, 25 Studio variants, the three production mappings, snapshot state, compatibility warnings, and property diff.
4. Cancel once and confirm no production status changes.
5. Promote with explicit confirmation.
6. Confirm production revision/hash appear and **Use promoted designs for new widgets** is ON.
7. Add a new production Clock and complete its configuration.
8. Confirm promoted geometry plus the selected user font, colour, time format, and other Clock preferences.
9. Confirm the pre-existing Clock retains legacy built-in geometry.

## Promotion — Battery

1. Verify and snapshot all mapped Battery variants for one saved revision.
2. Promote after reviewing progress/radius compatibility.
3. Add and configure a new Battery widget.
4. Confirm promoted geometry, actual battery/runtime data, colors, native blur, progress, and radius.
5. Confirm the pre-existing Battery retains legacy built-in geometry.

## Override warnings

1. Try promotion with incomplete device verification and cancel the explicit warning confirmation.
2. Try again and use **Promote despite warnings**; confirm production metadata is marked partial/not tested rather than verified.
3. Repeat with no current snapshot and with only a stale snapshot.
4. Confirm no stale snapshot is copied or shown as a production snapshot.
5. Create an invalid design value and confirm validation blocks promotion even when warning overrides would otherwise be allowed.

## Revert and history

1. Promote revision A, then save/verify/promote revision B.
2. Open **REVERT PRODUCTION DESIGN** and activate revision A.
3. Confirm production status points to A and a new promoted-mode render uses A.
4. Select **Legacy built-in design**.
5. Add a new Clock and confirm built-in geometry is used.
6. Confirm revision A and B remain listed in history.

## Gallery isolation

1. With Developer Mode ON, confirm the current developer snapshot is shown ahead of production.
2. Make the developer snapshot stale and confirm it remains the developer-mode fallback ahead of production.
3. Turn Developer Mode OFF and reopen the gallery.
4. Confirm the immutable production snapshot is shown.
5. Modify or replace a developer snapshot, turn Developer Mode OFF again, and confirm it never appears in normal UI.
6. Activate another production revision without a matching production snapshot and confirm packaged/legacy fallback is used rather than a stale production PNG.

## Required result record

Record phone model, Android/API, One UI, One UI Home version, widget/variant/revision, each checklist result, notes, and test time. Report failures with screenshots and the production revision/hash. Physical Samsung testing is required before claiming One UI parity.

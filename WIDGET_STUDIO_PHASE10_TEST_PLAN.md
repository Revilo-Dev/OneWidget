# Widget Studio Phase 10 Manual Test Plan

Automated tests cover all 20 descriptors, namespaced element identities, 25 standard
variants, compact visibility and bounds, size mappings, inert developer modes,
verification migration/policy, and lock-screen promotion blocking. The following
checks require Samsung hardware.

## Registry and editor

1. Open Widget Studio and select every descriptor once. Confirm each has a renderer,
   selectable elements, all 1×1 through 5×5 variants, and no crash.
2. For every element, exercise supported visibility, movement, sizing, padding,
   alignment, and radius controls; verify undo/redo and Save/Discard/Cancel.
3. Compare EDIT and ACTUAL, then load every family into the Developer Test Widget.
4. On API 31+, verify exact geometry. On API 26-30, confirm the vertical fallback is
   readable and diagnostics identify the limitation.

## Home-screen production matrix

For each of the 18 Home-screen families, verify Compact, Medium, and Large mappings
that the provider supports:

1. Keep an existing widget in legacy mode and confirm its original layout is unchanged.
2. Save, snapshot, verify, and promote a Studio revision.
3. Enable promoted defaults, add a new instance, and confirm it receives promoted
   geometry while the existing instance remains legacy.
4. Resize across mapped categories and confirm the correct design variant resolves.
5. Reconfigure and delete the new instance; confirm settings remain instance-local and
   the production-mode key is cleaned up.
6. Disable Developer Mode and confirm developer snapshots/data never leak into gallery
   or production previews.

## Runtime states and actions

1. Weather: test location granted, denied, loading, cached data, and fetch failure.
2. Next event: test event, empty, denied/revoked permission, unavailable provider, and
   query error. Calendar display must render the correct current three-week range.
3. Step counter: grant and deny activity recognition; verify denial can still finish
   placement and displays Permission needed.
4. Alarm: test unset, on, off, snoozed, toggle, snooze, and Clock launch.
5. Hydration and Counter: test plus/minus, lower bound, daily reset text/time, resize,
   and scheduled reset.
6. Toggles: test all nine kinds, on/off indicator, square/wide mapping, and destination.
7. Media: test listener unavailable, idle, playing, paused, artwork/no artwork,
   previous/play/next, progress/time, and session launch.
8. Google, Samsung Browser, and YouTube searches: verify each opens its own target.
9. Open app and Text: verify configured icon/label/text and root action survive promoted
   rendering. Glass must retain the One UI native blur root contract.
10. Confirm all Studio, configuration-sample, snapshot, and Developer Test Widget
    previews are inert.

## Lock screen, AOD, and cover

1. Edit and snapshot both message descriptors and verify Developer Test Widget output.
2. On a supported Samsung device, manually compare the actual 1×1 and 2×1 monotone
   ServiceBox output on lock screen, AOD, and cover host where available.
3. Record the dedicated System UI host check. Native Home blur is not required here.
4. Confirm production promotion is blocked with the dedicated-host explanation.

Record phone model, Android/API, One UI, One UI Home/System UI version, widget ID,
variant, revision, permissions, runtime state, checklist results, notes, time, and
production hash for every failure.

## Latest host verification

On 2 September 2026, the host-side Phase 3–10 gate completed with 203 unit tests,
zero failures, a successful debug APK build, and Android lint at zero errors. The
physical matrix above remains open: the configured Android SDK reported no connected
device and no emulator, so lock screen, AOD, cover screen, permission dialogs,
launcher resizing, and promoted layouts were not marked as manually verified.

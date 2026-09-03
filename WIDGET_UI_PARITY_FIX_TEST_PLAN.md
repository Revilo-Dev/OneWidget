# Widget UI parity correction test plan

## Automated checks

1. Run `./gradlew testDebugUnitTest` to verify widget size policy mapping and existing renderer tests.
2. Run `./gradlew assembleDebug` to compile RemoteViews, provider XML, and manifest metadata.
3. Run `./gradlew lintDebug`; compare results with the existing baseline and investigate only newly introduced findings.

## Device verification (Samsung One UI Home)

1. Add each gallery item and confirm its offered starting size: only Toggle has 1x1; Search begins 4x1; Media begins 2x1.
2. Resize a newly added 2x1 item down and back up where the launcher exposes that operation. Confirm its remote layout changes at every host callback and no size becomes stuck after a previous larger placement.
3. Confirm Toggle 1x1 is a centred circle with fully transparent corners; resize it wide and verify the pill version remains usable.
4. Start media playback, wait through several progress/time refreshes, then change tracks. Verify no black flash on refresh and a single crossfade only on track changes.
5. Inspect Media 3x2 and 4x2 on dark and light wallpapers for curved corners with no black halo or mismatched edge line.
6. Open Gallery, Studio Actual preview, and configuration previews in light and dark themes; ensure transparent widget content remains readable while saved widget colors remain unchanged.
7. Confirm system/configuration back cancels without creating or changing a widget, while explicit save applies the result.

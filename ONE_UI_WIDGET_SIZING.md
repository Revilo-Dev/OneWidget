# One UI widget sizing

## Keep the Samsung renderer enabled

Exact-size providers must retain `app:widgetStyle="colorful"` and a non-empty
`app:widgetSize` declaration. Removing the Samsung size declaration disables
the native background path that supplies wallpaper blur and launcher styling.

`widgetSize` is a set of supported Samsung layout classes, not the initial
column/row count. Every variant in a family declares the classes needed to
resize between its supported forms. For example, Glass includes `small`,
`medium`, `large`, `extraLarge` and `extraLargeLong`. Declaring only `wideSmall`
for Glass 2x1 caused One UI to select its wide preset instead.

Initial placement uses the variant's Android target cells when the launcher's
size table and the declared Samsung classes permit them. Minimum dimensions
must fit those target cells; the previous 162x76 dp minimum blocked 2x1 on the
tested launcher's 80x74 dp minimum-cell calculation. These providers use the
70*cells-30 dp estimate and family resize bounds.

The pin callback records the user's selection, but does not overwrite
`appWidgetMinWidth` or `appWidgetMinHeight`. Only the launcher can report the
actual geometry. Writing desired values into those fields does not resize it.

## Preview choices and launcher limits

`app:previewSize` selects the preview class separately from the supported
resize classes. This avoids advertising every resize class for every alias.
All variants remain registered Android widget providers with their own target
cells, and the in-app gallery requests the exact provider for its selection.

Samsung's system picker derives its dimensions from its own preset table,
including compensation for the current grid. It does not use arbitrary target
cells for its preset labels. The inspected launcher also filters `extraLarge`
out of its preview list. Consequently, a native picker card does not establish
that an in-app target was placed correctly, and not every in-app size is shown
as an identical card in Samsung's picker. Do not replace native One UI widgets
with ordinary widgets to work around this limitation.

Home Up/custom grids can change the mapping between Samsung layout classes and
grid cells. Android resize bounds are dp values; they cannot guarantee exclusive
snapping to a list of discrete cell pairs on every launcher.

## Verification required

Verify the live widget's `semAppWidgetColumnSpan`, `semAppWidgetRowSpan` and
`appWidgetSizes`, then resize it and check again. Also check that the One UI
host retains `isStandardized: true` and enables its background at nonzero,
nonopaque opacity. Registration/manifest checks alone do not establish this.

The focused Glass 2x1 phone check recorded a 2x1 span with `semWidgetSize=2`,
`isStandardized: true`, background opacity 102 and native background visibility
enabled. Full-build placement and resizing checks are still pending.

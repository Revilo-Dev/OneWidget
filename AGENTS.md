# OneWidget agent rules

## Working in this repository

- OneWidget is an Android app by Revilo Dev with application id `dev.revilodev.onewidget`
- preserve existing user changes and inspect the working tree before editing
- keep shared widget appearance in the common surface and appearance components
- use the same configuration screen for appearance and retain each widget's complication controls
- keep production data separate from Studio geometry and sample preview data
- retain Samsung `widgetStyle="colorful"`, supported `widgetSize` classes and the outer `@android:id/background` surface for native wallpaper blur
- read actual launcher bounds when rendering and never overwrite launcher size options to simulate resizing
- Glass supports every combination of two through seven columns and one through seven rows
- use 24 dp corners for larger surfaces and rounded ends for single row surfaces
- borders default to white at 20 percent opacity and 4 physical pixels with the stroke inset inside the rounded bounds
- use bounded image decoding and caches and avoid sending oversized bitmaps through RemoteViews
- refresh only active widgets and prefer system events and non waking scheduled work
- do not use frequent wakeup alarms for clocks or progress animations
- preserve original project and dependency credits and never expose local credentials
- run the relevant unit tests and Android build and lint checks before delivery
- report device verification separately from build and unit test results
- update README when user visible behavior or development requirements change

## Commenting guidelines

- write single line comments only
- use lowercase words without punctuation or emojis
- explain the human readable reason when it helps the next reader
- avoid over explanation and comments that repeat the code
- remove redundant or useless comments from code you change
- apply these rules to Kotlin Gradle and XML comments

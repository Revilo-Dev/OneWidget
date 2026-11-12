# Changelog

## [1.0.0] - 2026-10-15

- Added all Glass grid configurations from 2x1 to 7x7 with a custom size picker.
- Expanded supported resize dimensions and read live launcher bounds consistently.
- Unified home screen widget appearance and configuration.
- Added white rounded borders with default 4 px thickness and 20% opacity, configurable globally or per widget.
- Added editable named appearance presets with colour, opacity and border settings.
- Added private custom background images for every home screen widget.
- Switched live clock rendering to font-specific TextClock layouts.
- Replaced frequent battery and storage polling with shared non-waking refreshes and system events.
- Shared and throttled weather requests and marked stale forecasts.
- Fixed to-do item actions, collection updates and daily reset initialization.
- Fixed compact clock date placement and shared text controls for older widgets.
- Updated app identity, About links, credits and the home page title behavior.
- Added agent rules, regression coverage and Android surface rendering tests.

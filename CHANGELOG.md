# Changelog

## 1.5.0-dev — Unreleased

### Added
- Shared builds for active Minecraft 26.2 and 26.3 targets, with separate output folders.
- Automatic selection of affected Minecraft targets in CI and manual selection of either target or both.
- Regression tests for harvesting limits, world/session changes, mode changes, and remembered replants.
- Explicit, disabled-by-default Fill Empty Farm Plots setting.
- Plain-language mode names in the settings screen.

### Changed
- Updated to Gradle 9.9.1, Loom 1.19.4, Fabric Loader 0.20.1, and current stable target-specific dependencies.
- Draft releases build exact tagged source commits, validate release versions, and include checksums and source provenance.
- Active release builds cover 26.2 and 26.3; historical version branches remain available separately.

### Fixed
- Out-of-range timing/batch values and unknown modes loaded from configuration files.
- Automatic harvest batch limits in 26.x.
- Pending work leaking across world/player changes and disconnects.
- Existing remembered retries continuing after the option was disabled.
- Settings-screen mode changes failing to clear previous automation state.
- Remembered-replant mode unexpectedly filling unrelated empty farm plots by default.

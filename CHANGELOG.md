# Changelog

## 1.5.0-dev — Unreleased

### Added
- Shared builds for active Minecraft 26.1, 26.2, 26.3, and 1.21.11 targets, with separate output folders.
- Automatic selection of affected Minecraft targets in CI and comma-separated manual version selection.
- Regression tests for harvesting limits, world/session changes, mode changes, and remembered replants.
- Explicit, disabled-by-default Fill Empty Farm Plots setting.
- Plain-language mode names in the settings screen.

### Changed
- Updated to Gradle 9.9.1, Loom 1.19.4, Fabric Loader 0.20.1, and current stable target-specific dependencies.
- Draft releases build fixed source commits, use the requested release version, and include checksums, source provenance, and every commit since the previous published release.
- Active release builds cover 26.1, 26.2, 26.3, and 1.21.11; historical version branches remain available separately.

### Fixed
- Out-of-range timing/batch values and unknown modes loaded from configuration files.
- Automatic harvest batch limits in 26.x.
- Pending work leaking across world/player changes and disconnects.
- Existing remembered retries continuing after the option was disabled.
- Settings-screen mode changes failing to clear previous automation state.
- Remembered-replant mode unexpectedly filling unrelated empty farm plots by default.

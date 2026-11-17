# Changelog

## 1.5.0-dev — Unreleased

### Added
- A missing-keybind notice in the config menu, with a hover hint explaining where to assign Cycle Harvest Mode.
- Exactly two automatic harvest presets: Max and the slower Server (safe).
- A Manual tuning switch that reveals a separate manual settings section and disables the preset selector.
- Staged menu edits: cancelling does not affect live farming settings.
- Modernized builds on dedicated Minecraft 26.1, 26.2, 26.3, and 1.21.11 branches, with separate output folders.
- Automatic selection of affected Minecraft targets in CI and comma-separated manual version selection.
- Regression tests for harvesting limits, world/session changes, mode changes, and remembered replants.
- Explicit, disabled-by-default Fill Empty Farm Plots setting.
- Plain-language mode names in the settings screen.

### Changed
- Main now contains only the latest Minecraft target. Active version branches carry their own tested implementation, and automatic/manual builds and draft releases compile each selected version from its own pinned branch commit.
- Release notes include version-branch commits and source links in the release description. Release attachments contain only playable mod jars and SHA256SUMS.txt; draft updates remove old notes and source metadata attachments.
- New configurations and menu reset defaults use Max with Manual tuning off. Existing saved choices are preserved.
- Faster Server (safe) preset: 10-tick harvest cooldown, 3-tick replant delay, and 20-tick missed-spot retries; still one crop per batch. Existing untuned Server settings migrate automatically.
- Updated to Gradle 9.9.1, Loom 1.19.4, Fabric Loader 0.20.1, and current stable target-specific dependencies.
- Draft releases build fixed version-branch source commits, use the requested release version, and include checksums, source provenance, and every commit since the previous published release.
- Active release builds cover 26.1, 26.2, 26.3, and 1.21.11; historical version branches remain available separately.

### Fixed
- Lost replants during rapid manual breaking, including breaks made while automatic harvesting is enabled or before the first client tick.
- Manual replanting now retries remembered spots after missing seeds, moving out of reach, delayed breaks, and temporary block obstructions.
- Recent predicted placements are checked for 20 ticks so server rollbacks can return the exact position and seed to recovery.
- Out-of-range timing/batch values and unknown modes loaded from configuration files.
- Automatic harvest batch limits in 26.x.
- Pending work leaking across world/player changes and disconnects.
- Existing remembered retries continuing after the option was disabled.
- Settings-screen mode changes failing to clear previous automation state.
- Remembered-replant mode unexpectedly filling unrelated empty farm plots by default.

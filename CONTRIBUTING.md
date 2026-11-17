# Contributing to AutoCrop

## Active targets

`main` builds Minecraft **26.2 and 26.3** from shared sources. Historical version branches are retained; this workflow does not modify or release those older implementations.

- `src/main/java`: shared gameplay, configuration, and ModMenu integration.
- `src/minecraft26.2/java` and `src/minecraft26.3/java`: small adapters for Minecraft API differences. Keep gameplay behavior shared.
- `versions/<target>.properties`: target-specific Minecraft and dependency pins.
- `src/test/java`: JUnit regression tests. Mockito replaces client/world/inventory boundaries; game crop states are real.
- `.github/scripts`: smart-build selection and Python standard-library tests.

## Development

Use JDK 25 and a short-lived branch from `main`. Follow Conventional Commits (`fix:`, `feat:`, `build:`, `ci:`, `docs:`).

```bash
./gradlew build -Pminecraft_target=26.2 --warning-mode fail
./gradlew build -Pminecraft_target=26.3 --warning-mode fail
python3 -m unittest discover -s .github/scripts -p 'test_*.py'
```

If actionlint is installed, run `actionlint` after workflow edits. Both Minecraft builds must pass for shared changes. Add a failing regression test before fixing behavior.

The Gradle wrapper distribution has a pinned SHA-256 checksum. Dependency versions are explicit; do not use dynamic `+` version selectors. Update each target's dependency pins using publisher metadata and verify both builds before landing the change.

## Runtime checks

Automated tests cannot prove live server acceptance or UI rendering. Before a public release, check both targets in-game:

1. Open settings, change modes, save/cancel, and restore defaults.
2. Manually harvest and replant supported crops.
3. Set automatic batch size to 1 and verify only one mature crop is harvested per batch.
4. Exhaust seeds, replenish them, and verify remembered retries; then disable remembered retries and verify they stop.
5. Leave intentional gaps in a mixed farm: they should remain empty by default. Enable Fill Empty Farm Plots separately to test inference.
6. Disconnect, switch dimensions/worlds, and respawn while replants are pending. Verify no old actions carry over.
7. Test hotbar refill with empty and full hotbars, plus inventory/chest/settings screens.
8. Verify behavior under latency and server protection rules on a server you control.

Record Minecraft version, dependency versions, settings, and results in the PR.

## CI and releases

See README for smart selection, manual version selection, artifacts, and immutable draft releases. Use **Build result** as the required CI check. Drafts are reviewed and published manually.

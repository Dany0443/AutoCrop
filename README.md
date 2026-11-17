# AutoCrop

A client-side Fabric mod that replants mature crops you break and optionally harvests nearby crops automatically.

Active support covers **Minecraft 26.1, 26.2, 26.3, and 1.21.11**. All four jars are built from the same gameplay sources. All other Minecraft versions are legacy and remain on their historical branches.

## Install and start farming

1. Choose the AutoCrop jar matching your Minecraft version from GitHub Releases. GitHub Actions also provides development jars under each successful build's **Artifacts** section.
2. Install Fabric Loader **0.20.1 or newer**, Fabric API, Cloth Config, and ModMenu for that same Minecraft version. Use **Java 25**.
3. Put the mod jars in your instance's `mods` folder and launch Minecraft.
4. Open **Mods → AutoCrop → Configure**, choose a **Harvest mode**, then save.
5. Optionally assign **Cycle Harvest Mode** under **Options → Controls → Key Binds → AutoCrop**. It starts unbound to avoid key conflicts. The AutoCrop config menu shows a notice until you assign it; hover over the notice for directions.

**Max is the default preset, with Manual tuning off.** Keep the appropriate seeds, carrots, or potatoes in your hotbar or main inventory. AutoCrop harvests and replants fully grown wheat, carrots, potatoes, and beetroot nearby.

## Harvest mode and Manual tuning

**Harvest mode has exactly two choices:**

| Mode | What it does |
|---|---|
| **Max** | Fastest available automatic timing and largest harvest batches. |
| **Server (safe)** | Automatic harvesting in single-crop batches, with a 10-tick harvest cooldown and a 3-tick replant delay. |

Both modes automatically harvest and replant. Server (safe) is the conservative preset; it is not a guarantee that a server allows the mod or its automation. Neither preset plants unrelated empty plots.

The main menu also has an **Enable AutoCrop** switch, **Refill seeds for me**, and **Manual tuning**. Turn **Manual tuning** on to reveal the **Manual settings** menu. The preset selector is disabled while manual tuning is active.

Manual settings let you choose automatic harvesting or replant-only behavior, then adjust replant delay, harvest interval, batch size, missed-replant recovery, retry timing, and empty-plot planting. Turn Manual tuning off to return to the selected Max or Server preset.

Missed-replant recovery works in both automatic and manual replanting modes. It remembers the exact broken position and its seed while you collect seeds, move back into reach, or remove a temporary obstruction. Recent placements are watched for 20 ticks (normally one second) to catch server rollbacks. Records are limited to 2,048 missed spots and cleared when you disable the mod, change behavior, disconnect, or change worlds.

Changes apply only when you save; cancelling leaves live settings unchanged. Existing configurations that do not match a preset automatically open with Manual tuning enabled, preserving their values. Turning AutoCrop off remembers the previous behavior and tuning for when you enable it again.

The mode-cycle keybind still cycles Off → replant-only → automatic. Replant-only behavior replants crops you break yourself, including retries when Remember missed replants is enabled.

**Auto-refill seeds** can swap seeds from your main inventory into the hotbar. It prefers an empty slot; if the hotbar is full, it swaps with the selected slot and moves that item into your inventory.

**Remember missed replants** retries failed replants in both manual and automatic modes. Turning it off clears remembered positions. Queues are also cleared when changing modes, worlds, player sessions, or disconnecting.

**Fill empty farm plots** is a separate option, **off by default**. With automatic mode and remembered replants enabled, it also plants unrelated empty farmland based on neighboring crops, or plants nether wart on empty soul sand. It can fill deliberately empty plots and change mixed-crop layouts.

Timing controls use game ticks. Increase replant delay if the server has not processed a crop break before planting is attempted. These settings do not guarantee acceptance by server anti-cheat or protection systems.

## Build a supported Minecraft version

The Gradle wrapper downloads Gradle **9.9.1**. Install a JDK 25, then run:

```bash
./gradlew build                         # Minecraft 26.3 (default)
./gradlew build -Pminecraft_target=26.2
./gradlew build -Pminecraft_target=26.1
./gradlew build -Pminecraft_target=1.21.11
```

Outputs are isolated by target:

```text
build/26.1/libs/autocrop-<version>-mc26.1.jar
build/26.2/libs/autocrop-<version>-mc26.2.jar
build/26.3/libs/autocrop-<version>-mc26.3.jar
build/1.21.11/libs/autocrop-<version>-mc1.21.11.jar
```

Use the jar without `-sources` or `-dev` for playing. Target-specific dependencies live in `versions/<target>.properties`; shared tooling versions live in `gradle.properties`. Minecraft 1.21.11 uses Loom remapping for a distributable jar.

## Smart GitHub Actions builds

Pushes and pull requests automatically select targets based on changed paths:

| Change | Targets compiled |
|---|---|
| Shared gameplay, resources, tests, build tools, or workflows | All four supported targets |
| `versions/26.1.properties` or `src/minecraft26.1/` | 26.1 |
| `versions/26.2.properties` or `src/minecraft26.2/` | 26.2 |
| `versions/26.3.properties` or `src/minecraft26.3/` | 26.3 |
| `versions/1.21.11.properties` or `src/minecraft1.21.11/` | 1.21.11 |
| Documentation only | Compilation skipped |

Multiple target-specific changes are combined. Unknown paths and unavailable comparison history conservatively build all supported targets. New branches also build all.

To choose manually, open **Actions → build → Run workflow**, select the branch containing your changes, and enter a comma-separated version list such as **`26.3,26.2`**, **`26.1,1.21.11`**, or **`all`**. Spaces and duplicate versions are handled automatically. Unsupported/legacy versions produce an error listing the supported targets. Each selected target gets its own downloadable jar artifact. All builds compile the selected revision's shared source; they do not fetch separate version-branch implementations.

The stable **Build result** job can be used as a required status check, including documentation-only pull requests.

## Draft releases

Open **Actions → release-draft → Run workflow**:

1. Select the branch containing your changes.
2. Enter a release tag such as `v1.5.0`, or leave it blank to use `mod_version` from `gradle.properties`.
3. Enter the Minecraft versions as comma-separated text, for example `26.3,26.2,26.1,1.21.11`, or use `all`.
4. Optionally enter a previous-release tag to override the changelog baseline.

The workflow builds and tests every selected target, then creates an unpublished draft with the jars, checksums, source commit, and full release notes. A new tag uses the selected workflow revision's exact commit. An existing tag always uses its tagged commit. The release tag supplies the version used in jar names and metadata, so a manual release does not require editing `mod_version` first.

Notes include **every commit since the previous published release**, including commits without a pull request. The baseline is the most recently published release reachable from the source commit; drafts and unrelated histories are skipped. If there is no prior release, the entire commit history is included. Every commit has a direct link, and notes include a full comparison link.

Rerunning an existing draft refreshes notes and assets, removing obsolete AutoCrop jars if you change the selected versions. Published releases cannot be overwritten. Review and publish drafts manually.

Pushing a version tag also runs this workflow for all four supported targets. Existing tags must contain this shared build setup to support the selected targets.

## Troubleshooting

- **Nothing happens:** Check the mode, crop maturity, seed availability, and distance. The mode key is unbound until you assign it.
- **A manual replant fails:** Keep seeds available, stay close, and try increasing replant delay. Manual mode does not retain failed replants.
- **Your held item moved:** A full hotbar caused auto-refill to swap with your selected slot. Keep a slot empty or disable auto-refill.
- **Unexpected planting on empty plots:** Turn off Fill Empty Farm Plots.
- **A mod fails to load:** Confirm all dependency jars match the Minecraft target and meet the minimum versions in the AutoCrop jar.

Report bugs with Minecraft and AutoCrop versions, mode/settings, singleplayer or server context, reproduction steps, and relevant logs.

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).

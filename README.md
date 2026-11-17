# AutoCrop

A client-side Fabric mod that replants mature crops you break and optionally harvests nearby crops automatically.

Active support covers **Minecraft 26.1, 26.2, 26.3, and 1.21.11**. Each version is maintained and built from its own named branch. `main` contains only the latest Minecraft implementation (26.3). All other Minecraft versions are legacy and remain on their historical branches.

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

Use Java 25 and check out the version branch you want to build:

```bash
git switch 26.3     # or 26.2, 26.1, 1.21.11
./gradlew build --warning-mode fail
```

`main` and `26.3` default to Minecraft 26.3. Each other version branch defaults to its own version and contains only that version's dependency profile and compatibility adapter. Switching a Gradle property does not replace switching branches.

The wrapper uses Gradle 9.9.1. Jars are written to `build/<version>/libs/`. Use the jar without `-sources` or `-dev` for playing. Minecraft 1.21.11 produces a remapped distribution jar.

## Smart GitHub Actions builds

Pushes to a supported version branch build that branch's version when build-related files change. Documentation-only changes skip compilation. A push to `main` builds the canonical `26.3` branch. Main never supplies the source for older Minecraft builds.

| Selected version | Source branch |
|---|---|
| 26.1 | `26.1` |
| 26.2 | `26.2` |
| 26.3 | `26.3` |
| 1.21.11 | `1.21.11` |

To choose manually, open **Actions → build → Run workflow** and enter a comma-separated version list such as **`26.3,26.2`**, **`26.1,1.21.11`**, or **`all`**. The workflow resolves each named version branch to its own commit before starting the build matrix. The workflow's selected branch supplies the automation definition; the requested version branches supply the Minecraft source. Each artifact and job summary identifies its version and source commit.

Spaces and duplicates are handled automatically; unsupported or legacy versions fail with a list of supported targets. Push builds pin the triggering revision of their version branch, even if that branch advances before a runner starts. Missing version branches fail rather than falling back to main. **Build result** reports the combined outcome.

## Draft releases

Open **Actions → release-draft → Run workflow**:

1. Select `main` for the latest release workflow.
2. Enter a release tag such as `v1.5.0`, or leave it blank to use `mod_version`.
3. Enter versions such as `26.3,26.2,1.21.11`, or `all`.
4. Optionally enter a previous-release tag as the changelog baseline.

The workflow snapshots the selected version branch commits, tests/builds each one, and creates or updates an unpublished draft. The requested release version is applied to jar names and metadata. Each build checks out its pinned version-branch commit, including when the release tag points to main. Tags remain immutable release anchors; they are not substituted for the version branch sources.

Draft assets include jars, checksums, `SOURCE_COMMIT.txt` (release anchor), `RELEASE_SOURCES.json` (each version branch and exact built commit), and full release notes. Notes include every commit reachable from the release anchor or selected branch sources since the previous published release, deduplicated across branches, including direct commits without pull requests. When the previous release has a source manifest, its version-branch history is excluded too. Older releases fall back to their release tag. Direct commit links, version source links, and a comparison link are included.

A new manual run snapshots the then-current version branches. Rerunning a draft refreshes notes/assets and removes obsolete AutoCrop jars if the selection changes. Published releases cannot be overwritten. Review and publish drafts manually.

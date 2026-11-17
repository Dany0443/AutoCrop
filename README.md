# AutoCrop

A client-side Fabric mod that replants mature crops you break and optionally harvests nearby crops automatically.

Active development targets **Minecraft 26.2 and 26.3**. Both jars are built from the same gameplay sources. Historical Minecraft versions remain available on their existing version branches.

## Install and start farming

1. Choose the AutoCrop jar matching your Minecraft version from GitHub Releases. GitHub Actions also provides development jars under each successful build's **Artifacts** section.
2. Install Fabric Loader **0.20.1 or newer**, Fabric API, Cloth Config, and ModMenu for that same Minecraft version. Use **Java 25**.
3. Put the mod jars in your instance's `mods` folder and launch Minecraft.
4. Open **Mods → AutoCrop → Configure** to choose your mode and settings.
5. Optionally assign **Cycle Harvest Mode** under **Options → Controls → Key Binds → AutoCrop**. It starts unbound to avoid key conflicts.

**Manual replanting is the default.** Keep the appropriate seeds, carrots, or potatoes in your hotbar or main inventory, then break a fully grown wheat, carrot, potato, or beetroot crop. AutoCrop attempts to replant it after the configured delay.

## Modes and settings

- **Off:** AutoCrop performs no farming actions.
- **Replant crops I break:** You harvest mature crops yourself; AutoCrop attempts to replant them. Automatic retries are not available in this mode.
- **Automatic harvest (check server rules):** AutoCrop scans nearby mature crops, breaks up to the configured batch size, and queues replanting. Only enable it where server rules allow automation.

**Auto-refill seeds** can swap seeds from your main inventory into the hotbar. It prefers an empty slot; if the hotbar is full, it swaps with the selected slot and moves that item into your inventory.

**Remember missed replants** retries failed replants in automatic mode. Turning it off clears remembered positions. Queues are also cleared when changing modes, worlds, player sessions, or disconnecting.

**Fill empty farm plots** is a separate option, **off by default**. With automatic mode and remembered replants enabled, it also plants unrelated empty farmland based on neighboring crops, or plants nether wart on empty soul sand. It can fill deliberately empty plots and change mixed-crop layouts.

Timing controls use game ticks. Increase replant delay if the server has not processed a crop break before planting is attempted. These settings do not guarantee acceptance by server anti-cheat or protection systems.

## Build either Minecraft version

The Gradle wrapper downloads Gradle **9.9.1**. Install a JDK 25, then run:

```bash
./gradlew build                         # Minecraft 26.3 (default)
./gradlew build -Pminecraft_target=26.2
```

Outputs are isolated by target:

```text
build/26.2/libs/autocrop-<version>-mc26.2.jar
build/26.3/libs/autocrop-<version>-mc26.3.jar
```

Use the jar without `-sources` for playing. Target-specific dependencies live in `versions/26.2.properties` and `versions/26.3.properties`; shared tooling versions live in `gradle.properties`.

## Smart GitHub Actions builds

Pushes and pull requests automatically select targets based on changed paths:

| Change | Targets compiled |
|---|---|
| Shared gameplay, resources, tests, build tools, or workflows | 26.2 and 26.3 |
| `versions/26.2.properties` or `src/minecraft26.2/` | 26.2 |
| `versions/26.3.properties` or `src/minecraft26.3/` | 26.3 |
| Documentation only | Compilation skipped |

Multiple target-specific changes are combined. Unknown paths and unavailable comparison history conservatively build both targets. New branches also build both.

To choose manually, open **Actions → build → Run workflow**, select the branch containing your changes, and choose **both**, **26.2**, or **26.3**. Each selected target gets its own downloadable jar artifact. All builds compile the selected revision's shared source; they do not fetch separate version-branch implementations.

The stable **Build result** job can be used as a required status check, including documentation-only pull requests.

## Draft releases

Set `mod_version` in `gradle.properties` to the release version, then tag that commit with the same version (optionally prefixed with `v`). A tag push builds both active targets and creates a **draft**, with jar checksums and the source commit attached.

Alternatively, run **Actions → release-draft** with an existing release tag and your chosen Minecraft versions. The tag must match `mod_version` in its commit. Every jar uses that exact commit; moving version branches are not used. Review and publish drafts manually.

## Troubleshooting

- **Nothing happens:** Check the mode, crop maturity, seed availability, and distance. The mode key is unbound until you assign it.
- **A manual replant fails:** Keep seeds available, stay close, and try increasing replant delay. Manual mode does not retain failed replants.
- **Your held item moved:** A full hotbar caused auto-refill to swap with your selected slot. Keep a slot empty or disable auto-refill.
- **Unexpected planting on empty plots:** Turn off Fill Empty Farm Plots.
- **A mod fails to load:** Confirm all dependency jars match the Minecraft target and meet the minimum versions in the AutoCrop jar.

Report bugs with Minecraft and AutoCrop versions, mode/settings, singleplayer or server context, reproduction steps, and relevant logs.

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).

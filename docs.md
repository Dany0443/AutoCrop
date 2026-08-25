# AutoCrop Documentation

Client-side crop automation and Quality of Life (QoL) mod for Fabric Minecraft.

[Documentation](docs.md) | License: GPL-3.0

---

## 1. Overview & Architecture

AutoCrop is a client-side Fabric mod designed to eliminate tedious repetitive actions during farming. It monitors player actions and the surrounding environment to automatically replant or batch-harvest mature crops.

### Key Highlights
- **100% Client-Side**: No server-side installation required. Works on singleplayer worlds, LAN games, and multiplayer servers.
- **Cross-Version Architecture**: Dedicated support across 13 Minecraft versions (`1.21.1` to `1.21.11`, `26.1`, `26.2`).
- **Zero Vanilla De-sync**: Respects client tick cycles, interaction hands, and server packet timing to prevent ghost blocks.
- **Smart Inventory Refill**: Automatically locates matching seeds across main inventory slots and swaps them into the hotbar.
- **Missed Replant Memory Queue**: Remembers unplanted farmland coordinates when running low on seeds and retries once seeds are acquired.

---

## 2. Core Mechanics

### 2.1 Harvest Modes

The mod provides three distinct operation modes that can be toggled in real time:

| Mode | Behavior | Use Case |
|---|---|---|
| `DISABLED` | Mod is inactive. Standard vanilla mechanics. | Normal gameplay or strict minigame environments. |
| `MANUAL` | Player breaks a mature crop manually; mod automatically queues an instant replant packet. | Standard vanilla survival, public SMPs, and anti-cheat servers. |
| `HARVEST_RISKY` | Automated 3D aura scanner breaks and replants mature crops within reach radius. | Private SMPs, singleplayer farms, and massive automated harvesting. |

> [!WARNING]
> `HARVEST_RISKY` sends automated block-break packets. Do not use this mode on competitive servers with strict anti-cheat plugins (such as GrimAC or Vulcan) to avoid accidental rate-limit flags.

### 2.2 Supported Crops & Seed Mapping

AutoCrop detects max-age crop block states and correctly maps the corresponding replant item:

| Crop Block | Maximum Age | Seed Item | Required Soil |
|---|---|---|---|
| Wheat (`CropBlock`) | 7 | `Items.WHEAT_SEEDS` | Farmland |
| Carrots (`CarrotBlock`) | 7 | `Items.CARROT` | Farmland |
| Potatoes (`PotatoBlock`) | 7 | `Items.POTATO` | Farmland |
| Beetroots (`BeetrootBlock`) | 3 | `Items.BEETROOT_SEEDS` | Farmland |
| Nether Wart (`NetherWartBlock`) | 3 | `Items.NETHER_WART` | Soul Sand |

### 2.3 Automatic Seed Refill

When replanting:
1. AutoCrop checks the player's 9 hotbar slots for the required seed.
2. If the seed is absent from the hotbar but `autoRefillSeeds` is enabled, the mod scans inventory slots 9 to 35.
3. The found seed stack is swapped into the current hotbar slot using client-side inventory interaction packets.
4. After planting, the original hotbar selection is seamlessly restored.

### 2.4 Missed Replant Memory & Retry Mechanism

If a crop is harvested but cannot be replanted immediately (e.g., inventory empty, out of reach, or player moving quickly):
1. The farmland position and required seed type are saved into an internal memory ring buffer (up to 2048 positions).
2. The mod enters a retry cooldown window (`missedReplantRetryCooldownTicks`).
3. During client ticks, the mod scans surrounding air blocks above farmland/soul sand, infers neighboring crop seed types if needed, and completes the planting as soon as seeds enter the inventory.
4. Once successfully planted or if the soil is destroyed/trampled, the location is removed from the memory buffer.

### 2.5 Keybindings & HUD Overlay

- **Cycle Harvest Mode**: Configurable in Options -> Key Binds -> AutoCrop (`autocrop.keybind.cycle`).
- Pressing the key cycles: `DISABLED` -> `MANUAL` -> `HARVEST_RISKY` -> `DISABLED`.
- An overlay message appears above the hotbar showing the newly selected mode in real time.

---

## 3. Configuration Reference

AutoCrop uses Cloth Config to save options to `config/autocrop.json`. All settings can be adjusted in-game via ModMenu.

### General Settings

| Option | JSON Key | Type | Default | Range | Description |
|---|---|---|---|---|---|
| Harvest Mode | `harvestMode` | Enum | `MANUAL` | `DISABLED`, `MANUAL`, `HARVEST_RISKY` | Active harvesting behavior. |
| Replant Delay | `replantDelayTicks` | Integer | `3` | `0` - `10` ticks | Delay between breaking a crop and executing the replant. Increase on high-ping servers. |
| Auto-Refill Seeds | `autoRefillSeeds` | Boolean | `true` | `true` / `false` | Automatically pull seeds from main inventory into hotbar when hotbar is empty. |

### Harvest (Risky) Settings

| Option | JSON Key | Type | Default | Range | Description |
|---|---|---|---|---|---|
| Aura Cooldown | `auraCooldownTicks` | Integer | `5` | `1` - `20` ticks | Delay in ticks between automated harvesting sweeps. |
| Risk Batch Size | `riskBatchSize` | Integer | `5` | `1` - `32` crops | Maximum number of crops harvested per aura tick sweep. |
| Remember Missed Replants | `rememberMissedReplants` | Boolean | `true` | `true` / `false` | Store unplanted holes in a memory buffer to retry later. |
| Retry Cooldown | `missedReplantRetryCooldownTicks` | Integer | `20` | `1` - `200` ticks | Ticks to wait before re-checking unplanted farmland blocks. |

---

## 4. Multi-Version Support & Structure

AutoCrop maintains standalone compilation targets for each supported Minecraft release:

```
autocrop/
├── build.gradle                   # Root Gradle build script (MC 26.2 target)
├── gradle.properties              # Root properties (mod_version, java 25)
├── settings.gradle                # Root settings
├── LICENSE                        # GNU General Public License v3.0
├── docs.md                        # Complete mod documentation
├── README.md                      # Repository readme
├── src/                           # Shared modern codebase (MC 26.x)
└── versions/                      # Standalone version subprojects
    ├── 1.21.1/                    # Target Minecraft 1.21.1
    ├── 1.21.2/                    # Target Minecraft 1.21.2
    ├── 1.21.3/                    # Target Minecraft 1.21.3
    ├── 1.21.4/                    # Target Minecraft 1.21.4
    ├── 1.21.5/                    # Target Minecraft 1.21.5
    ├── 1.21.6/                    # Target Minecraft 1.21.6
    ├── 1.21.7/                    # Target Minecraft 1.21.7
    ├── 1.21.8/                    # Target Minecraft 1.21.8
    ├── 1.21.9/                    # Target Minecraft 1.21.9
    ├── 1.21.10/                   # Target Minecraft 1.21.10
    ├── 1.21.11/                   # Target Minecraft 1.21.11
    ├── 26.1/                      # Target Minecraft 26.1
    ├── 26.2/                      # Target Minecraft 26.2
    ├── build-1.21.x.gradle        # Shared Gradle configuration for 1.21.x
    ├── build-26.x.gradle          # Shared Gradle configuration for 26.x
    └── new-version.sh             # Scaffold script for new Minecraft versions
```

---

## 5. Development & Building

### Prerequisites
- Java Development Kit (JDK) 25
- Git

### Build Commands

To build all subprojects or a specific target from the repository root:

```bash
# Build root project (MC 26.2)
./gradlew build

# Build specific Minecraft version subproject
./gradlew -p versions/1.21.1 build
```

### Adding a New Minecraft Version

To scaffold a new subproject target:

```bash
./versions/new-version.sh 1.21.12 1.21.11
```

Then verify mappings and dependencies inside `versions/1.21.12/gradle.properties`.

---

## 6. License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for complete details.

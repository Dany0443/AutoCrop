# AutoCrop Documentation

Client-side crop automation and Quality of Life (QoL) mod for Fabric Minecraft.

[Documentation](docs.md) | License: GPL-3.0

---

## 1. Overview & Architecture

AutoCrop is a client-side Fabric mod designed to eliminate tedious repetitive actions during farming. It monitors player actions and the surrounding environment to automatically replant or batch-harvest mature crops.

### Key Highlights
- **100% Client-Side**: No server-side installation required. Works on singleplayer worlds, LAN games, and multiplayer servers.
- **Cross-Version Architecture**: Dedicated support across 14 Minecraft versions (`1.21.1` to `1.21.11`, `26.1`, `26.2`, `26.3`).
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

## 4. Multi-Version Support & Architecture

AutoCrop utilizes the **Branch-Per-Version Architecture**:
- Every branch is a standalone, self-contained Gradle project that builds immediately upon checkout.
- `main` always tracks the latest supported Minecraft version (**Minecraft 26.3**).
- Dedicated git branches exist for all supported earlier releases:
  - `26.2`
  - `26.1`
  - `1.21.11` down through `1.21.1`
- No nested `versions/` subdirectories exist on any branch, eliminating duplicated wrappers and repository bloat.

### Branch Layout

```
autocrop/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.yml
│   │   └── feature_request.yml
│   ├── workflows/
│   │   ├── build.yml           # CI: Compiles jar on push and PR
│   │   ├── commit-lint.yml     # Lints PR titles and commits
│   │   └── release-draft.yml   # Multi-version release matrix
│   ├── dependabot.yml          # Monthly GitHub Actions upgrades
│   └── pull_request_template.md
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   └── main/
│       ├── java/com/danz/autocrop/...
│       └── resources/...
├── .commitlintrc.json
├── .gitattributes
├── .gitignore
├── build.gradle
├── CONTRIBUTING.md
├── docs.md
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
├── README.md
└── settings.gradle
```

---

## 5. Development & Building

### Prerequisites
- Java Development Kit (JDK) 25
- Git

### Build Commands

Each version branch is completely self-contained at root:

```bash
# Build current branch (e.g. MC 26.3 on main)
./gradlew build

# Switch to another Minecraft version and build
git switch 26.2
./gradlew build

git switch 1.21.1
./gradlew build
```

---

## 6. License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for complete details.

# AutoCrop

A lightweight, client-side QoL (Quality of Life) mod for Fabric Minecraft that handles the tedious parts of farming for you. It automatically harvests and replants crops based on your settings.

License: [GPL-3.0](LICENSE)

---

## Supported Versions

AutoCrop uses a **Branch-Per-Version Architecture** compiled on Java 25 across 14 Minecraft releases. Every branch is a standalone, self-contained project:

| Minecraft Version | Dedicated Git Branch | Fabric Loader | Fabric API |
|---|---|---|---|
| **26.3 (Latest)** | `main` | `>=0.19.5` | `>=0.162.0+26.3` |
| 26.2 | `26.2` | `>=0.18.4` | `>=0.145.2+26.1.1` |
| 26.1 | `26.1` | `>=0.18.4` | `>=0.145.2+26.1.1` |
| 1.21.11 | `1.21.11` | `>=0.19.2` | `>=0.141.4+1.21.11` |
| 1.21.10 | `1.21.10` | `>=0.19.2` | `>=0.138.4+1.21.10` |
| 1.21.9 | `1.21.9` | `>=0.19.2` | `>=0.134.1+1.21.9` |
| 1.21.8 | `1.21.8` | `>=0.19.2` | `>=0.136.1+1.21.8` |
| 1.21.7 | `1.21.7` | `>=0.19.2` | `>=0.129.0+1.21.7` |
| 1.21.6 | `1.21.6` | `>=0.19.2` | `>=0.128.2+1.21.6` |
| 1.21.5 | `1.21.5` | `>=0.19.2` | `>=0.128.2+1.21.5` |
| 1.21.4 | `1.21.4` | `>=0.19.2` | `>=0.119.4+1.21.4` |
| 1.21.3 | `1.21.3` | `>=0.19.2` | `>=0.114.1+1.21.3` |
| 1.21.2 | `1.21.2` | `>=0.19.2` | `>=0.106.1+1.21.2` |
| 1.21.1 | `1.21.1` | `>=0.19.2` | `>=0.116.11+1.21.1` |

---

## Core Mechanics & Harvest Modes

You can cycle through three different modes using the configured in-game keybind (default: Options -> Controls -> Key Binds -> AutoCrop):

| Mode | Behavior | Recommended Use Case |
|---|---|---|
| `DISABLED` | The mod is entirely inactive. Vanilla behavior. | Normal gameplay or strict minigame environments. |
| `MANUAL` | Player breaks a mature crop manually; mod automatically queues an instant replant packet. | Standard vanilla survival, public SMPs, anti-cheat servers. |
| `HARVEST_RISKY` | Automated 3D aura scanner breaks and replants mature crops within reach radius. | Private SMPs, singleplayer farms, massive automated harvesting. |

> [!WARNING]
> `HARVEST_RISKY` sends automated block-break packets. Do not use this mode on competitive servers with strict anti-cheat plugins to avoid accidental rate-limit flags.

### Supported Crops & Seeds

| Crop Block | Maximum Age | Seed Item | Required Soil |
|---|---|---|---|
| Wheat (`CropBlock`) | 7 | `Items.WHEAT_SEEDS` | Farmland |
| Carrots (`CarrotBlock`) | 7 | `Items.CARROT` | Farmland |
| Potatoes (`PotatoBlock`) | 7 | `Items.POTATO` | Farmland |
| Beetroots (`BeetrootBlock`) | 3 | `Items.BEETROOT_SEEDS` | Farmland |
| Nether Wart (`NetherWartBlock`) | 3 | `Items.NETHER_WART` | Soul Sand |

---

## Features

* **Auto-Refill Seeds:** If your hotbar runs out of seeds, the mod automatically scans inventory slots 9-35 and swaps matching seeds into your active hotbar slot.
* **Missed Replant Memory Buffer:** Remembers unplanted farmland coordinates when running low on seeds and retries once seeds enter the inventory.
* **Custom Tick Delays:** Configurable replant and break delays (in ticks) to prevent ghost blocks on laggy or high-ping servers.
* **In-Game Configuration:** Fully integrated with Cloth Config and ModMenu so you can adjust batch sizes, delays, and aura cooldowns without restarting.

---

## Configuration Reference

AutoCrop uses Cloth Config to save options to `config/autocrop.json`. All settings can be adjusted in-game via ModMenu.

### General Settings

| Option | JSON Key | Type | Default | Range | Description |
|---|---|---|---|---|---|
| Harvest Mode | `harvestMode` | Enum | `MANUAL` | `DISABLED`, `MANUAL`, `HARVEST_RISKY` | Active harvesting behavior. |
| Replant Delay | `replantDelayTicks` | Integer | `3` | `0` - `10` ticks | Delay between breaking a crop and executing the replant. |
| Auto-Refill Seeds | `autoRefillSeeds` | Boolean | `true` | `true` / `false` | Automatically pull seeds from main inventory into hotbar. |

### Harvest (Risky) Settings

| Option | JSON Key | Type | Default | Range | Description |
|---|---|---|---|---|---|
| Aura Cooldown | `auraCooldownTicks` | Integer | `5` | `1` - `20` ticks | Delay in ticks between automated harvesting sweeps. |
| Risk Batch Size | `riskBatchSize` | Integer | `5` | `1` - `32` crops | Maximum number of crops harvested per aura sweep. |
| Remember Missed Replants | `rememberMissedReplants` | Boolean | `true` | `true` / `false` | Store unplanted positions in a buffer to retry later. |
| Retry Cooldown | `missedReplantRetryCooldownTicks` | Integer | `20` | `1` - `200` ticks | Ticks between retrying remembered farmland blocks. |

---

## Requirements & Installation

1. Install the following in your `.minecraft/mods` folder:
   - **Fabric Loader** (see version table above)
   - **Fabric API**
   - **Java 25**
   - **Cloth Config API**
   - **ModMenu** (optional, to access the in-game settings screen)
2. Drop the compiled AutoCrop `.jar` for your Minecraft version into `.minecraft/mods`.
3. Launch Minecraft and configure your keybind under Options -> Controls -> Key Binds -> AutoCrop.

---

## Building from Source

Each version branch is completely self-contained at the repository root:

```bash
# Build current branch (Minecraft 26.3 on main)
./gradlew build

# Switch to another version branch and build
git switch 26.2
./gradlew build

git switch 1.21.1
./gradlew build
```

---

## License

GNU General Public License v3.0 (GPL-3.0). See [LICENSE](LICENSE) for details.

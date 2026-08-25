# AutoCrop

A lightweight, client-side QoL (Quality of Life) mod for Fabric Minecraft that handles the tedious parts of farming for you. It automatically harvests and replants crops based on your settings.

[Documentation](docs.md) | License: GPL-3.0

---

## Supported Versions

AutoCrop provides standalone builds compiled on Java 25 across 13 Minecraft releases:

| Minecraft Version | Loom Remap Target | Recommended Fabric Loader |
|---|---|---|
| 1.21.1 | `versions/1.21.1` | `>=0.19.2` |
| 1.21.2 | `versions/1.21.2` | `>=0.19.2` |
| 1.21.3 | `versions/1.21.3` | `>=0.19.2` |
| 1.21.4 | `versions/1.21.4` | `>=0.19.2` |
| 1.21.5 | `versions/1.21.5` | `>=0.19.2` |
| 1.21.6 | `versions/1.21.6` | `>=0.19.2` |
| 1.21.7 | `versions/1.21.7` | `>=0.19.2` |
| 1.21.8 | `versions/1.21.8` | `>=0.19.2` |
| 1.21.9 | `versions/1.21.9` | `>=0.19.2` |
| 1.21.10 | `versions/1.21.10` | `>=0.19.2` |
| 1.21.11 | `versions/1.21.11` | `>=0.19.2` |
| 26.1 | `versions/26.1` | `>=0.18.4` |
| 26.2 | `versions/26.2` | `>=0.18.4` |

---

## How It Works

You can cycle through three different modes using an in-game keybind:
* **Disabled:** The mod is entirely inactive. Vanilla behavior.
* **Manual:** You break a fully grown crop yourself, and the mod automatically sends a packet to replant it for you.
* **Harvest (Risky):** The mod actively scans your surroundings and automatically breaks and replants mature crops in a radius. *(Note: Called "risky" because aggressive settings might get flagged by strict server anti-cheats).*

---

## Features

* **Auto-Refill Seeds:** If your hotbar runs out of seeds, the mod will automatically pull a stack from your main inventory to keep planting.
* **Missed Replant Memory:** If you run out of seeds completely, it remembers which dirt blocks are empty and will automatically replant them as soon as you pick up more seeds.
* **Custom Tick Delays:** Tweak the delay (in ticks) for breaking and replanting. Bump this up if you are playing on a laggy or high-ping server to prevent ghost blocks.
* **In-Game Config:** Fully integrated with Cloth Config so you can tweak your harvest batch sizes and cooldowns without leaving the game.

---

## Requirements

Make sure you have these installed:
* Fabric Loader (see version table)
* Fabric API
* Java 25
* Cloth Config API (Required for settings)
* ModMenu (Required to access the config screen)

---

## Installation

1. Drop the compiled `.jar` file into your `.minecraft/mods` folder along with the required dependencies.
2. Launch the game.
3. Configure your preferred keybind in Options -> Key Binds -> AutoCrop.

---

## Documentation

For full configuration guides, mechanics breakdown, and development instructions, see [docs.md](docs.md).

---

## License

GNU General Public License v3.0 (GPL-3.0). See [LICENSE](LICENSE) for details.

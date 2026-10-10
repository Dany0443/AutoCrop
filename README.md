# AutoCrop

A lightweight, client-side QoL (Quality of Life) mod for Fabric Minecraft that handles the tedious parts of farming for you. It automatically harvests and replants crops based on your settings.

[Documentation](docs.md) | License: GPL-3.0

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

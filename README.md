# AutoCrop

A lightweight, client-side QoL (Quality of Life) mod for Minecraft 26.1 Fabric that handles the tedious parts of farming for you. It automatically harvests and replants crops based on your settings.

## How it works
You can cycle through three different modes using an in-game keybind:
* **Disabled:** The mod is entirely inactive. Vanilla behavior.
* **Manual:** You break a fully grown crop yourself, and the mod automatically sends a packet to replant it for you.
* **Harvest (Risky):** The mod actively scans your surroundings and automatically breaks and replants mature crops in a radius. *(Note: Called "risky" because aggressive settings might get flagged by strict server anti-cheats).*

## Features
* **Auto-Refill Seeds:** If your hotbar runs out of seeds, the mod will automatically pull a stack from your main inventory to keep planting.
* **Missed Replant Memory:** If you run out of seeds completely, it remembers which dirt blocks are empty and will automatically replant them as soon as you pick up more seeds.
* **Custom Tick Delays:** Tweak the delay (in ticks) for breaking and replanting. Bump this up if you are playing on a laggy/high-ping server to prevent ghost blocks.
* **In-Game Config:** Fully integrated with Cloth Config so you can tweak your harvest batch sizes and cooldowns without leaving the game.

## Requirements
Make sure you have these installed:
* Minecraft 26.1
* Fabric Loader >=0.18.4
* Fabric API >=0.145.0
* Java 25
* Cloth Config API (Required for settings)
* ModMenu (Required to access the config screen)

## Installation
Drop the compiled `.jar` file into your `.minecraft/mods` folder along with the required dependencies, launch the game, and check your keybinds to get started.

## License
MIT

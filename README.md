<p align="center">
  <img src="src/main/resources/logo.png" alt="HexTale" width="160">
</p>

<h1 align="center">HexTale</h1>

<p align="center">
  <b>Study raw hextech, distil the essences of everything, burn found runes into your tools - and decide what to do when your magic tears the world open.</b>
</p>

<p align="center">
  <a href="https://www.curseforge.com/projects/1751402"><img alt="CurseForge" src="https://cf.way2muchnoise.eu/1751402.svg"></a>
  <img alt="Minecraft 26.3" src="https://img.shields.io/badge/Minecraft-26.3-62B47A">
  <a href="https://neoforged.net"><img alt="NeoForge" src="https://img.shields.io/badge/Loader-NeoForge-F16436"></a>
  <a href="LICENSE"><img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue"></a>
</p>

<p align="center">
  <img src="docs/screenshots/altar-ritual.jpg" alt="A ritual at the Hextech Altar" width="800">
</p>

## Features

⚡ **Born of a storm** - a lightning rod on amethyst shatters it into volatile orbs of raw hextech. Their dust wakes
your crafting table, furnace and cauldron into hextech machines.

🧪 **Everything is made of essences** - 22 of them, in every item. Boil items down in the alembic, split the brew in
the separator, keep it in labelled jars, carry it through brass pipes - and brew chemtech tonics at the Lab Bench.

📜 **Research, not recipes** - draw every device on a parchment at the Research Table: lead the ink to each goal and
read the numbers to steer around the curses hidden in the dark fields.

🔮 **The Hextech Altar** - pedestals give up their items, steles their runes, jars their essence: rituals forge hextech
tools and devices, and at last wake the altar into the Hexcore.

📜 **Runes you find** - rune scrolls lie in dungeon chests. Burn them into pickaxes, tools, clothes and machines:
smelting, vein mining, ore sight, tunnels through rock and many more.

🌀 **The Void** - careless magic wears the land thin: haze, blighted soil, mites that drink your hearts and at last a
breach full of voidlings. Heal the land with nullstone and the Purifier - or ring the breach with anchors, feed it,
and fold it into a heart you can carry anywhere.

🤖 **Automation in theme** - pylons that grab items with tendrils of light, a sorting matrix, the Whirl Reaper, the Hex
Sawmill, a smelting foundry and low-lag helpers for mob farms.

🧥 **Gear with a story** - three hextech pickaxes, a hextech sword, axe and shovel, hexsteel armour, the researcher's 3D
clothes and a hextech skypack that glides like an elytra.

📖 **A researcher's diary** - a star chart of the whole mod: the path you walked, the steps you can take now and the
rest hidden in fog, all written as a curious researcher's notes.

## Controls

| Action | Default |
|---|---|
| Open the Researcher's Diary | *Use* with the diary in hand |
| Start or stop an altar ritual | *Sneak* + *Use* the altar with an empty hand (or a redstone signal) |
| Link a pylon | *Use* the pylon, then *Use* the target |
| Set a pipe side | *Sneak* + *Use* the pipe with an empty hand |
| Fly with the skypack | *Jump*, then *Jump* again in the air |
| Skip the breach cinematic | `Backspace` |

## Crafting

Start with the **Researcher's Diary** (a book, an amethyst shard and a feather on a crafting table) and a lightning rod
on an amethyst block. The diary shows every next step and its recipe. In creative mode all items are in the
**Pocky Mods** tab.

## Configuration

`hextale-synced.toml`: how many orbs a strike gives, how far lightning runs through amethyst, and whether careless magic
wears the land thin at all (the Void can be turned off for a modpack).

## Compatibility

[![JEI](docs/compat/jei.png)](https://www.curseforge.com/minecraft/mc-mods/jei) ✅ **[JEI](https://www.curseforge.com/minecraft/mc-mods/jei)** (optional) - every recipe of the mod: workbench, alloys, crucible and brews, altar rituals and runes, boiling and separation, lab experiments, chemtech fuel and devices.

[![Jade](docs/compat/jade.png)](https://www.curseforge.com/minecraft/mc-mods/jade) ✅ **[Jade](https://www.curseforge.com/minecraft/mc-mods/jade)** (optional) - what jars, machines and mechanisms hold and do.

## Screenshots

![A breach of the Void on blighted land, voidlings at its feet](docs/screenshots/the-void.jpg)

![A researcher's workshop: jars of essence, the Research Table, alembic and separator](docs/screenshots/workshop.jpg)

![The Hexcore and the researcher's clothes](docs/screenshots/hexcore.jpg)

![The Researcher's Diary: a star chart of the whole mod](docs/screenshots/researchers-diary.jpg)

![The Research Table: drawing a device on the parchment](docs/screenshots/research-table.jpg)

## Installation

1. Install [NeoForge](https://neoforged.net) for Minecraft 26.3 (Java 25).
2. Put this mod into the `mods` folder (client and server).

You are free to use HexTale in any modpack.

## Building

```sh
./gradlew build
```

The jar is written to `build/libs/`.

## Credits

- Author: **Pocky**.
- Inspired by the hextech of **Arcane** and by [Thaumcraft](https://www.curseforge.com/minecraft/mc-mods/thaumcraft).
- All textures, models and sounds are made for this mod. Some particle sprites are built on the
  [Particle Pack](https://kenney.nl/assets/particle-pack) by Kenney (CC0).

<!-- more-mods:start -->
## More mods by Pocky

**🌦️ Weather & nature**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/hoarfrost-snow-blizzards"><img src="docs/more-mods/hoarfrost.png" width="48" alt="Hoarfrost: Snow & Blizzards"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/hoarfrost-snow-blizzards"><b>Hoarfrost: Snow & Blizzards</b></a> - seven kinds of snowfall, a living wind and soft snow on the land (<a href="https://github.com/Pocky-l/hoarfrost">source</a>)</td>
  </tr>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><img src="docs/more-mods/petrichor.png" width="48" alt="Petrichor: Rain & Storms"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><b>Petrichor: Rain & Storms</b></a> - realistic rain, puddles, drips and branching lightning (<a href="https://github.com/Pocky-l/petrichor">source</a>)</td>
  </tr>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><img src="docs/more-mods/rustling_leaves.png" width="48" alt="Rustling Leaves"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><b>Rustling Leaves</b></a> - falling leaves and leaf piles you can wade through and rake, client-side (<a href="https://github.com/Pocky-l/rustling-leaves">source</a>)</td>
  </tr>
</table>

**💡 Light**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/lumen-rigs"><img src="docs/more-mods/lumen_rigs.png" width="48" alt="Lumen Rigs"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/lumen-rigs"><b>Lumen Rigs</b></a> - aimable spotlights and floodlights with colored light and visible beams (<a href="https://github.com/Pocky-l/lumen-rigs">source</a>)</td>
  </tr>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/neon-glowsticks"><img src="docs/more-mods/neon_glowsticks.png" width="48" alt="Neon Glowsticks"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/neon-glowsticks"><b>Neon Glowsticks</b></a> - throwable glowsticks that bounce, roll and light up the dark (<a href="https://github.com/Pocky-l/neon-glowsticks">source</a>)</td>
  </tr>
</table>

**🎨 Graphics & performance**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/turbo-for-distant-horizons"><img src="docs/more-mods/dhturbo.png" width="48" alt="Turbo for Distant Horizons"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/turbo-for-distant-horizons"><b>Turbo for Distant Horizons</b></a> - Distant Horizons terrain generated several times faster (<a href="https://github.com/Pocky-l/dhturbo">source</a>)</td>
  </tr>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/pocky-shaders"><img src="docs/more-mods/pocky_shaders.png" width="48" alt="Pocky Shaders"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/pocky-shaders"><b>Pocky Shaders</b></a> - a cosy, natural shader pack that sets itself up (<a href="https://github.com/Pocky-l/pocky-shaders">source</a>)</td>
  </tr>
</table>

**⚔️ Gear & gameplay**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/holy-staff"><img src="docs/more-mods/holy_staff.png" width="48" alt="Holy Staff"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/holy-staff"><b>Holy Staff</b></a> - a healing staff with three skills and flying heal numbers (<a href="https://github.com/Pocky-l/holy-staff">source</a>)</td>
  </tr>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack"><img src="docs/more-mods/vacpack.png" width="48" alt="Rancher's Vacpack"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack"><b>Rancher's Vacpack</b></a> - a Slime Rancher vacuum gun for items and small mobs (<a href="https://github.com/Pocky-l/ranchers-vacpack">source</a>)</td>
  </tr>
</table>

**⚡ Tech & energy**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/mekanism-advancedgenerators"><img src="docs/more-mods/mekanismadvancedgenerators.png" width="48" alt="Mekanism Advanced Generators"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/mekanism-advancedgenerators"><b>Mekanism Advanced Generators</b></a> - nine tiers of solar panels, big solar generators, cubes and cables for Mekanism (<a href="https://github.com/Pocky-l/Mekanism-MoreSolarPanels">source</a>)</td>
  </tr>
</table>

**🛠️ Server tools**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/inventory-backups"><img src="docs/more-mods/inventorybackups.png" width="48" alt="Inventory Backups"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/inventory-backups"><b>Inventory Backups</b></a> - inventory and ender chest backups with one-click restore for admins (<a href="https://github.com/Pocky-l/inventory-backups">source</a>)</td>
  </tr>
</table>

<!-- more-mods:end -->

## License

[MIT](LICENSE)

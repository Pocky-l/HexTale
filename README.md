<p align="center">
  <img src="src/main/resources/logo.png" alt="HexTale" width="160">
</p>

<h1 align="center">HexTale</h1>

<p align="center">
  <b>Pages from a researcher's diary: hextech magic you study, distil and bind with runes.</b>
</p>

<p align="center">
  <a href="https://www.curseforge.com/projects/1751402"><img alt="CurseForge" src="https://cf.way2muchnoise.eu/1751402.svg"></a>
  <img alt="Minecraft 26.3" src="https://img.shields.io/badge/Minecraft-26.3-62B47A">
  <a href="https://neoforged.net"><img alt="NeoForge" src="https://img.shields.io/badge/Loader-NeoForge-F16436"></a>
  <img alt="Beta" src="https://img.shields.io/badge/Status-Beta-orange">
  <a href="LICENSE"><img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue"></a>
</p>

## Features

I always thought amethyst was good for nothing but spyglasses. Then, one stormy night, I set a lightning rod on an
amethyst block - and the strike shattered it into humming orbs of **raw hextech**, the spark running on through every
crystal around. I learned the hard way that raw hextech is volatile: one orb slipped from my hands on a cliff and burst
at my feet.

Its dust turned out to be the key. A pinch of it **woke my crafting table, furnace and cauldron**: the Hex Workbench now
runs on crystal charge, the Hex Furnace melts brass and hexsteel, and at the **lapidary** I cut raw orbs into true
crystals by feel, turning the stone until its hidden cleavage plane glows.

Soon I understood that **everything is made of essences** - twenty-two of them, from Fire and Water to Magic and Rift - and
every item holds its own mix. The loupe on my desk reveals it, the **alembic** boils items down, the **separator** splits
the brew, **jars and brass pipes** keep it and carry it wherever I need it.

![The Research Table: drawing a device on the parchment](docs/screenshots/research-table.jpg)

Nothing here is built before it is understood. At the **Research Table** I draw every device on a worn parchment,
leading ink from the hextech orb to each goal. Dark fields hide curses, and only the numbers along my ink tell me where
it is safe - one careless step and the sheet goes up in flames. Every finished drawing becomes notes I can read myself
or hand to a friend.

The **crucible** taught me transmutation: boiling water swallows whatever I throw in, and a catalyst rises out changed.
An ordinary sapling, fed with magic, plant and sap, became the sprout of a **magic tree** - violet wood with glowing
veins that never burns.

The **runes** I did not invent - I found them, as scrolls in old dungeon chests, their stars a matter of luck. At the
**Hextech Altar** the pedestals give up their items, the steles their runes, the jars their essence, and the ritual
burns runes into my pickaxe like enchantments: smelting, vein mining, ore sight, tunnels through rock.

Now my workshop runs on its own. **Pylons** catch items with tendrils of light and carry them between chests and
furnaces, a **Sorting Matrix** sends every item home, the **Whirl Reaper** harvests my fields with whirlwinds and the
**Hex Sawmill** fells whole trees and plants them again.

All of it is in my diary - the path I walked, the steps I can take now and one more into the fog. Many of its pages are
still blank: plans for chemtech, shimmer and the hexcore are waiting to be written.

![The Researcher's Diary: the path through the mod](docs/screenshots/researchers-path.jpg)

## Controls

| Action | Default |
|---|---|
| Open the Researcher's Diary | *Use* with the diary in hand |
| Turn the crystal at the lapidary | Mouse wheel or `A` / `D` |
| Link a pylon | *Use* the pylon, then *Use* the target |
| Set a pipe side | *Sneak* + *Use* the pipe with an empty hand |

## Crafting

Start with the **Researcher's Diary** (a book, an amethyst shard and a feather on a crafting table) and a lightning rod
on an amethyst block. The diary shows every next step and its recipe. In creative mode all items are in the
**Pocky Mods** tab.

## Configuration

`hextale-synced.toml`: how many hextech orbs a strike gives and how far lightning runs through amethyst.

## Compatibility

[![JEI](docs/compat/jei.png)](https://www.curseforge.com/minecraft/mc-mods/jei) ✅ **[JEI](https://www.curseforge.com/minecraft/mc-mods/jei)** (optional) - every recipe of the mod: workbench, alloys, crucible and brews, altar rituals and runes, boiling and separation, lab experiments, chemtech fuel and devices.

[![Jade](docs/compat/jade.png)](https://www.curseforge.com/minecraft/mc-mods/jade) ✅ **[Jade](https://www.curseforge.com/minecraft/mc-mods/jade)** (optional) - what jars, machines and mechanisms hold and do.

## Installation

1. Install [NeoForge](https://neoforged.net) for Minecraft 26.3 (Java 25).
2. Put this mod into the `mods` folder (client and server).

HexTale is in beta: back up your worlds. You are free to use it in any modpack.

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

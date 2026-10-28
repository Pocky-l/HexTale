<p align="center">
  <img src="docs/showcase/title.gif" alt="HexTale" width="834">
</p>

<p align="center">
  <a href="https://www.curseforge.com/minecraft/mc-mods/hextale"><img alt="CurseForge" src="https://cf.way2muchnoise.eu/1751402.svg"></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/hextale/files"><img alt="Minecraft 26.3, 26.1.2, 1.21.1" src="https://img.shields.io/badge/Minecraft-26.3%20%7C%2026.1.2%20%7C%201.21.1-62B47A"></a>
  <a href="https://neoforged.net"><img alt="NeoForge" src="https://img.shields.io/badge/Loader-NeoForge-F16436"></a>
  <a href="docs/modpack.md"><img alt="Modpacks welcome" src="https://img.shields.io/badge/Modpacks-welcome-8a6bd1"></a>
  <a href="LICENSE"><img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue"></a>
</p>

<p align="center"><i>Pages from the diary of a researcher who should have stopped after the first explosion.</i></p>

<p align="center"><b>Hextech magic in the spirit of Arcane and Thaumcraft. Study the world, boil it down to essences, draw your
devices before you build them, burn runes into everything you own, and live with what all that magic does to the land.</b></p>

<p align="center"><img src="docs/showcase/hexcore.gif" alt="A ritual on the Hexcore" width="720"></p>

<p align="center"><b>300+</b> blocks and items &middot; <b>22</b> essences &middot; <b>72</b> runes &middot; <b>33</b> things that take them &middot; <b>180</b> diary entries</p>

## Features

<h3 align="center">Page 4. What things are made of</h3>
<p align="center"><img src="docs/showcase/workshop.gif" alt="The workshop: jars of essence, alembic, separator and crucible" width="720"></p>
<p align="center"><i>Coal is fire and nothing else. Seeds are life. I boil everything down in the alembic,<br>keep the brew in jars (labelled, after one bad evening) and run it through brass pipes.</i></p>

<h3 align="center">Page 15. Runes in old chests</h3>
<p align="center"><img src="docs/showcase/altar.gif" alt="A ritual at the Hextech Altar" width="720"></p>
<p align="center"><i>I did not invent the runes. They turn up in dungeon chests and in the margins of my notes.<br>The altar burns them in. My pickaxe now smelts what it digs and sees ore through stone.</i></p>

<h3 align="center">Page 19. Small wonders that do the work</h3>
<p align="center">
  <img src="docs/showcase/pylons.gif" alt="Pylons gathering loot" width="264">
  <img src="docs/showcase/reaper.gif" alt="The Whirl Reaper" width="264">
  <img src="docs/showcase/sawmill.gif" alt="The Hex Sawmill" width="264">
</p>
<p align="center"><i>Pylons that pick things up with threads of light. A reaper that harvests with little whirlwinds.<br>A sawmill that fells and replants. Rift gates, a skypack, a claw on a cord of light.</i></p>

<h3 align="center">Page 22. Something is wrong with the grass</h3>
<p align="center"><img src="docs/showcase/void.gif" alt="A breach of the Void, voidlings at its feet" width="720"></p>
<p align="center"><i>Every jar I boiled wore the world a little thinner. Now there is a lilac haze at dusk,<br>and last week the air tore open behind the barn. I could heal the land. Or feed the breach.</i></p>

<h3 align="center">Every page. The diary</h3>
<p align="center"><img src="docs/showcase/diary.gif" alt="The Researcher's Diary" width="720"></p>
<p align="center"><i>Drawn as a star chart: every device, every brew, every rune, and the roads between them.</i></p>

## Getting started

Craft the **Researcher's Diary** (book + amethyst shard + feather); it shows every next step with its recipe. The first
hextech waits under glassy fulgurite scars on the surface, where an old strike left a geode of dormant crystal. Most
devices are researched at the Research Table before they can be made. In creative mode everything is in the Pocky Mods tab.

**For modpack makers:** every recipe is an ordinary data-driven recipe type (`hextale:hex_crafting`, `hextale:infusion`,
...), so data packs and [KubeJS](https://www.curseforge.com/minecraft/mc-mods/kubejs) can change them; materials go by
`c:` tags, machines and the Void respect claims, quests get advancement triggers. Working with essence slowly sickens the
land around a base (the Thinning); `thinning = false` in `hextale-synced.toml` turns it off. Everything is in
[the modpack guide](docs/modpack.md). Free to use in any modpack.

## Controls

| Action | Default |
|---|---|
| Open the Researcher's Diary | *Use* with the diary in hand |
| Start or stop an altar ritual | *Sneak* + *Use* the altar with an empty hand (or a redstone signal) |
| Link a pylon | *Use* the pylon, then *Use* the target |
| Set a pipe side | *Sneak* + *Use* the pipe with an empty hand |
| Fly with the skypack | *Jump*, then *Jump* again in the air |
| Listen with the tuning fork | Hold *Use* |
| Swing on the grapple | Hold *Use*; *Jump* reels in, *Sneak* lets out |
| Wind the recall charm | *Use* a jar of rift essence with the charm |
| Skip the breach cinematic | `Backspace` |

## Compatibility

<p align="center">
  <a href="https://www.curseforge.com/minecraft/mc-mods/jei"><img src="docs/compat/jei.png" width="48" height="48" alt="JEI" title="JEI - every recipe of the mod"></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/jade"><img src="docs/compat/jade.png" width="48" height="48" alt="Jade" title="Jade - what jars, machines and mechanisms hold and do"></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/curios"><img src="docs/compat/curios.png" width="48" height="48" alt="Curios" title="Curios - the Researcher's Goggles in a head slot"></a>
</p>

All optional: [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) shows every recipe of the mod,
[Jade](https://www.curseforge.com/minecraft/mc-mods/jade) what jars and machines hold and do,
[Curios](https://www.curseforge.com/minecraft/mc-mods/curios) puts the Researcher's Goggles in a head slot.

## Installation

1. Install [NeoForge](https://neoforged.net) for Minecraft 26.3 (Java 25); 26.1.2 and 1.21.1 builds are on the
   [files page](https://www.curseforge.com/minecraft/mc-mods/hextale/files).
2. Put this mod into the `mods` folder (client and server).

## Building

```sh
./gradlew build
```

The jar is written to `build/libs/`.

## Credits

- Author: **Pocky**.
- Inspired by the hextech of **Arcane** and by [Thaumcraft](https://www.curseforge.com/minecraft/mc-mods/thaumcraft).
- All textures, models and sounds are made for this mod. Some particle sprites are built on the
  [Particle Pack](https://kenney.nl/assets/particle-pack) by Kenney (CC0). The page banner uses the
  [Cinzel](https://fonts.google.com/specimen/Cinzel) typeface (SIL Open Font License).

<!-- more-mods:start icons -->
<p align="center"><b>More mods by Pocky</b></p>
<p align="center">
<a href="https://www.curseforge.com/minecraft/mc-mods/hoarfrost-snow-blizzards"><img src="docs/more-mods/hoarfrost.png" width="48" height="48" alt="Hoarfrost: Snow &amp; Blizzards" title="Hoarfrost: Snow &amp; Blizzards - seven kinds of snowfall, a living wind and soft snow on the land"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><img src="docs/more-mods/petrichor.png" width="48" height="48" alt="Petrichor: Rain &amp; Storms" title="Petrichor: Rain &amp; Storms - realistic rain, puddles, drips and branching lightning"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><img src="docs/more-mods/rustling_leaves.png" width="48" height="48" alt="Rustling Leaves" title="Rustling Leaves - falling leaves and leaf piles you can wade through and rake, client-side"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/lumen-rigs"><img src="docs/more-mods/lumen_rigs.png" width="48" height="48" alt="Lumen Rigs" title="Lumen Rigs - aimable spotlights and floodlights with colored light and visible beams"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/neon-glowsticks"><img src="docs/more-mods/neon_glowsticks.png" width="48" height="48" alt="Neon Glowsticks" title="Neon Glowsticks - throwable glowsticks that bounce, roll and light up the dark"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/turbo-for-distant-horizons"><img src="docs/more-mods/dhturbo.png" width="48" height="48" alt="Turbo for Distant Horizons" title="Turbo for Distant Horizons - Distant Horizons terrain generated several times faster"></a>
<a href="https://www.curseforge.com/minecraft/shaders/pocky-shaders"><img src="docs/more-mods/pocky_shaders.png" width="48" height="48" alt="Pocky Shaders" title="Pocky Shaders - a cosy, natural shader pack that shows the weather of these mods"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/holy-staff"><img src="docs/more-mods/holy_staff.png" width="48" height="48" alt="Holy Staff" title="Holy Staff - a healing staff with three skills and flying heal numbers"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack"><img src="docs/more-mods/vacpack.png" width="48" height="48" alt="Rancher's Vacpack" title="Rancher's Vacpack - a Slime Rancher vacuum gun for items and small mobs"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/mekanism-advancedgenerators"><img src="docs/more-mods/mekanismadvancedgenerators.png" width="48" height="48" alt="Mekanism Advanced Generators" title="Mekanism Advanced Generators - nine tiers of solar panels, big solar generators, cubes and cables for Mekanism"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/inventory-backups"><img src="docs/more-mods/inventorybackups.png" width="48" height="48" alt="Inventory Backups" title="Inventory Backups - inventory and ender chest backups with one-click restore for admins"></a>
</p>
<!-- more-mods:end -->

## License

[MIT](LICENSE)

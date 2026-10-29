# HexTale for modpack makers

Everything a pack can change without touching the mod's code: recipes, tags, world generation, configs, commands and
criteria for quests. HexTale is MIT licensed; use it in any modpack.

## Recipes

All of HexTale's recipes live in the vanilla recipe manager. Data packs override or remove them like any recipe, and
recipe scripts (KubeJS, KubeGroovy, CraftTweaker) see them: remove by id, by output or by type, add new ones with the
script's "custom JSON" call (`event.custom({...})` in KubeJS). The mod's machines never match recipes through the
recipe manager themselves, so the recipe book ignores them.

| Type | Machine | Folder in the mod |
|---|---|---|
| `minecraft:crafting_shaped` / `_shapeless`, smelting, blasting, stonecutting | vanilla stations | `data/hextale/recipe/*.json` |
| `hextale:hex_crafting` | Hex Workbench | `recipe/hex_crafting/` |
| `hextale:infusion` | Hextech Altar circle | `recipe/infusion/` |
| `hextale:alloying` | Hex Furnace | `recipe/alloying/` |
| `hextale:crucible` | Crucible (transmutation) | `recipe/crucible/` |
| `hextale:brewing` | Crucible (brews for Chem Vials) | `recipe/brewing/` |
| `hextale:lab_experiment` | Lab Bench (what a chemtech research asks for) | `recipe/lab_experiment/` |
| `hextale:awakening` | a block woken by hextech dust | `recipe/awakening/` |

Ingredients use the vanilla 1.21.1 format: `{"item": id}`, `{"tag": tag}` or a list of them (on Minecraft 26.3 a plain id
or `#tag`). Shared materials are asked for by
common tags (`#c:ingots/brass`, `#c:ingots/zinc`, `#c:ingots/iron`, `#c:glass_blocks/colorless`, ...), so brass or
zinc from other mods work, and unifiers can hide HexTale's copies.

Research still guards results: whatever recipe makes an item, the player needs the research that opens that item
(the Research Table or the Lab Bench). JEI names the research in the result's tooltip.

### `hextale:hex_crafting`

A shaped (`pattern` + `key`) or shapeless (`ingredients`, 1-9) grid like the vanilla crafting table, plus the
`charge` the workbench's crystal spends (0-64, default 1).

```json
{
  "type": "hextale:hex_crafting",
  "pattern": [" B ", "G G", "BGB"],
  "key": {"B": {"tag": "c:ingots/brass"}, "G": {"tag": "c:glass_blocks/colorless"}},
  "result": {"id": "hextale:alembic"},
  "charge": 1
}
```

### `hextale:infusion`

The altar's centre item, up to any number of pedestal items (each pedestal holds one, matched exactly), essences drawn
from the jars around the circle (mB by essence id), optional runes set on the steles, and the result.
`keep_components: true` lets the result keep the centre item's enchantments, damage and name.

```json
{
  "type": "hextale:infusion",
  "center": {"item": "hextale:hextech_crystal"},
  "pedestals": [{"item": "minecraft:lightning_rod"}, {"tag": "c:ingots/hexsteel"}, {"tag": "c:ingots/hexsteel"}, {"tag": "c:ingots/brass"}],
  "essences": {"spark": 16, "metal": 12, "crystal": 8},
  "result": "hextale:arc_spire"
}
```

Essence ids: the primals `fire`, `water`, `earth`, `air`, `light`, `shadow`, `life`, `metal`, `spark` and the
compounds `steam`, `magma`, `ash`, `plant`, `crystal`, `ore`, `cold`, `sap`, `beast`, `decay`, `magic`, `rift`,
`circuit`.

### `hextale:alloying`

One to three inputs (each in its own furnace slot, any order), the result, burning time in ticks (default 200) and
experience.

```json
{
  "type": "hextale:alloying",
  "inputs": [{"ingredient": {"tag": "c:ingots/copper"}, "count": 3}, {"ingredient": {"tag": "c:ingots/zinc"}, "count": 1}],
  "result": {"id": "hextale:brass_ingot", "count": 4},
  "time": 200,
  "experience": 0.7
}
```

### `hextale:crucible`

A catalyst thrown into boiling water that holds at least these essences turns into the result (the richest recipe
the water can pay wins).

```json
{
  "type": "hextale:crucible",
  "catalyst": {"tag": "minecraft:wool"},
  "essences": {"magic": 3, "beast": 2},
  "result": {"id": "hextale:arcane_cloth"}
}
```

### `hextale:brewing`

A catalyst in water with these essences, over at least `heat` (1-5; a Chem Burner gives 5), starts a brew; `doses` =
how many Chem Vials it fills. Brews: `fuel` (combustible mix), `growth` (growth tonic), `coolant`, `glow` (glow mix), `acid`, `raw_shimmer`,
`refined_shimmer`.

```json
{
  "type": "hextale:brewing",
  "catalyst": {"item": "minecraft:fermented_spider_eye"},
  "essences": {"decay": 8, "water": 4},
  "brew": "acid",
  "heat": 5,
  "doses": 3
}
```

### `hextale:lab_experiment`

What the Lab Bench asks to finish a chemtech research: the compound essence to make (`target`), how many parts
(`amount`), how pure (`purity` 0-1), and an optional item sample.

```json
{
  "type": "hextale:lab_experiment",
  "topic": "acid",
  "target": "decay",
  "amount": 8,
  "purity": 0.65,
  "sample": {"item": "minecraft:fermented_spider_eye"}
}
```

### `hextale:awakening`

Right-clicking any block of `block` (an id, a list or a `#tag`) with the `catalyst` (hextech dust when left out)
wakes it into `result` after a short swirl.

```json
{
  "type": "hextale:awakening",
  "block": ["minecraft:cauldron", "minecraft:water_cauldron"],
  "result": "hextale:crucible"
}
```

Not recipes: the multiblocks' shapes, what the lightning wave turns into hextech (block tag
`hextale:carries_lightning`), the lapidary's cutting, rune burning in the Rune Circle and the essences of items (data map
`hextale:essences`, `data/hextale/data_maps/item/essences.json`).

## Tags

Items:

| Tag | Use |
|---|---|
| `c:ingots/brass`, `c:nuggets/brass`, `c:storage_blocks/brass` | brass |
| `c:ingots/zinc`, `c:nuggets/zinc`, `c:raw_materials/zinc`, `c:ores/zinc`, `c:storage_blocks/zinc`, `c:storage_blocks/raw_zinc` | zinc and sphalerite |
| `c:ingots/hexsteel`, `c:storage_blocks/hexsteel` | hexsteel |
| `c:dusts/hextech`, `c:gems/hextech` | hextech dust, hextech crystal |
| `hextale:research_paper` | what the Research Table takes as parchment (paper) |
| `hextale:separator_fuels` | what burns in the Separator's fuel slot (blaze powder) |
| `hextale:fuel_catalysts` | catalysts of the combustible mix (coals) |
| `hextale:magic_tree_catalysts` | saplings the crucible turns into a magic tree sprout |
| `hextale:soaks_essence` | blocks a crucible or drain valve soaks into magic stone |
| `hextale:repairs_hexsteel`, `hextale:repairs_arcane_cloth` | repair materials |

Blocks: `hextale:carries_lightning` (what a lightning wave runs through and wakes), `hextale:resonant` (what the
Tuning Fork hears), `hextale:blastable` (what a Dust Charge breaks), `hextale:acid_dissolves`,
`hextale:blights_to_soil`, `hextale:blights_to_rock`, `hextale:void_ground`, `hextale:sorting_storage` (storage the
Sorting Matrix fills by itself), `hextale:whirl_crops`, `hextale:whirl_gourds`, `hextale:whirl_stalks`.

## World generation

| Feature | Biome tag (default: the overworld) |
|---|---|
| Sphalerite (zinc) ore | `hextale:has_sphalerite` |
| Struck geodes (the hextech start) | `hextale:has_struck_geodes` |
| Shimmerbloom caves | `hextale:has_bloom_caves` |

Empty a tag (`{"replace": true, "values": []}`) to turn a feature off. If a pack turns off struck geodes, hextech
still comes from lightning rods next to amethyst.

## Chest loot

Rune scrolls and hextech finds are added to vanilla chest loot by global loot modifiers
(`data/hextale/loot_modifiers/rune_scrolls_{common,deep,ancient}.json`, `hextech_finds_{common,deep}.json`). Each
lists the loot tables it adds to; override a file in a data pack to add other mods' dungeon chests or to change the
chances. The modifiers check the queried loot table, so per-player chests (Lootr) roll them for every player.

## Configs

`hextale-synced.toml` (server, synced to clients):

| Option | Default | What it does |
|---|---|---|
| `thinning` | true | Working essence wears the land around a base thin (haze, blight, Void Mites, breaches). Off = none of it. |
| `thinningRate` | 1.0 | How fast it grows (0-10). |
| `blightChangesBlocks` | true | Blighted land turns grass and stone into Void soil and rock and its plants into Void flora. |
| `voidMitesSpawn` | true | Void Mites crawl out of blighted ground. |
| `voidlingsSpawn` | true | Breaches send out voidlings (wild and fed). |
| `breaches` | true | Land worn all the way thin tears open into a breach by itself (a Breach Heart still unfolds one). |
| `breachCooldownMinutes` | 20 | Rest after a breach is gone before a new one may open within 1000 blocks. |
| `minOrbs` / `maxOrbs` | 1 / 1 | Unstable hextech orbs per amethyst block a lightning wave shatters. |
| `chainRadius` / `maxChain` | 5 / 125 | How far and how much amethyst one strike runs through. |

`hextale-client.toml`: `breachCinematic` (the camera flight when a breach opens near you), `showWipSteps` (ideas
in the diary that are not in the game yet).

What wears the land thin, by source, shows `/hextale void sources [radius]` (the Chem Boiler's cracking counts as
`fumes`). A lit Hextech Lamppost burns off a share (35-65% with runes) of every gain within its reach (8-16 blocks)
before it reaches the land - except the Void's own feeding around a breach - and its chunk never catches the creep of
the chunks around it; plan pack balance with that in mind.

## Servers and claims

Everything that breaks or places blocks asks NeoForge's block events first, so claim mods (FTB Chunks, Open Parties
and Claims, ...) protect against it:

- tools and gadgets act as the player using them (area mining runes, Dust Charge, Chem Vials, Spawner Cage, ...);
- placed devices (Hex Sawmill, Whirl Reaper, Drain Valve, Hexcore) act as the player who placed them;
- the Void's blight and lightning without a player act as the fake player **[HexTale]**, UUID
  `2030168d-9eab-3b8b-b353-18a58e583390` - allow or deny it in your claim mod.

Block tag `hextale:void_immune`: blocks the blight, the healing land and the Hexcore's lightning never touch.

## Automation

Other mods' pipes, tanks and hoppers:

| Block | Fluids | Items |
|---|---|---|
| Crucible | water in (any side) | catalysts and things to dissolve in, as if thrown in (not from below) |
| Alembic | water in (sides) | its sided slots, as before |
| Crystal Battery | - | crystals in |
| Hex Workbench | - | crystals into the socket (the grid is not exposed) |
| Hex Lapidary | - | crystal in from the top, disc from the sides, finished stone out from the bottom |
| Experience Condenser | liquid experience out (the first fluid in `c:experience`, 20 mB per point) | - |
| Pump | pushes water into another mod's tank on top | - |
| Essence Jar, Big Essence Jar | essence fluid in and out (see below) | - |
| Creative Essence Jar | every essence fluid out, endless | - |
| Hextech Purifier, Void Anchor | - | a hextech or synthetic crystal in from the top and the sides, a clouded one out from below |
| Chem Boiler | - (mixtures come in through brass pipes, bottles, or out of the unstickered jars around it) | - |
| Separator, Hex Furnace, Hex Foundry, Research Table, Lab Bench, Pedestal, Disintegrator, ... | - | as before (sided slots) |

Essence as a fluid: `hextale:essence` with the data component `hextale:essence_kind` (the essence id, e.g. `fire`);
1 mB of fluid = 1 mB of essence. A jar gives fluid only while it holds one essence and nothing else (mixtures are the
mod's own business and are never split); it takes the essence fluid its sticker allows. The fluid has no bucket and no
block, so it cannot be placed in the world.

Research still holds: hoppers and pipes never count as a researcher, so what they bring only makes what needs no
research. The Sorting Matrix fills any storage with an item handler; it finds chests, barrels and drawers by itself
through the block tag `hextale:sorting_storage`.

## Quests and advancements

Criteria triggers (each also takes the usual `player` condition):

| Trigger | Conditions | When |
|---|---|---|
| `hextale:research_completed` | `topic` (optional) | a research is finished (also on login for what the player already knows) |
| `hextale:guide_step` | `step` (optional) | a step of the Researcher's Diary is done |
| `hextale:guide_event` | `event` (optional) | something happened in the world: `breach_opened`, `hexcore_awakened`, `awakened/<block>`, `big_essence_jar_formed`, `shimmer_veins` (a vial of refined shimmer drunk), `veins_washed` (light essence washed a vein out), `shimmer_craving` (the shimmer's gifts ran out), ... |
| `hextale:ritual_finished` | `item` (item predicate), `kind` (`infuse`, `engrave`, `write`) | a ritual finishes near the player (`infuse` at the altar, `engrave` and `write` in the Rune Circle) |
| `hextale:runes_learned` | `count` | the codex holds at least `count` runes |

```json
{
  "criteria": {
    "battery": {"trigger": "hextale:research_completed", "conditions": {"topic": "crystal_battery"}}
  }
}
```

Commands (permission level 2), e.g. for quest rewards:

- `/hextale research grant|revoke <targets> <topic|all>`
- `/hextale guide grant|reset <targets> <step|all>` - grant also completes the steps before it.

## Shimmer in the veins

Every vial of refined shimmer drunk adds a vein (player attachment `hextale:shimmer_veins`: `count` 0-5 and
`fed_until`, the game time the gifts last until; kept through death). While fed the veins add transient attribute
modifiers `hextale:shimmer_veins_health` (+2 max health per vein), `hextale:shimmer_veins_damage` (+0.5 attack damage)
and `hextale:shimmer_veins_size` (+0.02 scale), Night Vision in the dark from 3 veins and Haste at 5. Unfed, the
modifiers are removed and nothing else happens. A bottle of essence with at least 8 mB of light essence removes a vein.

## Curios

With [Curios](https://www.curseforge.com/minecraft/mc-mods/curios) installed, the Researcher's Goggles also work from
a Curios head slot (item tag `curios:head`) and show on the head, over a helmet too.

## Stable ids

`docs/released-ids.txt` lists every item and block id HexTale has released. They never disappear: a renamed id keeps
working as an alias of its new name, so quests, recipes and worlds made for one version keep working in the next.

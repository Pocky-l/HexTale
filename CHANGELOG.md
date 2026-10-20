# Changelog

All notable changes to this mod are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-beta.2] - 2026-10-20
### Added
Every hextech tool and every working device now takes runes, and most runes are gifts you feel while you work: luck,
free buffs, rare golden finds. A rune that works shows it with a puff of dust in its colour. Runes are still found as
scrolls in treasure chests, so every scroll you find now has more places to go.

- Hextech pickaxe, eight new runes: Haste (a chance of Haste while digging, Haste II at three stars), Fertility (drops
  as with Fortune, on top of the enchantment), Radiance (more experience from ores, now and then some from stone),
  Breath (a chance to be fed a little; under water every block gives breath back), Banishing (Night Vision while
  digging in the dark), Fireproofing (Fire Resistance the moment a block opens lava), Eternal Flame (in the dark a hex
  wisp may stay where the block was - a mote of cold light that lights the tunnel for good and goes out at a touch)
  and Mending (wear may be shrugged off and a point mended).
- Hextech axe, shovel and sword take up to four runes like the pickaxe; a grindstone takes them off.
  - Axe: Gravity (the whole tree comes down at one blow, its leaves too; sneak for one block; a log wall without
    leaves is never felled), Growth (a tree cut at its foot is planted again, from a sapling in your bag or with a
    chance out of nothing), Pollen (leaves as with Fortune), Corrosion (a blow may eat through armour: more damage
    and the armour wears), Haste, Mending, Lens.
  - Shovel: Cache (soil, sand and gravel may hide a treasure - bones, flint, nuggets, clay, an emerald, a pottery
    sherd, a diamond, even a rune scroll), Erosion (digs 3 x 3 of soil, 3 x 3 x 2 at three stars; never the stone
    among it), Fertility (grass and soil give seeds, bulbs and saplings), Haste, Mending, Lens.
  - Sword: Withering (a blow may heal you), Chain Lightning (a blow may jump on as lightning to the hostile creatures
    around), Spoils (more loot, as with Looting), Radiance (more experience), Bellows (a kill may give Strength),
    Flash (the first blow on an unhurt creature strikes harder), Engraving (a kill may leave the head of a zombie,
    skeleton, creeper or piglin), Mending, Lens.
- Whirl Reaper, three runes instead of two and six new ones: Fertility (a reaped crop may be bonemealed at once),
  Pollen (one or two more of the produce), Photosynthesis (now and then a golden carrot, a glistering melon slice or a
  golden apple), Spring (every harvest wets the farmland around and may quicken the plants next to it), Radiance
  (harvests give experience, gathered over the reaper) and Dam (a whirl may cost no wind).
- Hex Sawmill, three runes instead of two and six new ones: Growth (a planted sapling may grow a stage at once), Pollen
  (leaves as with Fortune), Blades (a log may give one more), Photosynthesis (a felled tree may leave a golden apple),
  Hush (bee nests and hives on a felled tree come down whole, bees and honey inside) and Dam (a tree may cost no wind).
- Hex Furnace takes up to three runes (it alone on the altar) and keeps them when broken: Forge (each ore may give one
  more ingot), Radiance (more experience), Eternal Flame (a melt may burn no fuel), Bellows (the longer it works
  without a pause, the faster it melts, up to three times), Reforging (iron, gold and copper tools and armour melt
  into ingots, not a nugget) and Lens.
- The guide shows the runes of the reaper, the sawmill, the furnace and the three tools under their recipes, and JEI
  lists them with their numbers per star.

Chemtech begins: the cheap, dirty answer to hextech - chemicals instead of crystals, a green flame, rust.

- Crucible brews (researched at the Research Table): a catalyst thrown into boiling water that holds its essences
  turns the water into doses of a brew, one kind at a time; a glass bottle takes a dose as a Chem Vial. Five brews:
  Combustible Mix (coal into fire and ash), Growth Tonic (bone meal into plant and sap), Coolant (a snowball into
  cold and ice), Glow Mix (glowstone dust into light and slime, over soul fire at least) and Acid (a fermented spider
  eye into venom and decay, only over a Chem Burner). Hotter brews say what heat they need.
- Thrown vials splash: the growth tonic makes the plants around shoot up and young animals grow up, the coolant puts
  fire out and freezes lava into obsidian, the glow mix leaves patches of cold green Chem Glow on the blocks around,
  the acid eats away the stone around (ores stay), rusts iron blocks, weathers copper and burns monsters. Dispensers
  throw them too. A vial of combustible mix burns in any furnace as long as a blaze rod.
- Chem Burner (researched, made at the Hex Workbench): a riveted iron stove with a green window, the hottest heat
  there is. It heats a crucible, an alembic, a separator (which then burns no blaze powder) or a Hex Furnace (which
  then needs no fuel) standing on it, and burns only while that machine has work. It feeds on vials of combustible mix
  (by hand or hoppers) or on any essence through pipes at its back and sides - the separator's dark unknown waste
  burns twice as long. Up to three runes: Eternal Flame (fuel may last twice as long), Steam (a brew boiled over it may
  give one more dose), Radiance (brews give experience), Fireproofing (players beside it keep Fire Resistance) and Lens.
- Chem Glass (glass dropped into water holding venom) and its panes, Rusted Plating (iron blocks splashed with acid)
  with stairs, slabs and riveted plating.
- The guide's chemtech branch has the brews, the glass, the burner and the plating in place of their sketches; JEI
  lists the brews.

### Fixed
- The altar named the right refusal when a rune did not fit a reaper or a sawmill, or when they were full.

## [1.0.0-beta.1] - 2026-10-20
### Added

**Hextech**
- Unstable hextech: put a lightning rod on an amethyst block and wait for a storm (or call the lightning with a
  Channeling trident) - the strike shatters the amethyst into lumpy, humming orbs, and runs on as a crackling wave
  through every amethyst block and cluster touching it, up to five blocks away, ring after ring, sparks jumping from
  block to block (budding amethyst carries the spark and stays whole). Raw hextech is volatile: an orb that
  lands after a long fall or is caught in a blast bursts in a flash of sparks that hurts and pushes what stands
  close, leaving hextech dust. Orbs fresh from the storm are still settling and survive their first fall.
- Hextech Dust: throw an orb (right click, or from a dispenser) and it shatters into glittering dust where it lands; a
  creature it hits takes a small arcane sting.
- Three grades of hextech: raw orbs, Hextech Crystals cut on the lapidary (they hold a charge and never burst) and
  Hextech Gemstones finished from crystals with a master disc. Crystals power most devices and the first pickaxe,
  gemstones the better pickaxes.
- Hex Workbench: sprinkle hextech dust on a crafting table and it wakes - the dust swirls into it, sparks climb its
  sides and it turns into a dark oak table with a flat polished deepslate worktop carved with a 3 x 3 grid of
  glowing channels. Almost every device is crafted here; hextech put into its socket soaks in as charge (raw orb 5,
  cut crystal 12, gemstone 30, up to 60), shown in a glass tube, and each craft spends a little. The grid's items lie
  in the worktop's cells. Devices still to be researched show nothing. Every ordinary crafting table recipe works on it
  too, free of charge. With JEI, a click on the arrow shows the
  recipes (the Hex Furnace's too).
- Hex Furnace: dust on a furnace wakes it into a polished stone kiln between dark oak posts, a glowing channel over
  its arched mouth, like the workbench. It melts up to three metals into an alloy on ordinary fuel, blue tongues among its flames: three copper and a zinc make four
  brass; iron, two hextech dust and coal make Hexsteel (researched first). Anything else a furnace smelts it smelts
  too, up to three of the same item at once - three iron ore give three ingots in one furnace's time. It crackles
  like a furnace. Hoppers fill it like a furnace.
- Zinc: Sphalerite Ore in stone and deepslate (a little rarer than copper, from deep underground up to sea level),
  raw zinc, zinc ingots, nuggets and blocks (common `c:` tags, so other mods' zinc works too). Block of Hexsteel.
- Hex Lapidary: a dark oak bench with a treadle and a lap disc, crafted on the workbench; its cutting discs are made
  at the altar (iron and flint; the master disc of hexsteel and diamond). Cutting is a mini-game: the crystal hangs
  large over the turning disc - turn it (mouse wheel or A/D, it turns on a little and settles) until the contact line
  glows green (it warms to yellow as a plane nears, arrows point at it when it lines up), then hold to grind and watch the facet grind flat. Grinding across the hidden cleavage planes scrapes
  red and cracks the crystal, so does too much heat; every facet cut cleanly gives the next grade, too many cracks
  shatter it into dust. A first cut has three planes, a finish five with a narrow window (a master disc feels the
  planes from further off and cuts raw orbs twice as fast). Discs wear with every cut and crack.
- The guide grows a chemtech half: its steps are bronze medallions wreathed in vines with leaves, small flowers, dew
  and drifting pollen, hanging from their parents by vines with leaves and a creeping bud instead of brass lines. The
  deeper the chemtech branch goes, the more it wilts - leaves brown and fall, rust creeps over the bronze and smog
  rises. Steps where hextech and chemtech meet wear half a wreath.
- Crucible, the first vessel of chemtech: sprinkle hextech dust on a cauldron (a water cauldron keeps its water) and
  it wakes as a pot of plain blackened cast iron on four legs. Fill it with a water bucket or water bottles and heat it
  from below - fire, a campfire or magma, soul fire faster, lava fastest. The water warms, steams, simmers and comes
  to a rolling boil, bubbles breaking all over it. Only boiling water takes things in: essence poured from bottles and
  jars falls in a stream, blooms over the water and swirls into it, tinting it; items thrown over the pot are drawn
  in, plop into the water, sink and dissolve into their essences (two lapis lazuli give 6 mB of magic, 6 of gem and
  2 of water); cold water and items without essences are spat back out. An item that is a recipe's catalyst does not
  dissolve when the water holds what the recipe needs: it takes those essences and rises out as the result, even from
  cold water. Wearing researcher's goggles you see every essence in the water floating over the pot as its icon with
  its amount; an essence that grows swells and draws motes up from the water. It holds 1500 mB of water and essence;
  more brims over the rim, spilling water and essence alike, so water dilutes and essence crowds out water. The water
  never boils off by itself. Use it to see how it stands, an empty bucket takes the water back out, sneak-use boils it
  all away in a cloud of steam (essence counts as liquid too: a crucible boiled down to its essence still holds it).
  Crucible recipes are data (`data/<namespace>/hextale_crucible/`) and show in JEI.
- Magic Tree: research it at the Research Table, then throw any sapling into a crucible holding 8 mB of magic, 6 of
  plant and 4 of sap - it drinks them in and rises out as a Magic Tree Sprout (anyone who has not researched the tree
  only dissolves the sapling, and is told what to research). The sprout glimmers and grows like any sapling; when it
  shoots up, light runs up the new trunk and the crown sheds its first leaves. The tree: a slender dark violet trunk
  that may kink once, bark buttresses and roots biting into the ground, three or four long branches and a tiered
  umbrella of flat leaf pads with short hanging strands. Hextech-blue light glows in the cracks of its bark and in its
  canopy at night, glowing leaves drift down from it and motes of light rise out of it. Its wood never burns. A full
  wood set: log, wood, stripped log and wood (an axe bares the glowing heartwood), planks, stairs, slab, fence, fence
  gate, door and trapdoor with a hexagon window and a blue inlay, button, pressure plate, leaves (they drop the sprout
  now and then) and the sprout in a flower pot. Magic wood carries a little magic essence.
- Essences as items: each of the 80 essences is an item with its pictogram in its colour - in the creative tab and in
  JEI, where crucible recipes ask for them. Typing an essence's name in the creative search or in JEI also finds every
  item you have studied that carries it (unstudied items stay hidden). The creative tab keeps one jar of essence, the
  Creative Essence Jar.
- Hextech Lamp: a hextech crystal floating in a cage of polished magic stone and brass, full light. It stands on a block or hangs under
  one, like a lantern.

**Research**
- Research Table: a two-block dark oak writing desk with brass trim, drawers, a bookshelf, a book rest for the diary,
  an inkwell and a leather writing pad. Unstable hextech hangs over the parchment from a brass arm, turning inside
  spinning gimbal rings; glyphs stream from the parchment while someone writes, and the freshly researched device rises
  over the desk. The table keeps its own paper; every research takes one sheet when it starts.
- Researcher's Diary: a book signed with its owner's name, with two tabs. The Researcher's Path is a map of steps
  through the mod in a dark sky to drag and zoom. Workstations - the hex workbench and furnace, the crucible, the
  Research Table, the alembic, the lapidary and, largest of all, the hextech altar - stand out in brass cog rings, and
  what they make stands around them; the links curve gently and never cross. There is no single road: a step needs only what it really
  needs, so several branches open at once - you choose where to go. You see what is done and what you can do now, and
  one step further, greyed out as "not reachable yet" (its name and what it waits for show under the mouse); the rest
  stays in the fog. Devices still being worked out (marked WIP) gather in small rings around the step they will grow
  from, and some grow from each other to show where the mod is heading - above all chemtech, a whole branch of plans
  west of the crucible: brews and chem vials, the chem burner and boiler, glowbloom extract and shimmer, the lab bench
  and mutations, and the smog that follows. Steps researched at the Research Table are hexagons, like the sections of
  the research parchment, with a loupe on their corner: violet with the item sealed under a turning violet mist until
  the research is done, brass after. Such a step is done by researching it - finding or being given the item teaches
  nothing - and every step counts only once the steps before it are done. Only the
  few turning points of the path wear an effect: raw hextech crackles with lightning, the ink, the first rune scroll,
  the hextech crystal and the gemstone glow gold, the radiant pickaxe is circled by blue sparks. The guide's steps are data (`data/<namespace>/hextale_guide/*.json`): data packs can add, change or
  remove steps, their goals, pictures and places on the map. The diary opens where your next steps are. A
  click opens the step's window in the middle of the page (back arrow, Escape or a click beside it returns to the map):
  the recipe, ritual or structure for a thing and what it opens (on a table, a button that lays its research on the
  parchment, always in reach under the text), the idea behind a device to come. Finishing a step is marked by a quiet stroke of the pen. The Runes tab
  lists every rune found so far - sign, name, stars, what it does and what it is used for. The Hextech Altar is the heart of the
  path: a large medallion in a hexagonal brass setting with turning gimbal rings, alone in its own patch of sky. The
  plates of the altar, the pedestal and the rune stele show the ritual circle at work in a turning 3D scene - the items
  rise from the pedestals into the altar, the steles' runes beam in, the jars pour their essences, a flash, and the
  gemstone turns over the altar; the part the step is about stands in a ring of light.
- Essences: everything is made of them. Nine primal essences (fire, water, earth, air, light, shadow, life, metal,
  lightning) combine into 71 more, from Steam and Ore to Time and Soul; every essence has its own picture. Every item
  carries up to six essences in different amounts, from hand-tuned base materials to anything crafted from them; the
  table comes from a data map (`hextale:essences`), so data packs can change it.
- Item study, the loupe arm: a brass telescoping arm with three lenses is fixed to the Research Table. Hold it with
  the left mouse button on the desk and drag it onto any item in the slots: its lenses fold over one another (x2, x4,
  x8), a magnified view of the item opens with its essences surfacing around it, and a moment later the item is
  studied, with a soft sound. Let go and the arm swings back onto its rest. In the world the arm on the desk turns over
  the parchment and folds its lenses while someone looks through it. Free and unlimited. A studied item shows its
  essences in its tooltip; a studied item under the loupe shows them at once.
- Research: every device and tool past the essence works - the altar circle, the gemstone, the lamp, the big jar,
  the pylons, the pickaxes, the lapidary and its master disc, hexsteel, the drain valve, the pump, the
  researcher's goggles, the filter and the essence sticker - must be researched at the table before it can be crafted
  or made at the altar. Choose it in the book and draw it on a worn parchment with a hexagon of six essences - one per
  direction, chosen by the device's theme, shown as ink wells in the hexagon's corners. Drag ink from
  the hextech orb to every goal - the gold sigils, counted at the top of the sheet; the ink may branch from the orb
  or from any inked section. A step costs ink of the essence its direction points to, a step onto a goal ten of the
  goal's own essence; right click takes a step back for free. Every parchment is a minesweeper: dark fields the ink may cross hide
  curses, and every inked section next to them counts the curses it touches (right click flags a suspected section).
  Simple devices have a third of the sheet dark and a few stone walls, deeper ones more, the deepest are nearly all
  dark - but every sheet can be finished by reasoning from the counts, without a guess. Stepping on a curse bursts the orb: the parchment catches fire and chars away from the curse, and a fresh sheet
  slides in with a new board for the next attempt. The flame button in the sheet's corner burns it on purpose (two
  clicks) to start over on a new board - only the paper is lost. A finished drawing writes
  Research Notes; reading them teaches the device - and notes can be handed to another player. The notes keep the drawing: laid back into the table, they show it on the parchment again.
- Runes are found, not researched: rune scrolls lie in treasure chests - simple runes in dungeons, temples, mineshafts
  and buried treasure, harder ones in strongholds, mansions, the Nether and trial vaults, the rarest in ancient cities,
  end cities and bastion treasure. A scroll's stars (one to three) are luck. Studying a scroll teaches the rune.
- Blank Scroll (three paper): put it on the altar, choose a rune you know on one stele and start the ritual - the altar
  draws the rune's essence once per star and writes a scroll of that rune with your stars, to pass the rune on.
- Creative mode: the Scroll of Omniscience teaches everything (every rune with three stars, every research, every item,
  every quest),
  the Scroll of Oblivion forgets everything. Creative mode itself knows nothing and draws no free ink: what is not
  learned stays locked, in creative as in survival.

**Essence works**
- Essence is one mixed liquid measured in millibuckets, one millibucket per point of an item's essence (the number in
  its tooltip).
- Alembic: set over fire, a lit campfire, lava or magma, it boils items in water into essence - a mixture of all their
  essences. An item the player who put it in has not studied boils into as much dark unknown essence. Every item takes
  250 mB of water: pour a bucket in, or pipe it from a Pump standing in water - pipes bring water in and take essence
  out on any side. The flask shows the water, the bubbles and the item turning inside.
- Bottles of essence: plain glass bottles fill with 16 mB at the machines and jars.
- Separator: draws the essence chosen on its screen out of a mixture into a concentrate and lets the rest go on with
  dark unknown essence added. It burns blaze powder; each side takes the mixture in or lets the concentrate or the rest
  out, set on its screen or by sneak-using that side.
  It works off 4 mB every half second. The dark unknown essence can be chosen as well: it is filtered out into its own
  bottles, and the rest comes out clean (no new unknown essence on top) - a mixture dirtied by unstudied items can be
  cleaned into a concentrate again.
- Essence Jar: a big glass jar with a wooden lid (glass and wooden slabs, no brass), 256 mB; in through every face - the bottom too - and out at the bottom. Sneak-use
  a side or the lid with an empty hand to stick an enamel label with an essence's sign on it. The Research Table draws
  its ink from jars of concentrate within 5 blocks - every drop it spends streams from its jar into the orb. A Creative Essence Jar holds every essence without end.
  A jar breaks quickly by hand and keeps its essence and its sticker: the item shows the liquid and the sticker inside
  its glass, and placed again it holds the same. A right click nudges the jar - it rocks on its foot and settles.
- Big Essence Jar, the first structure built in the world: a 3 x 3 floor of glass, two rings of glass around a hollow
  middle and a lid of any wooden slabs, woken by a right click with unstable hextech or a hextech gemstone (not used
  up). It looks like the Essence Jar four times as large and holds sixteen jars' worth (4096 mB): bottles by hand, pipes
  join it anywhere (machines draw through them, a Siphon Pylon can stand on its lid), it pours out under its middle,
  machines draw from it like from any jar. Breaking any of its blocks
  takes it apart into its glass and slabs; the essence inside is lost and no jar drops. The ground under it stays lit. The guide shows the structure layer by layer.
- Brass Pipes - slim tubes with small flanges - carry essence and water and join a machine only where it has a port;
  the machine's model grows a matching joint there. Sneak-use a pipe's side with an empty hand to set it: next to
  another pipe it shuts the joint (both pipes show capped stubs) or opens it again; next to a machine or jar it cycles
  both ways -> in only (green collar) -> out only (blue collar; a jar on that side is pumped out into the pipes) -> shut.
  The goggles and the pipe's tooltip tell what the side you look at does. The Drain Valve pours whatever reaches it out into the world while
  its handwheel is open; essence it pours onto stone soaks in and turns it into magic stone.
- Researcher's Goggles: worn on the head, they show the essences inside a jar, pipe or machine you look at; looking at a
  Research Table shows its ink while coloured streams run from each jar to the orb.
- Liquid essence swirls with an animated texture, glows at its surface, darkens in the depths and bubbles. The
  alembic's flask and the separator's vessel are round glass with framed panes, like the jar.

**Rune works**
- Hextech Altar circle: the altar, Pedestals within six blocks, Rune Steles within seven and Essence Jars within eight.
  With an item on the altar, a stele lists only the runes that do something for it (the runes a pickaxe or a
  pylon takes, and those its rituals ask for) in a tidy, scrollable list: each with its name, your stars, what it does
  on that item, its number and the essence it draws at your stars (a tooltip shows all three star levels with their essence, what it cannot share an item with
  and, for a ritual, the essence the whole ritual draws). The circle stays sensible: a rune stands on one stele only,
  an item never gets more runes than its slots (the header counts them, e.g. 2/3 on a pylon; raising the stars
  of a rune it already has takes no slot) and never two incompatible runes - such runes are dimmed with the reason; an empty altar shows the whole codex.
  Sneak-use the altar with an empty hand (or give it a redstone pulse) to start a ritual: the rings turn around the
  centre item, the pedestal items are drawn in one by one, then the essences one after another, while shards of each
  stele's hieroglyph peel off and wind in a corkscrew into the work, white-hot, cooling into the rune's colour, and
  the centre becomes something new. The runes go into the work: after a ritual every stele of the circle goes dark
  and waits for a rune to be chosen again. A ritual short of essence stalls and, left too long, collapses. Ghosts of the items a ritual
  still lacks hover over the free pedestals.
- Block of Brass.
- Hextech pickaxes: the Hextech Pickaxe (iron strength), the Refined Hextech Pickaxe (diamond) and the Radiant Hextech
  Pickaxe (netherite, fireproof), each made at the altar from the one before - runes and enchantments carry over. Their
  crystal glows in the colour of the runes they carry.
- Runes on a pickaxe, like enchantments: put the pickaxe alone on the altar, choose runes on rune steles and start the
  ritual - each rune is burned in, paid only with its own essence (more for more stars and for harder runes). Up to
  four runes on any pickaxe; thirteen runes of the codex take to a pickaxe, and a rune is as strong as its stars. A
  stele with more stars raises a rune the pickaxe already has. Some runes cannot share a pickaxe, and only one of them
  may answer the right click. A grindstone takes the runes off again.
- The guide shows a turning 3D altar burning the chosen rune into a pickaxe or the smelter; pick any rune they take to
  see what it does, how strong it is at one, two and three stars, what it costs and which runes it is incompatible
  with. JEI shows the same for every rune, next to every pickaxe recipe.
- Magic stone: stone that has drunk magic - flat plates of a dusky violet stone with nothing set into it. Throw two lapis
  lazuli into a boiling crucible, then stone, cobblestone, deepslate, tuff, andesite, diorite, granite, calcite or
  smooth stone: each block sinks, takes 6 mB of the magic, and rises out as magic stone in a burst of violet light. An open
  Drain Valve over such stone soaks it too: the essence it pours soaks in where the stream lands, one block per 16 mB. Polished magic stone, magic bricks with stairs, slab and wall, and chiseled magic bricks with the
  circle's hexagon cut into them: the stone of the ritual circle.

**Item transport: hex pylons**
- Instead of a clumsy mechanical arm, the pull of hextech: four pylons made at the altar, each its own shape - a low
  brass bowl that catches, a flat plate with claws that throws, a calcite and copper column with an amethyst crystal
  that passes on, a glass bulb that pours essence.
- Gathering Pylon: set on a chest, a barrel, a furnace or any container, it reaches for the items lying around (6
  blocks, more with runes) and lowers them into what it stands on - a furnace gets ore through its top. With nothing
  under it, it sends them along its links.
- Sending Pylon: stands on a storage and sends what it gives out (a furnace's result, a chest's anything) to the places
  you link. Right click the pylon, then right click a storage: a chest or a barrel simply fills, a furnace (or a blast
  furnace, a smoker) sorts what comes - what it can cook into the input, fuel into the fuel slot; a gathering pylon on a
  furnace does the same. Up to four links, tried in order; the same clicks again unlink one, sneak + right click with an
  empty hand unlinks all. Clicking the air or a block that cannot take items stops linking and does what it always does;
  with a block in hand a right click on a pylon places the block. A broken block drops its links by itself, and sneak +
  right click with an empty hand on a linked block unties it from every pylon. The item tooltip tells how.
- Pylons belong to whoever placed them and deliver on their behalf: they feed the Alembic, the Separator, the Hex
  Furnace, the Research Table and Pedestals like a hopper would, and the Alembic boils what they bring into its own
  essences only if the pylon's owner (online) has studied it. The goggles show a pylon's owner.
- A pylon with several links takes them in turn: each move goes to the next link that takes something, so every
  linked chest, machine or jar gets its share.
- A Sending Pylon waits until its target has room for a whole batch (a furnace with 62 ore gets nothing until 8 fit),
  and what is already flying to a target counts against its room, so no extra items fly there only to come back.
- Relay Pylon: neither takes nor gives - it catches items and essence sent to it and passes them on to its own links,
  up to eight relays in a chain. Made of calcite, copper ingots, amethyst and a lightning rod.
- Siphon Pylon: set on an Essence Jar, it draws the essence up into its glass bulb (swirling in the jar's colour) and
  sends it as glowing motes arcing along its links, like the essence drawn into the altar - into other jars, a separator, anything that takes
  essence from pipes; relays pass the stream on.
- Filter, made at the Hextech Altar (a brass ingot with string, brass nuggets and an amethyst shard): right click it to set it up - nine slots (click with an item to put it in), "only these" or "all but
  these", matched by kind or exactly (enchantments, contents), and numbers: keep N of each item in the source, or fill
  a target only up to N (scroll over a slot to set N). Right click a pylon with it to set it in (the one there comes
  back), sneak + right click to take it out. On a gathering pylon it picks what to gather, on a sending pylon what to
  send, on a relay what may pass - a few relays with filters sort a storeroom. The filter's items circle the pylon's heart.
- Essence Sticker, made at the Hextech Altar (paper, slime and an amethyst shard, six at a time): right click to pick its essence from all of them. On an Essence Jar the jar takes only that essence
  (from pipes, bottles and siphons alike); sneak + right click with an empty hand peels it off. On a siphon or a relay
  it lets only that essence through, pure. Pylons never split a mixture - it always travels whole; separating essences
  stays the separator's work.
- Sorting Matrix, made at the Hextech Altar (a hextech crystal, an observer, a comparator, glass and brass): a lattice
  of eight lenses turning on its corner around a hextech gem inside a brass frame. Set it on a chest - the dump chest -
  and it reads what lies there and sends every item home, up to eight kinds at a time: to any chest, barrel or shulker
  box within 6 blocks that already holds that item (want a chest for cobblestone? put one cobblestone in it), or along
  its links - through relays too, and a relay whose Filter names an item makes the chest behind it that item's home.
  Linked furnaces get the ores and food nobody keeps, and more of the fuel they already burn. What has no home stays
  in the dump chest; a chest that carries a pylon is never filled. Linking another pylon to the matrix sends into its
  chest, so gathering and sending pylons feed it. It holds eight links; Shift widens its reach, Motor quickens it,
  Lift moves more at once. With the researcher's goggles, the storages it sorts into show a turning ring on their lids.
- Pylon runes, burned in at the altar like a smelter's: Shift (reach and link range), Motor (pace), Lift (items per
  move), Lens. A broken pylon keeps its runes.
- The magic fetches every item: a tendril of light, two strands twisting around a swaying line, reaches out of the gem,
  closes a turning sigil around the item, draws back and drags it in, swinging a little; the gem flares as it drinks the
  item in, and on to the next gem or container another tendril stretches out and pulls it along. Several items get
  several tendrils, one after another. Links show as gold threads while you sneak or hold a pylon; while linking, a
  thread runs to your crosshair. Soft whooshes for pulling and sending, a low touch for landing.
- Researcher's Goggles: every link around shows while worn; looking at a pylon shows its reach, pace, link range, its
  links (where to, how far) and runes, a gathering pylon's reach lights up as a ring on the ground; looking at a linked
  block shows which pylons send to it.
- Decorative: Magic Stone Pillar (like a quartz pillar) and Brass Grate (see-through, like a copper grate).
- Built to be cheap: items move straight between inventories in one transaction (only the fetching is drawn), idle
  pylons look four times rarer, and the clients get one small message per move.

**Farming: the Whirl Reaper**
- Whirl Reaper: a brass mast on a foot of whorled magic stone, three framed sails turning round it and a hextech
  crystal floating on top in a little whirl of its own. Researched at the Research Table and made at the altar
  (hextech crystal, feathers, an iron hoe, brass, polished magic stone; wind, plant and crystal essence). Set among
  the beds, it sends a whirl of wind to the nearest ripe plant within 4 blocks (3 up and down): the whirl leaves the
  crystal, flies over to the plant and stands on it as a turning funnel of light, crumbs of the plant swirling up it.
  - Wheat, carrots, potatoes, beetroot (and modded crops), nether wart, cocoa and sweet berries are reaped where
    they stand: they start again from a sprout, one seed stays in the ground, the harvest falls on their own block.
  - A melon or pumpkin on its stem is lifted off into the whirl, spun and broken open where it lay.
  - Sugar cane, cactus and bamboo three blocks tall are twisted off above the root; the pieces rise turning round
    the funnel and drop beside the root (a cactus would burn them on top of itself).
  - Every whirl drinks wind essence from a jar within 5 blocks (1 mB for four whirls; feathers boil into wind); the
    jar's stream runs into the crystal. Without wind it stands becalmed, its sails barely turning.
  - A Gathering Pylon nearby picks up the harvest. Runes at the altar: Shift (wider field), Motor (whirls more often),
    Lens; a broken reaper keeps them. Right click shows its field, pace and wind; Jade too.
  - Light on the server: it reads its field block by block only when it looks, and an idle reaper looks more and
    more rarely; the clients play each whirl from one small message.
- Whorled Magic Stone: polished magic stone with a carved spiral (stonecutter).
- Hex Sawmill: a dark oak stump with a hextech saw disc of blued steel turning in a slot under a brass guard, a
  crystal floating over it. Researched at the Research Table, made at the altar (hextech crystal, iron axe, dark oak
  logs, an oak sapling, brass; wind, wood and metal essence). It throws a glowing copy of its disc at the nearest grown
  tree within 6 blocks (3 up and down): the disc flies over, saws through the trunk in sawdust and sparks, and the whole
  tree tips over away from the mill, slowly and then faster, and breaks apart where it hits the ground. Every log and
  every leaf of it is taken; the drops (logs, saplings, sticks, apples) land on its stump and a sapling from them is
  planted where it stood - four on the four feet of a big tree when four dropped.
  - Only trees grown from the ground are felled: logs on dirt with grown leaves. Log walls, pillars and trees whose
    leaves were placed by hand are never touched; roots under the ground stay.
  - Each tree takes 2 mB of wind essence from a jar within 5 blocks (the jar's stream runs into the crystal); without
    wind it stands becalmed. Runes at the altar: Shift (farther trees), Motor (saws more often), Lens; a broken sawmill
    keeps them. Right click shows its grove, pace and wind; Jade too. Sounds: the disc whirring off, the bite into
    wood, the trunk creaking over, the crash and the rustle of leaves.

**Sound and integrations**
- The Researcher's Diary opens with the sound of its leather cover and turning pages.
- Synthesized sounds: the alembic simmers, the separator drips, the pump strokes, the valve's handwheel turns, jars
  glug, ink drips with every step; rituals hum, draw and finish with layered drones, whooshes and soft impacts.
- [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) shows boiling, separation, the altar's infusions and an info
  page for every block of the essence works and the ritual circle;
  [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) shows what the jars, machines, pumps and valves hold and do.

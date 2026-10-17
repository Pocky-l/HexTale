# Changelog

All notable changes to this mod are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - Unreleased

### Added

**Hextech**
- Unstable hextech: put a lightning rod on an amethyst block and wait for a storm (or call the lightning with a
  Channeling trident) - the strike shatters the amethyst into lumpy, humming orbs. Raw hextech is volatile: an orb that
  lands after a long fall or is caught in a blast bursts in a flash of sparks that hurts and pushes what stands
  close. Orbs fresh from the storm are still settling and survive their first fall.
- Hextech Gemstone: unstable hextech refined at the Hextech Altar into a smooth, steady sphere. It no longer bursts,
  and every hextech pickaxe and device is built around one.
- Hextech Lamp: a gemstone floating in a cage of polished magic stone and brass, full light. It stands on a block or hangs under
  one, like a lantern.

**Research**
- Research Table: a two-block dark oak writing desk with brass trim, drawers, a bookshelf, a book rest for the diary,
  an inkwell and a leather writing pad. Unstable hextech hangs over the parchment from a brass arm, turning inside
  spinning gimbal rings; glyphs stream from the parchment while someone writes, and a freshly drawn rune rises over the
  desk as a glowing hieroglyph. The table keeps its own paper; every research takes one sheet when it starts.
- Researcher's Diary: a book signed with its owner's name, holding the Researcher's Guide - one branching path through
  the mod in a dark sky to drag and zoom. Things to make, runes to study and devices still being worked out (marked WIP)
  share the path in the order they can really be made; a step opens when the steps before it are done, and only what
  is done or open can be seen - the rest of the path fades into the fog. Key steps glow gold, storm steps crackle with
  lightning, hextech steps are circled by blue sparks, cursed runes smoke, heat steps shed embers. A click opens the
  step's plate: the recipe or ritual for a thing, what a rune does, what it is used for and what it opens (and a button
  that lays it on the Research Table), the idea behind a device to come.
- Essences: everything is made of them. Nine primal essences (fire, water, earth, air, light, shadow, life, metal,
  lightning) combine into 71 more, from Steam and Ore to Time and Soul; every essence has its own picture. Every item
  carries up to six essences in different amounts, from hand-tuned base materials to anything crafted from them; the
  table comes from a data map (`hextale:essences`), so data packs can change it.
- Item study, the prism: put an item into the study slot and find where its essences hide in a misty hexagonal field.
  Shine rays in from the brass rim - a ray is absorbed by an essence ahead, bent away by one beside its path, turned back
  by two - and read where it leaves; check a section to look inside. Rays and checks share a small budget. A studied
  item shows its essences in its tooltip.
- Rune research: choose a rune the guide has opened and draw it on a worn parchment with a hexagon of six
  essences - one per direction, chosen by the rune's theme. Drag ink from the hextech orb to every point of the rune;
  the ink may branch from the orb or from any inked section. A step costs ink of the essence its direction points to,
  a step onto a point ten of the point's own essence; right click takes a step back for free. A shorter drawing earns
  up to three stars. In chapter I the dark runes are stone; from chapter II they are dark fields the ink may cross,
  but some of their sections are cursed - every inked section next to them counts the curses it touches, like a
  minesweeper. Stepping on a curse bursts the orb and burns the sheet. A finished drawing writes the rune's scroll;
  studying the scroll teaches the rune to the researcher with the scroll's stars. Smelting comes first; every rune
  opens the runes and devices after it on the guide.
- Creative mode: the Scroll of Omniscience teaches everything (every rune with three stars, every item, every quest),
  the Scroll of Oblivion forgets everything.

**Essence works**
- Essence is one mixed liquid measured in millibuckets, one millibucket per point of an item's essence (the number in
  its tooltip).
- Alembic: set over fire, a lit campfire, lava or magma, it boils items in water into essence - a mixture of all their
  essences. An item the player who put it in has not studied boils into as much dark unknown essence. Every item takes
  250 mB of water: pour a bucket in, or pipe it from a Pump standing in water. The flask shows the water, the bubbles and
  the item turning inside.
- Bottles of essence: plain glass bottles fill with 16 mB at the machines and jars.
- Separator: draws the essence chosen on its screen out of a mixture into a concentrate and lets the rest go on with
  dark unknown essence added. It burns blaze powder; each side takes the mixture in or lets the concentrate or the rest
  out, set on its screen or by sneak-using that side.
- Essence Jar: a big glass jar with a wooden lid, 256 mB; in through the lid and the sides, out at the bottom. Sneak-use
  a side or the lid with an empty hand to stick an enamel label with an essence's sign on it. The Research Table draws
  its ink from jars of concentrate within 5 blocks - every drop it spends streams from its jar into the orb. A Creative Essence Jar holds every essence without end.
- Brass Pipes - slim tubes with small flanges - carry essence and water and join a machine only where it has a port;
  the machine's model grows a matching joint there. The Drain Valve pours whatever reaches it out into the world while
  its handwheel is open; essence it pours onto stone soaks in and turns it into magic stone.
- Researcher's Goggles: worn on the head, they show the essences inside a jar, pipe or machine you look at; looking at a
  Research Table shows its ink while coloured streams run from each jar to the orb.
- Liquid essence swirls with an animated texture, glows at its surface, darkens in the depths and bubbles. The
  alembic's flask and the separator's vessel are round glass with framed panes, like the jar.

**Rune works**
- Hextech Altar circle: the altar, Pedestals within six blocks, Rune Steles within seven and Essence Jars within eight.
  Sneak-use the altar with an empty hand (or give it a redstone pulse) to start a ritual: the rings turn around the
  centre item, the pedestal items are drawn in one by one, then the essences one after another, and the centre
  becomes something new. A ritual short of essence stalls and, left too long, collapses. Ghosts of the items a ritual
  still lacks hover over the free pedestals.
- Hex Smelter: a kiln of magic bricks with brass posts and a hextech gem floating in its crown, made at the altar from
  a blast furnace under the rune of Smelting. It smelts anything a furnace smelts, twice as fast, and burns fiery
  essence instead of fuel. While it has something to smelt it draws the essence by itself from the nearest jar of fiery
  essence within 5 blocks - a braid of light arches out of the jar's lid, waves and twinkling sparks run along it,
  hex sigils turn on the jar and around the gem, wisps of essence drift off, and the gem swells as it pours in, with a
  soft siphoning sound; pipes at the
  back and the sides and bottles work too. The more fire an essence holds, the less of it goes. Its mouth burns with a hextech-blue flame while it works. Runes are burned into it at the altar
  like into a pickaxe, up to three: Tempering saves essence, Bellows speeds it up, Forge adds ingots from ore, Lens
  strengthens the others; the gem glows in their colours and the smelter keeps them when broken. JEI, Jade and the
  goggles show it.
- Block of Brass.
- Hextech pickaxes: the Hextech Pickaxe (iron strength), forged under the rune of Smelting, the Refined Hextech Pickaxe (diamond) and the Radiant Hextech
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
- Magic stone: stone that has drunk essence - flat plates of a dusky violet stone with nothing set into it. Craft eight stone,
  cobblestone, deepslate, tuff, andesite, diorite, granite or calcite around a bottle of any essence (the bottle comes
  back), or open a Drain Valve over such stone: the essence it pours soaks into the stone where the stream lands, one
  block per 16 mB. Polished magic stone, magic bricks with stairs, slab and wall, and chiseled magic bricks with the
  circle's hexagon cut into them: the stone of the ritual circle.

**Item transport: hex pylons**
- Instead of a clumsy mechanical arm, the pull of a hextech gem: three pylons, columns of fluted magic stone crowned
  with brass and a floating gemstone, made at the altar.
- Gathering Pylon: set on a chest, a barrel, a furnace or any container, it lifts the items lying around (3 blocks, more
  with runes) into its gem and lowers them into what it stands on - a furnace gets ore through its top. With nothing
  under it, it sends them along its links.
- Sending Pylon: stands on a storage and throws what it gives out (a furnace's result, a chest's anything) to the places
  you link. Sneak + right click the pylon with an empty hand, then sneak + right click a storage's face: the face counts
  (a furnace takes ore from the top, fuel from the side). Up to four links, tried in order; the same click again unlinks.
- Relay Pylon: neither takes nor gives - it catches items thrown to it and passes them on to its own links, up to eight
  relays in a chain. A relay with filters lets only those items through, so a few relays sort a storeroom.
- Filters: right click a pylon with an item to make it a filter (up to four, circling the gem); with an empty hand to
  take one off, or to hear what the pylon does now.
- Pylon runes, burned in at the altar like a smelter's: Shift (reach and link range), Motor (pace), Lift (items per
  move), Lens. A broken pylon keeps its runes.
- Moved items fly as glowing items with a trail of motes along arcs from gem to gem, a ring of light opens where a
  gathered item lay and a flash marks where it lands; the gems spin up with each move, a gathering pylon's ring snaps
  in as it pulls. Links show as gold threads while you sneak or hold a pylon; while linking, a thread runs to your
  crosshair. Soft whooshes for pulling and throwing, a low touch for landing.
- Decorative: Magic Stone Pillar (like a quartz pillar) and Brass Grate (see-through, like a copper grate).
- Built to be cheap: items move straight between inventories in one transaction (only the flight is drawn), idle
  pylons look four times rarer, and the clients get one small message per move.

**Sound and integrations**
- Synthesized sounds: the alembic simmers, the separator drips, the pump strokes, the valve's handwheel turns, jars
  glug, ink drips with every step; rituals hum, draw and finish with layered drones, whooshes and soft impacts.
- [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) shows boiling, separation, the altar's infusions and an info
  page for every block of the essence works and the ritual circle;
  [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) shows what the jars, machines, pumps and valves hold and do.

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
  spinning gimbal rings; glyphs stream from the parchment while someone writes, and the freshly researched device rises
  over the desk. The table keeps its own paper; every research takes one sheet when it starts.
- Researcher's Diary: a book signed with its owner's name, with two tabs. The Researcher's Path is a tree of steps
  through the mod in a dark sky to drag and zoom: every theme - the workshop, the essence works, the rune lore, the
  hextech circle, the pylons and the forge - grows as its own branch in its own direction, in a haze of its colour.
  There is no single road: a step needs only what it really needs, so brass and raw hextech both start the journey and
  several branches open at once - you choose where to go. Devices still being worked out (marked WIP) gather in small
  fans around the step they will grow from; only what is done or open can be seen - the rest fades into the fog. Key
  steps glow gold, storm steps crackle with lightning, hextech steps are circled by blue sparks, cursed ones smoke,
  heat steps shed embers; an ink drop marks what must be researched first. The diary opens where your next steps are. A
  click opens the step's window in the middle of the page (back arrow, Escape or a click beside it returns to the map):
  the recipe, ritual or structure for a thing and what it opens (on a table, a button that lays its research on the
  parchment), the idea behind a device to come. Finishing a step is marked by a quiet stroke of the pen. The Runes tab
  lists every rune found so far - sign, name, stars, what it does and what it is used for. The Hextech Altar is the heart of the
  path: a large medallion in a hexagonal brass setting with turning gimbal rings, alone in its own patch of sky. The
  plates of the altar, the pedestal and the rune stele show the ritual circle at work in a turning 3D scene - the items
  rise from the pedestals into the altar, the steles' runes beam in, the jars pour their essences, a flash, and the
  gemstone turns over the altar; the part the step is about stands in a ring of light.
- Essences: everything is made of them. Nine primal essences (fire, water, earth, air, light, shadow, life, metal,
  lightning) combine into 71 more, from Steam and Ore to Time and Soul; every essence has its own picture. Every item
  carries up to six essences in different amounts, from hand-tuned base materials to anything crafted from them; the
  table comes from a data map (`hextale:essences`), so data packs can change it.
- Item study, the prism: put an item into the study slot and find where its essences hide in a misty hexagonal field.
  Shine rays in from the brass rim - a ray is absorbed by an essence ahead, bent away by one beside its path, turned back
  by two - and read where it leaves; check a section to look inside. Rays and checks share a small budget. A studied
  item shows its essences in its tooltip.
- Research: every device and tool past the essence works - the altar circle, the gemstone, the lamp, the big jar,
  the pylons, the pickaxes, the Hex Smelter, the drain valve - must be researched at the table before it can be crafted
  or made at the altar. Choose it in the book and draw it on a worn parchment with a hexagon of six essences - one per
  direction, chosen by the device's theme. Drag ink from the hextech orb to every point; the ink may branch from the orb
  or from any inked section. A step costs ink of the essence its direction points to, a step onto a point ten of the
  point's own essence; right click takes a step back for free. Simple devices have stone walls; deeper ones have dark
  fields the ink may cross, but some of their sections are cursed - every inked section next to them counts the curses
  it touches, like a minesweeper. Stepping on a curse bursts the orb and burns the sheet. A finished drawing writes
  Research Notes; reading them teaches the device - and notes can be handed to another player.
- Runes are found, not researched: rune scrolls lie in treasure chests - simple runes in dungeons, temples, mineshafts
  and buried treasure, harder ones in strongholds, mansions, the Nether and trial vaults, the rarest in ancient cities,
  end cities and bastion treasure. A scroll's stars (one to three) are luck. Studying a scroll teaches the rune.
- Blank Scroll (three paper): put it on the altar, choose a rune you know on one stele and start the ritual - the altar
  draws the rune's essence once per star and writes a scroll of that rune with your stars, to pass the rune on.
- Creative mode: the Scroll of Omniscience teaches everything (every rune with three stars, every research, every item,
  every quest),
  the Scroll of Oblivion forgets everything.

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
  A jar breaks quickly by hand and keeps its essence and its sigil: the item shows the liquid and the sticker inside
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
  With an item on the altar, a stele lists only the runes that do something for it (the runes a pickaxe, smelter or
  pylon takes, and those its rituals ask for) in a tidy, scrollable list: each with its name, your stars, what it does
  on that item, its number and the essence it draws at your stars (a tooltip shows all three star levels with their essence, what it cannot share an item with
  and, for a ritual, the essence the whole ritual draws). The circle stays sensible: a rune stands on one stele only,
  an item never gets more runes than its slots (the header counts them, e.g. 2/3 on the Hex Smelter; raising the stars
  of a rune it already has takes no slot) and never two incompatible runes - such runes are dimmed with the reason; an empty altar shows the whole codex.
  Sneak-use the altar with an empty hand (or give it a redstone pulse) to start a ritual: the rings turn around the
  centre item, the pedestal items are drawn in one by one, then the essences one after another, and the centre
  becomes something new. A ritual short of essence stalls and, left too long, collapses. Ghosts of the items a ritual
  still lacks hover over the free pedestals.
- Hex Smelter: a kiln of magic bricks with brass posts and a hextech gem floating in its crown, made at the altar from
  a blast furnace under the rune of Smelting (find its scroll first). It smelts anything a furnace smelts, twice as fast, and burns fiery
  essence instead of fuel. While it has something to smelt it draws the essence by itself from the nearest jar of fiery
  essence within 5 blocks - a braid of light arches out of the jar's lid, waves and twinkling sparks run along it,
  hex sigils turn on the jar and around the gem, wisps of essence drift off, and the gem swells as it pours in, with a
  soft siphoning sound; pipes at the
  back and the sides and bottles work too. The more fire an essence holds, the less of it goes. Its mouth burns with a hextech-blue flame while it works. Runes are burned into it at the altar
  like into a pickaxe, up to three: Tempering saves essence, Bellows speeds it up, Forge adds ingots from ore, Lens
  strengthens the others; the gem glows in their colours and the smelter keeps them when broken. JEI, Jade and the
  goggles show it.
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
- Magic stone: stone that has drunk essence - flat plates of a dusky violet stone with nothing set into it. Craft eight stone,
  cobblestone, deepslate, tuff, andesite, diorite, granite or calcite around a bottle of any essence (the bottle comes
  back), or open a Drain Valve over such stone: the essence it pours soaks into the stone where the stream lands, one
  block per 16 mB. Polished magic stone, magic bricks with stairs, slab and wall, and chiseled magic bricks with the
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
  Smelter, the Research Table and Pedestals like a hopper would, and the Alembic boils what they bring into its own
  essences only if the pylon's owner (online) has studied it. The goggles show a pylon's owner.
- A pylon with several links takes them in turn: each move goes to the next link that takes something, so every
  linked chest, machine or jar gets its share.
- A Sending Pylon waits until its target has room for a whole batch (a furnace with 62 ore gets nothing until 8 fit),
  and what is already flying to a target counts against its room, so no extra items fly there only to come back.
- Relay Pylon: neither takes nor gives - it catches items and essence sent to it and passes them on to its own links,
  up to eight relays in a chain. Made of calcite, copper ingots, amethyst and a lightning rod.
- Siphon Pylon: set on an Essence Jar, it draws the essence up into its glass bulb (swirling in the jar's colour) and
  sends it as glowing motes arcing along its links, like the essence drawn into the altar - into other jars, a separator, the Hex Smelter, anything that takes
  essence from pipes; relays pass the stream on.
- Transfer Sigil, written at the Hextech Altar (a brass ingot with string, brass nuggets and an amethyst shard): right click it to set it up - nine slots (click with an item to put it in), "only these" or "all but
  these", matched by kind or exactly (enchantments, contents), and numbers: keep N of each item in the source, or fill
  a target only up to N (scroll over a slot to set N). Right click a pylon with it to set it in (the one there comes
  back), sneak + right click to take it out. On a gathering pylon it picks what to gather, on a sending pylon what to
  send, on a relay what may pass - a few relays with sigils sort a storeroom. The sigil's items circle the pylon's heart.
- Essence Sigil, written at the Hextech Altar (paper, slime and an amethyst shard, six at a time): right click to pick its essence from all of them. On an Essence Jar the jar takes only that essence
  (from pipes, bottles and siphons alike); sneak + right click with an empty hand peels it off. On a siphon or a relay
  it lets only that essence through, pure. Pylons never split a mixture - it always travels whole; separating essences
  stays the separator's work.
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

**Sound and integrations**
- Synthesized sounds: the alembic simmers, the separator drips, the pump strokes, the valve's handwheel turns, jars
  glug, ink drips with every step; rituals hum, draw and finish with layered drones, whooshes and soft impacts.
- [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) shows boiling, separation, the altar's infusions and an info
  page for every block of the essence works and the ritual circle;
  [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) shows what the jars, machines, pumps and valves hold and do.

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
  and every hextech tool and device is built around one.
- Hextech Lamp: a gemstone floating in a cage of polished magic stone and brass, full light. It stands on a block or hangs under
  one, like a lantern.

**Research**
- Research Table: a two-block dark oak writing desk with brass trim, drawers, a bookshelf, a book rest for the diary,
  an inkwell and a leather writing pad. Unstable hextech hangs over the parchment from a brass arm, turning inside
  spinning gimbal rings; glyphs stream from the parchment while someone writes, and a freshly drawn rune rises over the
  desk as a glowing hieroglyph. The table keeps its own paper; every research takes one sheet when it starts.
- Researcher's Diary: a star book signed with its owner's name. Its first page is the Researcher's Path - quests from
  the Research Table to the full codex, each with its goal, its recipe and a short note, done the moment the goal is
  met. Its second page is a dark sky to drag and zoom, where the 72 runes hang as stars in constellations. Leather
  bookmarks turn between the two.
- Essences: everything is made of them. Nine primal essences (fire, water, earth, air, light, shadow, life, metal,
  lightning) combine into 71 more, from Steam and Ore to Time and Soul; every essence has its own picture. Every item
  carries up to six essences in different amounts, from hand-tuned base materials to anything crafted from them; the
  table comes from a data map (`hextale:essences`), so data packs can change it.
- Item study, the prism: put an item into the study slot and find where its essences hide in a misty hexagonal field.
  Shine rays in from the brass rim - a ray is absorbed by an essence ahead, bent away by one beside its path, turned back
  by two - and read where it leaves; check a section to look inside. Rays and checks share a small budget. A studied
  item shows its essences in its tooltip.
- Rune research: choose one of the 72 runes in the star book and draw it on a worn parchment with a hexagon of six
  essences - one per direction, chosen by the rune's theme. Drag ink from the hextech orb to every point of the rune;
  the ink may branch from the orb or from any inked section. A step costs ink of the essence its direction points to,
  a step onto a point ten of the point's own essence; right click takes a step back for free. A shorter drawing earns
  up to three stars. In chapter I the dark runes are stone; from chapter II they are dark fields the ink may cross,
  but some of their sections are cursed - every inked section next to them counts the curses it touches, like a
  minesweeper. Stepping on a curse bursts the orb and burns the sheet. A finished drawing writes the rune's scroll;
  studying the scroll teaches the rune to the researcher with the scroll's stars.

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
  its ink from jars of concentrate within two blocks. A Creative Essence Jar holds every essence without end.
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
- Hextech Pickaxe: infused from an iron pickaxe; it keeps its enchantments and takes up to four runes. A rune chosen on
  a stele is burned into the tool as it is made, or into a finished tool later; flint grinds runes off again. Every
  one of the 72 runes does something on a tool, and runes cost nothing to fire. Runes have no descriptions - try them.
- Magic stone: stone that has drunk essence - a cool blue-violet stone with nothing set into it. Craft eight stone,
  cobblestone, deepslate, tuff, andesite, diorite, granite or calcite around a bottle of any essence (the bottle comes
  back), or open a Drain Valve over such stone: the essence it pours soaks into the stone where the stream lands, one
  block per 16 mB. Polished magic stone, magic bricks with stairs, slab and wall, and chiseled magic bricks with the
  circle's hexagon cut into them: the stone of the ritual circle.

**Sound and integrations**
- Synthesized sounds: the alembic simmers, the separator drips, the pump strokes, the valve's handwheel turns, jars
  glug, ink drips with every step; rituals hum, draw and finish with layered drones, whooshes and soft impacts.
- [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) shows boiling, separation, the altar's infusions and an info
  page for every block of the essence works and the ritual circle;
  [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) shows what the jars, machines, pumps and valves hold and do.

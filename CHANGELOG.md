# Changelog

All notable changes to this mod are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - Unreleased

### Added
- Research Table: a two-block dark oak writing desk with brass trim, a drawer cabinet, a bookshelf, a book rest for
  the diary, an inkwell and a leather writing pad. Unstable hextech hangs over the parchment from a brass arm, turning
  inside spinning gimbal rings with motes circling it; while someone writes, glyphs stream from the parchment into the
  orb, and a freshly learned rune rises over the desk as a glowing hieroglyph. The desk glows faintly while an orb lies on it.
- Rune research: choose one of 72 runes in the star book, then draw it on a nine-cornered parchment of hexagonal
  sections: drag ink with the mouse from the hextech orb to every point of the rune. The ink branches: a new line may
  start at the orb or at any section already inked. Every step costs ink of the essence whose corner of the parchment
  it heads towards, a step onto a point ten of the point's own essence; drops of colour around the line's end show what each
  direction costs. Right click takes a step back for free - ink is spent only when the last point binds the rune. A
  finished drawing writes the rune's scroll into the output slot; shorter drawings write scrolls with up to three stars.
- Dark runes: in chapter I they are stone the ink cannot pass. From chapter II on they are dark fields the ink may
  cross, but some of their sections are cursed - like a minesweeper, every inked section next to them shows how many
  curses touch it, and dark sections can be flagged with a right click. Stepping on a curse bursts the orb: the sheet
  burns, the ink drawn so far is spent and the blast hurts; the curse is remembered for the next attempt. Chapter III
  hides points inside rings of dark runes.
- Runes are learned by studying their scrolls: the knowledge stays with the researcher, with the scroll's stars. A
  scroll of a rune that is not open to you in the research book yet cannot be studied.
- Ink: the desk holds nine vials, one per element; the part the current thread would spend shimmers in them.
- Essence works: ink is made, not found, and carried in plain glass bottles. The nine signs - fire, water, earth, air,
  light, shadow, life, metal, lightning - are essences.
  - The Alembic (set over fire, a lit campfire, lava or magma) boils items in water into essence - a mixture of all
    their essences. An item the player who put it in has not studied boils into as much dark unknown essence instead. Every item takes 250 mB of water: pour a bucket in, or pipe it from a Pump standing in water.
    The flask shows the water, the bubbles and the item turning inside; the essence is bottled or leaves through the
    spout at the front.
  - The Separator splits a mixture in two with a reagent (charcoal - fire, sugar - water, clay - earth, feather - air,
    glow ink sac - light, ink sac - shadow, bone meal - life, iron nugget - metal, redstone - lightning; item tags, so
    packs can change them): the reagent's essence becomes a concentrate, all the rest goes on with a dark unknown
    essence added - never ink, best poured away. Each side takes the mixture in or lets the concentrate or the rest out - chosen on the
    port panel of its screen or by sneak-using that side; the top is a gas vent. Without a reagent it waits, and its
    screen shows what is missing.
  - Essence is one mixed liquid measured in millibuckets, one millibucket per point of an item's essence (the
    number in its tooltip): a bottle holds 16 mB, a jar 256 mB, and every step of a rune's thread costs 1 mB of ink. Essence Jars - big glass
    jars with a wooden lid - store it (in through the lid and the sides, out at the bottom; separator inlets and drain valves draw from them). The Research Table keeps no ink of its
    own: it draws each essence's ink from jars of that concentrate within two blocks of the desk.
  - Brass Pipes carry essence and water and join a machine only where it has a port - the machine's model grows a
    joint there; no vessel takes back what its own outlet lets out. The Drain Valve pours whatever reaches it out into
    the world while its handwheel is open, and drains jars beside it or piped to it.
- Researcher's Goggles (leather, brass and glass panes), worn on the head: looking at a jar, a pipe or a machine of the
  essence works shows the essences inside it beside the crosshair; looking at a Research Table shows its ink, essence
  by essence, while coloured streams run from each jar of concentrate to the orb.
- Essence Jar labels: sneak-use a side or the lid of a jar with an empty hand to stick an enamel label with an essence's sign on it;
  every click shows the next essence, then none. Only a sign for the player.
- Every essence has its own picture: a flame, a drop, a mountain, wind, the sun, a crescent, a sprout, a gear and a bolt.
- Liquid essence in jars, alembics and separators swirls with an animated texture, glows brighter at its surface,
  darkens in the depths and bubbles; a jar's surface sways a little.
- Sounds: the alembic bubbles while it boils, the separator drips, the pump strokes, the drain valve's handwheel
  turns, jars glug when bottles are poured, ink drips on the parchment with every step and a chime marks a quest done.
- Every research takes a paper from the table's paper slot when it starts.
- Integrations: [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) shows boiling and separation, a bottle of each
  concentrate and an info page for every block of the essence works; [Jade](https://www.curseforge.com/minecraft/mc-mods/jade)
  shows what the jars, separators, alembics, pumps and valves hold and do.
- The Researcher's Path: the first page of the research book is a path of quests from the Research Table to the full
  codex - brass, unstable hextech, the diary, studying items, essences, the essence works, ink and the rune chapters.
  Click a quest to see its goal, its recipe and a short explanation; quests are done the moment their goal is met.
  Bookmarks turn the book between the path and the star map.
- The Researcher's Diary is a star book: a dark sky to drag around and zoom, where the 72 runes hang as stars in
  constellations - Smelting and the tool runes in the centre, the other runes on the rays of their elements. Click a
  star to see the rune; on the Research Table it goes to the parchment with one click.
- Every item carries elements of different strength (base materials, then names and recipes for everything else).
  Put an item into the study slot to find its elements by their echoes: they hide on nine strings, as far out as they
  are strong. Strike the parchment anywhere: a ripple runs out and stops at the nearest hidden element; everything
  inside it is empty and the sections on its rim that could hold an element glow. Strike an element's own section to
  reveal it. A studied item shows its elements in its tooltip and can be dissolved into ink.
- Five research tools crafted from the scrolls of the first runes - Researcher's Lens (the fog lifts farther), Fork
  Quill (the thread may cross itself), Mortar (some sections of the thread cost no ink), Brass Scraper (the thread may pass dark runes) and
  Parchment Stretcher (the first sections cost no ink). They sit in the table's tool slots and grow stronger with their
  rune's stars.
- Unstable hextech from lightning striking a lightning rod on an amethyst block.
- Brass and rune scrolls.
- Base item elements come from a data map (`hextale:elements`), so data packs can change them.

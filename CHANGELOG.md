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
  sections: hold the mouse on the hextech orb and drag an ink thread through every point of the rune. The thread starts
  on the ink of the rune's action element and takes the colour of each point it passes; every section costs a drop of
  the ink it runs on. Dark runes block it, it never crosses itself, and fog lifts only around the thread. Drag back to
  wind it up for free - ink is spent only when the last point binds the rune. A finished thread writes the rune's
  scroll into the output slot; shorter threads write scrolls with up to three stars.
- Runes are learned by studying their scrolls: the knowledge stays with the researcher, with the scroll's stars. A
  scroll of a rune that is not open to you in the research book yet cannot be studied.
- Ink: the desk holds nine vials, one per element. Studied items put into the inkwell dissolve into the ink of their
  elements; the part the current thread would spend shimmers in the vials. Every research takes a paper from the
  table's paper slot when it starts.
- The Researcher's Diary is a star book: a dark sky to drag around and zoom, where the 72 runes hang as stars in
  constellations - Smelting and the tool runes in the centre, the other runes on the rays of their elements. Click a
  star to see the rune; on the Research Table it goes to the parchment with one click.
- Every item carries elements of different strength (base materials, then names and recipes for everything else).
  Put an item into the study slot to find its elements by their echoes: they hide on nine strings, as far out as they
  are strong. Strike the parchment anywhere: a ripple runs out and stops at the nearest hidden element; everything
  inside it is empty and the sections on its rim that could hold an element glow. Strike an element's own section to
  reveal it. A studied item shows its elements in its tooltip and can be dissolved into ink.
- Five research tools crafted from the scrolls of the first runes - Researcher's Lens (the fog lifts farther), Fork
  Quill (the thread may cross itself), Mortar (more ink per item), Brass Scraper (the thread may pass dark runes) and
  Parchment Stretcher (the first sections cost no ink). They sit in the table's tool slots and grow stronger with their
  rune's stars.
- Unstable hextech from lightning striking a lightning rod on an amethyst block.
- Brass and rune scrolls.
- Base item elements come from a data map (`hextale:elements`), so data packs can change them.

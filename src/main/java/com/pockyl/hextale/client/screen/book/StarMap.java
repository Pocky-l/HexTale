package com.pockyl.hextale.client.screen.book;

import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.research.ResearchTool;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where every rune hangs in the book's sky. Smelting shines in the centre with the five tool runes around it; every
 * other rune lies on the ray of its acting element (the same nine directions as the research parchment's corners):
 * chapter II runes first, chapter III further out. Coordinates are map pixels at zoom 1, the centre at 0, 0.
 */
final class StarMap {
    static final int TOOL_RING = 58;
    static final int RAY_START = 112;
    static final int RAY_STEP = 30;
    /** Chapter III runes acting on shadow sit beyond their ray's chapter II runes. */
    static final int OUTPOST_GAP = 18;
    static final int SIGN_RADIUS = 392;
    static final int EXTENT = 430;

    /** A line between two stars; {@code chain} lines link a ray, the others lead from Smelting to the tools. */
    record Link(ComplexRune from, ComplexRune to, boolean chain) {
    }

    private static final Map<ComplexRune, int[]> POSITIONS = new LinkedHashMap<>();
    private static final List<Link> LINKS = new ArrayList<>();
    /** The first star of each element's ray, for the faint line from the centre. */
    private static final Map<SimpleRune, ComplexRune> RAY_HEADS = new LinkedHashMap<>();

    static {
        POSITIONS.put(ResearchBook.FIRST, new int[]{0, 0});
        ResearchTool[] tools = ResearchTool.values();
        for (int i = 0; i < tools.length; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / tools.length;
            POSITIONS.put(tools[i].rune(), new int[]{(int) Math.round(Math.cos(angle) * TOOL_RING),
                    (int) Math.round(Math.sin(angle) * TOOL_RING)});
            LINKS.add(new Link(ResearchBook.FIRST, tools[i].rune(), false));
        }
        for (SimpleRune action : HexGrid.RING) {
            List<ComplexRune> ray = new ArrayList<>();
            List<ComplexRune> outposts = new ArrayList<>();
            for (ComplexRune rune : ComplexRunes.all()) {
                if (rune.action() != action || POSITIONS.containsKey(rune)) {
                    continue;
                }
                boolean outpost = ResearchBook.chapter(rune) == 3 && action != SimpleRune.SHADOW && action != SimpleRune.SPARK;
                (outpost ? outposts : ray).add(rune);
            }
            // Chapter II before chapter III, so a ray grows outwards as the book opens.
            ray.sort((a, b) -> ResearchBook.chapter(a) - ResearchBook.chapter(b));
            double[] u = HexGrid.unit(action);
            int r = RAY_START;
            ComplexRune previous = null;
            List<ComplexRune> all = new ArrayList<>(ray);
            all.addAll(outposts);
            for (int i = 0; i < all.size(); i++) {
                ComplexRune rune = all.get(i);
                if (i == ray.size() && i > 0) {
                    r += OUTPOST_GAP;
                }
                // A slight zigzag keeps long rays from looking ruled.
                double side = (i % 2 == 0 ? 1 : -1) * 6;
                POSITIONS.put(rune, new int[]{(int) Math.round(u[0] * r - u[1] * side), (int) Math.round(u[1] * r + u[0] * side)});
                if (previous != null) {
                    LINKS.add(new Link(previous, rune, true));
                } else {
                    RAY_HEADS.put(action, rune);
                }
                previous = rune;
                r += RAY_STEP;
            }
        }
        if (POSITIONS.size() != ComplexRunes.COUNT) {
            throw new IllegalStateException("The star map misses runes: " + POSITIONS.size());
        }
    }

    private StarMap() {
    }

    static int[] position(ComplexRune rune) {
        return POSITIONS.get(rune);
    }

    static Map<ComplexRune, int[]> positions() {
        return Collections.unmodifiableMap(POSITIONS);
    }

    static List<Link> links() {
        return Collections.unmodifiableList(LINKS);
    }

    static Map<SimpleRune, ComplexRune> rayHeads() {
        return Collections.unmodifiableMap(RAY_HEADS);
    }
}

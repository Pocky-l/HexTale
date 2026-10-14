package com.pockyl.hextale.research;

import net.minecraft.world.item.Item;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.List;

/**
 * The parchment of one rune: dark-rune walls (one element each), the points to cover and their resources, and the
 * star thresholds in symbols.
 *
 * @param walls  per section: the dark rune's element, or null for open parchment
 * @param points the rune's points; the first two are its action and target elements
 */
public record RuneBoard(ComplexRune rune, SimpleRune[] walls, List<Point> points, Par par) {
    /** A point to cover with a symbol; {@code resource} is left on it to fill it. */
    public record Point(SimpleRune element, int cell, Item resource) {
    }

    /**
     * Star thresholds.
     *
     * @param three the best route with every tool at full strength
     * @param two   at least two symbols above that and one above the best route without tools
     * @param sheet symbols a plain sheet holds: finishing within it earns one star
     * @param plain the best route without tools
     */
    public record Par(int three, int two, int sheet, int plain) {
    }

    public int tier() {
        return ResearchBook.chapter(rune);
    }

    /** Index of the point in this section, or -1. */
    public int pointAt(int cell) {
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).cell() == cell) {
                return i;
            }
        }
        return -1;
    }

    public int stars(int symbols) {
        return symbols <= par.three ? 3 : symbols <= par.two ? 2 : 1;
    }

    /** Elements dissolved by filled points (bit per element ordinal). */
    public int dissolved(int filledMask) {
        int mask = 0;
        for (int i = 0; i < points.size(); i++) {
            if ((filledMask & 1 << i) != 0) {
                mask |= 1 << points.get(i).element().ordinal();
            }
        }
        return mask;
    }

    /** Sections that stop ink: dark runes whose element no filled point has dissolved. */
    public boolean[] activeWalls(int filledMask) {
        int dissolved = dissolved(filledMask);
        boolean[] out = new boolean[walls.length];
        for (int i = 0; i < walls.length; i++) {
            out[i] = walls[i] != null && (dissolved & 1 << walls[i].ordinal()) == 0;
        }
        return out;
    }
}

package com.pockyl.hextale.research;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.List;

/**
 * The parchment of one rune: dark runes (walls the ink thread cannot cross), the points the thread must pass and the
 * star thresholds in thread length.
 *
 * @param walls  per section: whether a dark rune is there
 * @param points the rune's points; the first two are its action and target elements
 */
public record RuneBoard(ComplexRune rune, boolean[] walls, List<Point> points, Par par) {
    /** A point the thread must pass; after it the thread runs on this element's ink. */
    public record Point(SimpleRune element, int cell) {
    }

    /**
     * Star thresholds in sections of thread.
     *
     * @param three the shortest plain thread found (tools can beat it)
     * @param two   a thread that wanders a little
     * @param plain the plain thread the generator found (sections from the orb to the last point)
     */
    public record Par(int three, int two, int plain) {
    }

    public int tier() {
        return ResearchBook.chapter(rune);
    }

    public boolean wall(int cell) {
        return walls[cell];
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

    public int stars(int length) {
        return length <= par.three ? 3 : length <= par.two ? 2 : 1;
    }
}

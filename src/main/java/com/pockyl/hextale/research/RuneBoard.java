package com.pockyl.hextale.research;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.Essence;

import java.util.List;

/**
 * The parchment of one rune: stone dark runes (walls the ink cannot cross, chapter I), dark fields (chapters II and III:
 * passable, but some of their sections are cursed and burst the orb, like the mines of a minesweeper), the points the
 * ink must reach and the star thresholds in sections of ink.
 *
 * @param walls  per section: a stone dark rune
 * @param dark   per section: a dark field section (passable)
 * @param cursed per section: a cursed dark field section
 * @param points the rune's points; the first two are its action and target elements
 */
public record RuneBoard(ComplexRune rune, boolean[] walls, boolean[] dark, boolean[] cursed, List<Point> points, Par par) {
    /** A point the ink must reach. */
    public record Point(Essence element, int cell) {
    }

    /**
     * Star thresholds in sections of ink.
     *
     * @param three the plain drawing's length
     * @param two   a drawing that wanders a little
     * @param plain the plain drawing the generator found (sections from the orb to every point)
     */
    public record Par(int three, int two, int plain) {
    }

    public int tier() {
        return ResearchBook.chapter(rune);
    }

    public boolean wall(int cell) {
        return walls[cell];
    }

    public boolean dark(int cell) {
        return dark[cell];
    }

    public boolean cursed(int cell) {
        return cursed[cell];
    }

    /** Whether any neighbour of this section is a dark field section: inked, it then shows its count of curses. */
    public boolean nearDark(int cell) {
        for (int next : HexGrid.neighbours(cell)) {
            if (dark[next]) {
                return true;
            }
        }
        return false;
    }

    /** How many cursed sections touch this one. */
    public int danger(int cell) {
        int count = 0;
        for (int next : HexGrid.neighbours(cell)) {
            if (cursed[next]) {
                count++;
            }
        }
        return count;
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

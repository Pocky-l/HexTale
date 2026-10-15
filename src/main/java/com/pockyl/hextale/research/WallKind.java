package com.pockyl.hextale.research;

import java.util.Locale;

/**
 * The dark runes on a parchment: walls, each with its own effect when the ink runs into it. Texts:
 * {@code gui.hextale.research_table.wall.<id>}.
 */
public enum WallKind {
    /** The ink stops in the section before it. */
    STONE,
    /** The ink bounces back and runs its remaining steps the other way. */
    MIRROR;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}

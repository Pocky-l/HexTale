package com.pockyl.hextale.rune;

import java.util.Locale;

/**
 * What a complex rune does. The category sets the rune's ink colour, so the colour of a rune already tells its kind.
 */
public enum RuneCategory {
    DESTRUCTION(0xEF6B5B),
    CREATION(0xD6AB52),
    PROTECTION(0x9FC7E8),
    MOVEMENT(0xCFE9F2),
    ENERGY(0x7EF0FF),
    PERCEPTION(0xFFE08A),
    LIQUIDS(0x4FB3FF),
    LIFE(0x8FE07A),
    MECHANICS(0xB8C4CC);

    private final int color;

    RuneCategory(int color) {
        this.color = color;
    }

    /** Ink colour, RGB. */
    public int color() {
        return color;
    }

    public String translationKey() {
        return "rune.hextale.category." + name().toLowerCase(Locale.ROOT);
    }
}

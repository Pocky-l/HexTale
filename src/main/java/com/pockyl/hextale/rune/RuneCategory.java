package com.pockyl.hextale.rune;

import java.util.Locale;

/**
 * What a complex rune does. The category sets the rune's ink colour and how its two radicals are composed into one
 * hieroglyph, so the shape of a rune already tells its kind.
 */
public enum RuneCategory {
    DESTRUCTION(0xEF6B5B, Layout.LEFT_RIGHT),
    CREATION(0xD6AB52, Layout.TOP_BOTTOM),
    PROTECTION(0x9FC7E8, Layout.BOX),
    MOVEMENT(0xCFE9F2, Layout.LEFT_RIGHT),
    ENERGY(0x7EF0FF, Layout.LEFT_RIGHT),
    PERCEPTION(0xFFE08A, Layout.ROOF),
    LIQUIDS(0x4FB3FF, Layout.TOP_BOTTOM),
    LIFE(0x8FE07A, Layout.TOP_BOTTOM),
    MECHANICS(0xB8C4CC, Layout.LEFT_RIGHT);

    private final int color;
    private final Layout layout;

    RuneCategory(int color, Layout layout) {
        this.color = color;
        this.layout = layout;
    }

    /** Ink colour, RGB. */
    public int color() {
        return color;
    }

    public Layout layout() {
        return layout;
    }

    public String translationKey() {
        return "rune.hextale.category." + name().toLowerCase(Locale.ROOT);
    }

    /** Hieroglyph structures: like ⿰ ⿱ ⿴ ⿸ in real scripts. */
    public enum Layout {
        LEFT_RIGHT,
        TOP_BOTTOM,
        BOX,
        ROOF
    }
}

package com.pockyl.hextale.quest;

/**
 * A theme of the researcher's guide: the workshop, the essence works, the rune lore, the hextech circle, the pylons, the
 * forge and chemtech. Its steps breathe a faint haze of its colour in the book.
 */
public enum Region {
    WORKSHOP(0xE0B85A),
    ESSENCES(0x3FE0A0),
    RUNES(0xB07AF0),
    CIRCLE(0x4A9CFF),
    PYLONS(0xF07AC0),
    FORGE(0xFF8A3A),
    CHEMTECH(0x7FD05A);

    private final int color;

    Region(int color) {
        this.color = color;
    }

    /** The region's tint: its haze in the book. */
    public int color() {
        return color;
    }
}

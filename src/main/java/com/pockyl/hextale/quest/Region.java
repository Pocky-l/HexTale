package com.pockyl.hextale.quest;

/**
 * A theme of the researcher's guide: its steps gather into one cluster in the book's sky, around the region's anchor,
 * under the region's name and in a faint haze of its colour. Regions are laid out around the workshop: the essence works
 * and the hextech circle to the east, the rune lore to the south.
 */
public enum Region {
    WORKSHOP(0xE0B85A, 0, 0),
    ESSENCES(0x5CD6C8, 250, -60),
    FIELDS(0x8FE07A, 250, -300),
    CHEMTECH(0xA8D84A, 520, -200),
    CIRCLE(0x5CC8F2, 480, 160),
    PYLONS(0xB08AE0, 760, 200),
    FORGE(0xFF9A4A, 60, 300),
    FIRE(0xFF6A3A, 330, 440),
    WATERS(0x4AA8FF, 560, 520),
    EARTH(0xC8A06A, -260, 280),
    LIGHT(0xFFE89A, -220, 600),
    STORM(0x9FEFFF, 120, 640),
    LIFE(0x7AE07A, -560, 320);

    private final int color;
    private final int x;
    private final int y;

    Region(int color, int x, int y) {
        this.color = color;
        this.x = x;
        this.y = y;
    }

    /** The region's tint: its haze and its name in the book. */
    public int color() {
        return color;
    }

    /** Where the region's cluster gathers on the book's page. */
    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public String translationKey() {
        return "book.hextale.region." + name().toLowerCase(java.util.Locale.ROOT);
    }
}

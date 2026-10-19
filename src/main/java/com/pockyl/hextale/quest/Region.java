package com.pockyl.hextale.quest;

/**
 * A theme of the researcher's guide. Its steps grow as one branch in the book's sky, away from the step it starts from,
 * in the region's own direction, in a faint haze of its colour: the workshop in the middle, the essence works to the
 * east, the hextech circle south-east with the pylons beyond it, the forge south, the rune lore west.
 *
 * @param angle the direction the branch grows in, degrees on screen (0 = east, 90 = south)
 */
public enum Region {
    WORKSHOP(0xE0B85A, 200),
    ESSENCES(0x3FE0A0, -20),
    RUNES(0xB07AF0, 150),
    CIRCLE(0x4A9CFF, 70),
    PYLONS(0xF07AC0, 15),
    FORGE(0xFF8A3A, 115);

    private final int color;
    private final double angle;

    Region(int color, double angle) {
        this.color = color;
        this.angle = angle;
    }

    /** The region's tint: its haze in the book. */
    public int color() {
        return color;
    }

    /** The direction the region's branch grows in, radians on screen. */
    public double angle() {
        return Math.toRadians(angle);
    }
}

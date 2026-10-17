package com.pockyl.hextale.rune;

/**
 * The nine primal essences around a circle (fire on top, then clockwise lightning, air, light, life, water, shadow,
 * earth, metal): the layout of the star map of runes and of the codex.
 */
public final class PrimalRing {
    /** Order around the circle, starting at the top. */
    public static final Essence[] RING = {Essence.FIRE, Essence.SPARK, Essence.AIR, Essence.LIGHT,
            Essence.LIFE, Essence.WATER, Essence.SHADOW, Essence.EARTH, Essence.METAL};
    private static final double[][] UNIT = new double[Essence.PRIMALS.size()][];

    static {
        for (int i = 0; i < RING.length; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / RING.length;
            UNIT[RING[i].ordinal()] = new double[]{StrictMath.cos(angle), StrictMath.sin(angle)};
        }
    }

    private PrimalRing() {
    }

    /** Unit vector from the centre towards the primal's place on the circle. */
    public static double[] unit(Essence primal) {
        return UNIT[primal.ordinal()];
    }

    /** The primal's place on the circle (0 = top), or -1 for a compound. */
    public static int index(Essence essence) {
        for (int i = 0; i < RING.length; i++) {
            if (RING[i] == essence) {
                return i;
            }
        }
        return -1;
    }
}

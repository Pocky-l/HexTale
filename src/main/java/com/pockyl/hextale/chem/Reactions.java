package com.pockyl.hextale.chem;

import com.pockyl.hextale.rune.Essence;

import org.jspecify.annotations.Nullable;

/**
 * The chemistry of the Lab Bench's flask, read straight off the essences: every compound forms when its two parents meet
 * in the flask at the right heat. The thermometer has {@value #ZONES} zones (cold, cool, warm, hot, searing); each primal
 * has a heat of its own (fire searing, lightning and light hot, metal, earth and life warm, air and water cool, shadow
 * cold) and a compound reacts in the zone halfway between its parents', rounded away from the middle - so fiery things
 * react hot, watery and shadowy things cold, and a compound of two compounds lands where its parents point.
 */
public final class Reactions {
    public static final int ZONES = 5;
    /** Degrees each zone spans: cold 0-20, cool 20-40, warm 40-60, hot 60-80, searing 80-100. */
    public static final float ZONE_SPAN = 20;
    public static final float MAX_TEMPERATURE = 100;
    private static final int MIDDLE = 2;
    private static final int[] ZONE_OF = new int[Essence.COUNT];
    private static final Essence[][] PRODUCT = new Essence[Essence.COUNT][Essence.COUNT];

    static {
        for (Essence essence : Essence.values()) {
            ZONE_OF[essence.ordinal()] = computeZone(essence);
            if (!essence.isPrimal()) {
                Essence[] parents = essence.parents();
                PRODUCT[parents[0].ordinal()][parents[1].ordinal()] = essence;
                PRODUCT[parents[1].ordinal()][parents[0].ordinal()] = essence;
            }
        }
    }

    private Reactions() {
    }

    private static int computeZone(Essence essence) {
        if (essence.isPrimal()) {
            return switch (essence) {
                case FIRE -> 4;
                case SPARK, LIGHT -> 3;
                case METAL, EARTH, LIFE -> 2;
                case AIR, WATER -> 1;
                default -> 0;
            };
        }
        Essence[] parents = essence.parents();
        int sum = computeZone(parents[0]) + computeZone(parents[1]);
        if (sum % 2 == 0) {
            return sum / 2;
        }
        return sum > MIDDLE * 2 ? (sum + 1) / 2 : (sum - 1) / 2;
    }

    /** The zone (0 cold - 4 searing) in which the essence forms from its parents; a primal's own heat. */
    public static int zone(Essence essence) {
        return ZONE_OF[essence.ordinal()];
    }

    /** The zone a temperature lies in. */
    public static int zoneAt(float temperature) {
        return Math.clamp((int) Math.floor(temperature / ZONE_SPAN), 0, ZONES - 1);
    }

    /** The middle of a zone, in degrees. */
    public static float middle(int zone) {
        return (zone + 0.5F) * ZONE_SPAN;
    }

    /** What two essences make together, in either order, or null when they do not react. */
    public static @Nullable Essence product(Essence a, Essence b) {
        return PRODUCT[a.ordinal()][b.ordinal()];
    }

    /** Whether forming it gives off gas that presses on the flask: airy things, steam and blasts. */
    public static boolean gas(Essence essence) {
        return essence.share(Essence.AIR) >= 0.4 || essence == Essence.STEAM || essence == Essence.BLAST;
    }
}

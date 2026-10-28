package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes an Impulse Plate takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars (1-3). They
 * throw higher and farther, soften the landing, keep the thrown running and fling hostile creatures away.
 */
public enum ImpulseRune implements HolderRune {
    /** It throws higher, and an aimed plate reaches farther targets. */
    SHIFT("air_earth"),
    /** It throws farther along its arrow, and an aimed throw flies quicker. */
    MOTOR("metal_spark"),
    /** Whoever it threw lands softly. */
    BREATH("air_life"),
    /** Whoever it throws keeps running fast for a while after. */
    HASTE("spark_life"),
    /** A hostile creature that steps on is flung back and high, with no soft landing. */
    BANISHING("light_shadow");

    public static final int SLOTS = 3;
    /** Ticks of Speed after a throw (Haste), by strength; Speed II from three stars. */
    public static final int[] HASTE_TICKS = {60, 100, 140, 180};
    /** Upward speed a hostile creature is flung with (Banishing), by strength. */
    public static final double[] BANISH_UP = {1.1, 1.4, 1.7, 2.0};
    /** Speed it is flung back with, against the arrow. */
    public static final double BANISH_BACK = 0.6;
    /** Upward speed of a throw, blocks a tick, by Shift's strength (index 0 = without it). */
    private static final double[] UP = {1.0, 1.2, 1.4, 1.65, 1.95};
    /** Speed along the arrow, by Motor's strength. */
    private static final double[] FORWARD = {0.45, 0.8, 1.1, 1.4, 1.8};
    /** How far (sideways) an aimed plate throws, by Shift's strength. */
    private static final int[] RANGE = {24, 32, 40, 48, 64};

    private final ComplexRune rune;

    ImpulseRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "impulse";
    }

    @Nullable
    public static ImpulseRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static double up(int strength) {
        return UP[Mth.clamp(strength, 0, 4)];
    }

    public static int range(int strength) {
        return RANGE[Mth.clamp(strength, 0, 4)];
    }

    public static double forward(int strength) {
        return FORWARD[Mth.clamp(strength, 0, 4)];
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> HolderRune.percent((float) (up(i) / up(0) - 1));
            case MOTOR -> HolderRune.percent((float) (forward(i) / forward(0) - 1));
            case BREATH -> "";
            case HASTE -> HolderRune.seconds(HolderRune.at(HASTE_TICKS, i));
            case BANISHING -> Integer.toString(apex(BANISH_UP[Mth.clamp(i, 1, 4) - 1]));
        };
    }

    /** About how many blocks a throw straight up with {@code speed} rises (vanilla gravity and drag). */
    public static int apex(double speed) {
        double y = 0;
        for (double v = speed; v > 0; v = (v - 0.08) * 0.98) {
            y += v;
        }
        return (int) Math.round(y);
    }
}

package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Levitation Plate takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars (1-3).
 * They make the column taller and quicker and the way down soft, put away what it lifts and lull the creatures in it.
 */
public enum PlateRune implements HolderRune {
    /** The column reaches higher. */
    SHIFT("air_earth"),
    /** It lifts faster. */
    MOTOR("metal_spark"),
    /** Whoever leaves the column floats down slowly for a while. */
    BREATH("air_life"),
    /** Items held at the top of the column are put into the container that caps it. */
    CACHE("earth_shadow"),
    /** Creatures in the column hang asleep, and stay dazed for a while after. */
    SLUMBER("life_shadow");

    public static final int SLOTS = 3;
    /** Ticks between two puttings away at the top (Cache), by strength. */
    public static final int[] CACHE_EVERY = {20, 10, 5, 2};
    /** Ticks a creature stays dazed after the column (Slumber), by strength. */
    public static final int[] SLUMBER_TICKS = {60, 120, 200, 300};
    /** Blocks the column reaches without Shift. */
    public static final int HEIGHT = 6;
    public static final int[] EXTRA_HEIGHT = {3, 6, 10, 18};
    /** Rising speed, blocks a tick, by Motor's strength (index 0 = without it). */
    private static final float[] SPEED = {0.22F, 0.28F, 0.34F, 0.42F, 0.5F};
    /** Ticks of Slow Falling after leaving the column, by strength. */
    public static final int[] FLOAT_TICKS = {40, 80, 120, 160};

    private final ComplexRune rune;

    PlateRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "plate";
    }

    @Nullable
    public static PlateRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int height(int strength) {
        return strength <= 0 ? HEIGHT : HEIGHT + HolderRune.at(EXTRA_HEIGHT, strength);
    }

    public static float speed(int strength) {
        return SPEED[Mth.clamp(strength, 0, 4)];
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(height(i));
            case MOTOR -> HolderRune.percent(speed(i) / speed(0) - 1);
            case BREATH -> HolderRune.seconds(HolderRune.at(FLOAT_TICKS, i));
            case CACHE -> HolderRune.seconds(HolderRune.at(CACHE_EVERY, i));
            case SLUMBER -> HolderRune.seconds(HolderRune.at(SLUMBER_TICKS, i));
        };
    }
}

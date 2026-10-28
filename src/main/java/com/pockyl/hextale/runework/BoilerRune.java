package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Chem Boiler takes - burned in at the Hextech Altar (the boiler alone on it), up to {@link #SLOTS}; a broken
 * boiler keeps them. Strength = stars (1-3). Its steam reaches further and pushes harder, it drinks less water, its
 * fumes come out scrubbed, and the people working beside it catch its rhythm.
 */
public enum BoilerRune implements HolderRune {
    /** The steam reaches machines further away. */
    SHIFT("air_earth"),
    /** The machines it drives work faster still. */
    BELLOWS("air_fire"),
    /** Steam takes less water. */
    DAM("earth_water"),
    /** Its fumes wear the land less. */
    PURIFICATION("light_water"),
    /** Players in its reach keep Haste while it drives. */
    HASTE("spark_life");

    public static final int SLOTS = 3;
    /** Reach in blocks every way, index 0 = without Shift. */
    public static final int[] REACH = {4, 5, 6, 8, 10};
    /** How much faster the driven machines work on top of the boiler's own push. */
    public static final float[] PUSH = {0.5F, 1.0F, 1.5F, 2.0F};
    /** Ticks of steam per millibucket of water, index 0 = without Dam. */
    public static final int[] WATER_TICKS = {4, 6, 8, 10, 12};
    /** Share of the fumes scrubbed out. */
    public static final float[] SCRUBBED = {0.25F, 0.4F, 0.55F, 0.65F};
    /** Haste level kept on the players around it. */
    public static final int[] HASTE_LEVEL = {1, 1, 2, 2};

    private final ComplexRune rune;

    BoilerRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "boiler";
    }

    @Nullable
    public static BoilerRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int reach(int shift) {
        return REACH[Mth.clamp(shift, 0, REACH.length - 1)];
    }

    public static int waterTicks(int dam) {
        return WATER_TICKS[Mth.clamp(dam, 0, WATER_TICKS.length - 1)];
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(reach(i));
            case BELLOWS -> "+" + HolderRune.factor(HolderRune.at(PUSH, i));
            case DAM -> HolderRune.percent(1 - WATER_TICKS[0] / (float) waterTicks(i));
            case PURIFICATION -> HolderRune.percent(HolderRune.at(SCRUBBED, i));
            case HASTE -> HolderRune.roman(HolderRune.at(HASTE_LEVEL, i));
        };
    }
}

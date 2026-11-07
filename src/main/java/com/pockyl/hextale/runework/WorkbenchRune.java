package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes an Awakened Workbench takes - burned in at the Hextech Altar (the bench alone on it), up to {@link #SLOTS}; a
 * broken bench keeps them. Strength = stars (1-3). They do what the Crystal Battery once did: a bigger pool of charge,
 * charge passed on to other benches, more out of a crystal, charge from the sun.
 */
public enum WorkbenchRune implements HolderRune {
    /** It holds more charge. */
    CACHE("earth_shadow"),
    /** It hands charge to the benches around it, reaches further and feeds faster. */
    CHAIN_LIGHTNING("spark_air"),
    /** A crystal soaked in may give half as much again. */
    THRIFT("earth_water"),
    /** Under the open sky or glass, by day, it slowly charges itself from the sunlight. */
    SUNLIGHT_CELL("light_spark");

    public static final int SLOTS = 3;
    /** Charge the bench holds, by strength. */
    public static final int[] POOL = {150, 300, 600, 1200};
    /** Blocks of reach added, by strength. */
    public static final int[] REACH = {2, 4, 6, 8};
    /** Charge handed to each bench or heart per feed, by strength. */
    public static final int[] FEED = {9, 12, 18, 24};
    public static final float[] BONUS = {0.15F, 0.25F, 0.35F, 0.45F};
    /** Sunlight Cell: ticks of daylight per point of charge. */
    public static final int[] SUN_TICKS = {200, 120, 80, 60};

    private final ComplexRune rune;

    WorkbenchRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "workbench";
    }

    @Nullable
    public static WorkbenchRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case CACHE -> Integer.toString(HolderRune.at(POOL, i));
            case CHAIN_LIGHTNING -> Integer.toString(HolderRune.at(REACH, i));
            case THRIFT -> HolderRune.percent(HolderRune.at(BONUS, i));
            case SUNLIGHT_CELL -> Integer.toString(1200 / HolderRune.at(SUN_TICKS, i));
        };
    }
}

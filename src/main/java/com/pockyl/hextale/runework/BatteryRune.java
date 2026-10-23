package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Crystal Battery takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more
 * with a Lens (1-4). It holds more, reaches further, and now and then gets more out of a crystal than the crystal had.
 */
public enum BatteryRune implements HolderRune {
    /** It holds more charge. */
    CACHE("earth_shadow"),
    /** It feeds the workbenches further away. */
    CHAIN_LIGHTNING("spark_air"),
    /** A crystal soaked in may give half as much again. */
    DAM("earth_water"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Share of capacity added, by strength. */
    public static final float[] ROOM = {0.25F, 0.5F, 0.75F, 1.0F};
    /** Blocks of reach added, by strength. */
    public static final int[] REACH = {2, 4, 6, 8};
    public static final float[] BONUS = {0.15F, 0.25F, 0.35F, 0.45F};

    private final ComplexRune rune;

    BatteryRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "battery";
    }

    @Nullable
    public static BatteryRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case CACHE -> HolderRune.percent(HolderRune.at(ROOM, i));
            case CHAIN_LIGHTNING -> Integer.toString(HolderRune.at(REACH, i));
            case DAM -> HolderRune.percent(HolderRune.at(BONUS, i));
            case LENS -> "";
        };
    }
}

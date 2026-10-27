package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Pink Brazier takes - burned in at the Hextech Altar (the brazier alone on it), up to {@link #SLOTS}; a
 * broken brazier keeps them. Strength = stars (1-3). Its pink light reaches further, the plants it
 * touches may burst into growth, people around it heal, and a vial may burn far longer.
 */
public enum BrazierRune implements HolderRune {
    /** The light reaches further. */
    SHIFT("air_earth"),
    /** A plant the light touches may grow as if bone-mealed. */
    POLLEN("life_air"),
    /** Players in the light keep Regeneration. */
    HEALING("water_life"),
    /** A vial may burn twice as long. */
    ETERNAL_FLAME("fire_light");

    public static final int SLOTS = 3;
    /** Radius of the light, index 0 = without Shift. */
    public static final int[] RADIUS = {4, 5, 6, 7, 9};
    public static final float[] BLOOM = {0.08F, 0.14F, 0.2F, 0.28F};
    /** Regeneration kept on players in the light, in ticks. */
    public static final int[] HEALING_TICKS = {100, 140, 180, 240};
    public static final float[] LONGER_BURN = {0.15F, 0.25F, 0.35F, 0.45F};

    private final ComplexRune rune;

    BrazierRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "brazier";
    }

    @Nullable
    public static BrazierRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(RADIUS[i]);
            case POLLEN -> HolderRune.percent(HolderRune.at(BLOOM, i));
            case HEALING -> HolderRune.seconds(HolderRune.at(HEALING_TICKS, i));
            case ETERNAL_FLAME -> HolderRune.percent(HolderRune.at(LONGER_BURN, i));
        };
    }
}

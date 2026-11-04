package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes an Essence Sprinkler takes - burned in at the Hextech Altar (the sprinkler alone on it), up to
 * {@link #SLOTS}; a broken sprinkler keeps them. Strength = stars (1-3). Its spray reaches further, a dose it sprays may
 * cost nothing, a plant it wets may grow a stage there and then, and its spray heals.
 */
public enum SprinklerRune implements HolderRune {
    /** The spray reaches further. */
    SHIFT("air_earth"),
    /** A dose sprayed may cost nothing. */
    DAM("earth_water"),
    /** A plant the spray wets may grow a stage at once. */
    FERTILITY("earth_life"),
    /** Players caught in the spray keep Regeneration. */
    HEALING("water_life");

    public static final int SLOTS = 3;
    /** Radius of the spray, index 0 = without Shift. */
    public static final int[] RADIUS = {3, 4, 5, 6, 7};
    public static final float[] THRIFT = {0.15F, 0.25F, 0.35F, 0.5F};
    public static final float[] QUICKEN = {0.08F, 0.12F, 0.16F, 0.22F};
    /** Regeneration kept on players in the spray, in ticks. */
    public static final int[] HEALING_TICKS = {100, 140, 180, 240};

    private final ComplexRune rune;

    SprinklerRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "sprinkler";
    }

    @Nullable
    public static SprinklerRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(RADIUS[i]);
            case DAM -> HolderRune.percent(HolderRune.at(THRIFT, i));
            case FERTILITY -> HolderRune.percent(HolderRune.at(QUICKEN, i));
            case HEALING -> HolderRune.seconds(HolderRune.at(HEALING_TICKS, i));
        };
    }
}

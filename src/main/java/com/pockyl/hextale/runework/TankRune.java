package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes an Axolotl Tank takes - burned in at the Hextech Altar (the tank alone on it), up to {@link #SLOTS}; a
 * broken tank keeps them. Strength = stars (1-3). They make the creature in it thrive: more slime and sooner, meals
 * that go further, experience, and some of its healing shared with the people around.
 */
public enum TankRune implements HolderRune {
    /** A shedding may give one slime more. */
    SPRING("life_water"),
    /** The axolotl sheds sooner. */
    HASTE("spark_life"),
    /** A shedding may cost no meal. */
    DAM("earth_water"),
    /** Players around a fed tank keep Regeneration. */
    HEALING("water_life"),
    /** Every shedding gives experience. */
    RADIANCE("life_light");

    public static final int SLOTS = 3;
    public static final float[] MORE_SLIME = {0.2F, 0.35F, 0.5F, 0.65F};
    /** The time between sheddings, as a share of the usual. */
    public static final float[] SOONER = {0.8F, 0.65F, 0.5F, 0.4F};
    public static final float[] MEAL_SPARED = {0.2F, 0.33F, 0.46F, 0.6F};
    /** Regeneration kept on players within {@link #HEALING_RADIUS} blocks, in ticks. */
    public static final int[] HEALING_TICKS = {100, 140, 180, 240};
    public static final double HEALING_RADIUS = 6;
    public static final int[] EXPERIENCE = {1, 2, 3, 4};

    private final ComplexRune rune;

    TankRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "tank";
    }

    @Nullable
    public static TankRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SPRING -> HolderRune.percent(HolderRune.at(MORE_SLIME, i));
            case HASTE -> HolderRune.percent(1 - HolderRune.at(SOONER, i));
            case DAM -> HolderRune.percent(HolderRune.at(MEAL_SPARED, i));
            case HEALING -> HolderRune.seconds(HolderRune.at(HEALING_TICKS, i));
            case RADIANCE -> Integer.toString(HolderRune.at(EXPERIENCE, i));
        };
    }
}

package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Chem Still takes - burned in at the Hextech Altar (the still alone on it), up to {@link #SLOTS}; a broken
 * still keeps them. Strength = stars, one more with a Lens (1-4). They make more of the shimmer and faster, pay
 * experience, and now and then a crystal grows out of the purest drops.
 */
public enum StillRune implements HolderRune {
    /** A refining may give a second vial. */
    STEAM("fire_water"),
    /** It refines faster. */
    HASTE("spark_life"),
    /** Every refined vial gives experience. */
    RADIANCE("life_light"),
    /** The purest drops may grow into a Synthetic Crystal. */
    PURIFICATION("light_water"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    public static final float[] SECOND_VIAL = {0.15F, 0.25F, 0.35F, 0.45F};
    /** Refining time as a share of the usual. */
    public static final float[] FASTER = {0.8F, 0.65F, 0.5F, 0.4F};
    public static final int[] EXPERIENCE = {2, 3, 4, 6};
    public static final float[] CRYSTAL = {0.04F, 0.07F, 0.1F, 0.14F};

    private final ComplexRune rune;

    StillRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "still";
    }

    @Nullable
    public static StillRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case STEAM -> HolderRune.percent(HolderRune.at(SECOND_VIAL, i));
            case HASTE -> HolderRune.percent(1 - HolderRune.at(FASTER, i));
            case RADIANCE -> Integer.toString(HolderRune.at(EXPERIENCE, i));
            case PURIFICATION -> HolderRune.percent(HolderRune.at(CRYSTAL, i));
            case LENS -> "";
        };
    }
}

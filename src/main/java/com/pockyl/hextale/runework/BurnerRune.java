package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Chem Burner takes - burned in at the Hextech Altar (the burner alone on it), up to {@link #SLOTS}; a
 * broken burner keeps them. Strength = stars (1-3). They make the cheap green flame go further: fuel that lasts longer,
 * brews that give more doses and experience, and a warmth that keeps fire off the people beside it.
 */
public enum BurnerRune implements HolderRune {
    /** A portion of fuel may burn twice as long. */
    ETERNAL_FLAME("fire_light"),
    /** A brew boiled over it may give one dose more. */
    STEAM("fire_water"),
    /** A brew boiled over it gives experience. */
    RADIANCE("life_light"),
    /** Players beside the lit burner are kept from fire. */
    FIREPROOFING("life_fire");

    public static final int SLOTS = 3;
    public static final float[] LONGER_BURN = {0.15F, 0.25F, 0.35F, 0.45F};
    public static final float[] MORE_DOSES = {0.2F, 0.35F, 0.5F, 0.65F};
    /** Experience a brew gives. */
    public static final int[] EXPERIENCE = {2, 3, 5, 7};
    /** Fire Resistance kept on players within {@link #WARMTH_RADIUS} blocks, in ticks. */
    public static final int[] WARMTH_TICKS = {200, 300, 400, 500};
    public static final double WARMTH_RADIUS = 4;

    private final ComplexRune rune;

    BurnerRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "burner";
    }

    @Nullable
    public static BurnerRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case ETERNAL_FLAME -> HolderRune.percent(HolderRune.at(LONGER_BURN, i));
            case STEAM -> HolderRune.percent(HolderRune.at(MORE_DOSES, i));
            case RADIANCE -> Integer.toString(HolderRune.at(EXPERIENCE, i));
            case FIREPROOFING -> HolderRune.seconds(HolderRune.at(WARMTH_TICKS, i));
        };
    }
}

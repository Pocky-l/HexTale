package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hand Loupe takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more with a
 * Lens (1-4). They make looking closer quicker and more rewarding: a shorter look, experience for every new item, and a
 * chance that a fresh find teaches one more thing in the bag.
 */
public enum LoupeRune implements HolderRune {
    /** The look takes less time. */
    HASTE("spark_life"),
    /** A newly studied item leaves experience. */
    RADIANCE("life_light"),
    /** A fresh study may teach one more unstudied item from the inventory. */
    INSIGHT("light_earth"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 2;
    /** Share of the look's time taken away, by strength. */
    public static final float[] QUICKER = {0.2F, 0.35F, 0.5F, 0.6F};
    public static final int[] EXPERIENCE = {2, 4, 6, 9};
    public static final float[] SECOND_FIND = {0.15F, 0.25F, 0.35F, 0.45F};

    private final ComplexRune rune;

    LoupeRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "loupe";
    }

    @Nullable
    public static LoupeRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case HASTE -> HolderRune.percent(HolderRune.at(QUICKER, i));
            case RADIANCE -> Integer.toString(HolderRune.at(EXPERIENCE, i));
            case INSIGHT -> HolderRune.percent(HolderRune.at(SECOND_FIND, i));
            case LENS -> "";
        };
    }
}

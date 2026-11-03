package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Sap Tap takes - burned in at the Hextech Altar (the tap alone on it), up to {@link #SLOTS}; a broken tap
 * keeps them. Strength = stars (1-3). The sap drips faster, a drop may come twice as big, and in the sun the tree may
 * give more.
 */
public enum TapRune implements HolderRune {
    /** The sap drips faster. */
    HASTE("spark_life"),
    /** A drop may come twice as big. */
    SPRING("life_water"),
    /** While the sun is up the tree may give an extra drop. */
    PHOTOSYNTHESIS("light_life");

    public static final int SLOTS = 2;
    /** Share of the drip time that is left, by strength. */
    public static final float[] FASTER = {0.8F, 0.65F, 0.5F, 0.4F};
    public static final float[] DOUBLE_DROP = {0.15F, 0.25F, 0.35F, 0.5F};
    public static final float[] SUN_DROP = {0.2F, 0.3F, 0.4F, 0.55F};

    private final ComplexRune rune;

    TapRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "tap";
    }

    @Nullable
    public static TapRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case HASTE -> "x" + HolderRune.factor(Math.round(10 / HolderRune.at(FASTER, i)) / 10F);
            case SPRING -> HolderRune.percent(HolderRune.at(DOUBLE_DROP, i));
            case PHOTOSYNTHESIS -> HolderRune.percent(HolderRune.at(SUN_DROP, i));
        };
    }
}

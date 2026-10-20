package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Lab Bench takes - burned in at the Hextech Altar (the bench alone on it), up to {@link #SLOTS}; a broken
 * bench keeps them. Strength = stars, one more with a Lens (1-4). They make experiments cheaper, calmer and faster, pay
 * experience for every sealed flask, and sometimes catch a flask on the edge of bursting.
 */
public enum LabRune implements HolderRune {
    /** A drop may cost no essence. */
    DAM("earth_water"),
    /** Pressure builds slower. */
    TEMPERING("earth_fire"),
    /** Reactions run faster. */
    HASTE("spark_life"),
    /** Every sealed experiment gives experience. */
    RADIANCE("life_light"),
    /** A flask about to burst may let its pressure out by itself. */
    HEAT_EATER("shadow_fire"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    public static final float[] FREE_DROP = {0.2F, 0.3F, 0.4F, 0.5F};
    /** Pressure gain as a share of the usual. */
    public static final float[] CALMER = {0.8F, 0.65F, 0.5F, 0.4F};
    /** Reaction speed as a factor. */
    public static final float[] FASTER = {1.3F, 1.6F, 1.9F, 2.3F};
    public static final int[] EXPERIENCE = {4, 6, 9, 12};
    public static final float[] CAUGHT = {0.35F, 0.5F, 0.65F, 0.8F};

    private final ComplexRune rune;

    LabRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "lab";
    }

    @Nullable
    public static LabRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case DAM -> HolderRune.percent(HolderRune.at(FREE_DROP, i));
            case TEMPERING -> HolderRune.percent(1 - HolderRune.at(CALMER, i));
            case HASTE -> HolderRune.percent(HolderRune.at(FASTER, i) - 1);
            case RADIANCE -> Integer.toString(HolderRune.at(EXPERIENCE, i));
            case HEAT_EATER -> HolderRune.percent(HolderRune.at(CAUGHT, i));
            case LENS -> "";
        };
    }
}

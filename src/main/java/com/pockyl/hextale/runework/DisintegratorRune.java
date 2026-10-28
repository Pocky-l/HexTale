package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Disintegrator takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more with
 * a Lens (1-4). They unmake things faster, wring more out of them, leave experience behind and keep the leak small.
 */
public enum DisintegratorRune implements HolderRune {
    /** It unmakes things faster. */
    MOTOR("metal_spark"),
    /** More essence out of every thing. */
    DAM("earth_water"),
    /** A chance that an unmade thing leaves experience behind. */
    RADIANCE("life_light"),
    /** Less of the unmaking leaks into the land. */
    HUSH("air_shadow"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Ticks to unmake one thing without Motor. */
    public static final int PACE = 40;
    public static final int[] PACES = {30, 20, 12, 8};
    /** Extra essence out of every thing, by Dam's strength. */
    public static final float[] YIELD = {0.25F, 0.5F, 0.75F, 1.0F};
    /** Chance of an experience orb per thing, by Radiance's strength. */
    public static final float[] EXPERIENCE = {0.1F, 0.2F, 0.3F, 0.45F};
    /** Share of the leak Hush holds back, by strength. */
    public static final float[] QUIET = {0.25F, 0.5F, 0.75F, 0.9F};

    private final ComplexRune rune;

    DisintegratorRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "disintegrator";
    }

    @Nullable
    public static DisintegratorRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    /** Ticks per thing, by Motor's strength (0 = none). */
    public static int pace(int strength) {
        return strength <= 0 ? PACE : HolderRune.at(PACES, strength);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case MOTOR -> HolderRune.seconds(pace(i));
            case DAM -> "+" + HolderRune.percent(HolderRune.at(YIELD, i));
            case RADIANCE -> HolderRune.percent(HolderRune.at(EXPERIENCE, i));
            case HUSH -> "-" + HolderRune.percent(HolderRune.at(QUIET, i));
            case LENS -> "";
        };
    }
}

package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Whirl Reaper takes - burned in at the Hextech Altar like a pylon's, up to {@link #SLOTS}. Strength =
 * stars (1-3); index 0 of the radius and interval tables is the reaper without the rune. Besides the field and the
 * pace, runes make every harvest give more: a bonemeal after it, more produce, a rare golden fruit, water and growth
 * for the plants around, experience, or wind saved.
 */
public enum ReaperRune implements HolderRune {
    /** A wider field. */
    SHIFT("air_earth"),
    /** Whirls leave more often. */
    MOTOR("metal_spark"),
    /** A reaped crop may be bonemealed at once. */
    FERTILITY("earth_life"),
    /** A harvest may give one or two more of its produce. */
    POLLEN("life_air"),
    /** Now and then a golden fruit: a golden carrot, a glistering melon slice or a golden apple. */
    PHOTOSYNTHESIS("light_life"),
    /** Every harvest wets the farmland around and may quicken the plants next to it. */
    SPRING("life_water"),
    /** Harvests give experience, gathered over the reaper. */
    RADIANCE("life_light"),
    /** A whirl may cost no wind. */
    DAM("earth_water");

    public static final int SLOTS = 3;
    /** Blocks around the reaper it reaps, by strength. */
    private static final int[] RADIUS = {4, 5, 6, 7, 8};
    /** Ticks between two whirls, by strength. */
    private static final int[] INTERVAL = {16, 12, 9, 6, 4};
    public static final float[] BONEMEAL = {0.15F, 0.25F, 0.35F, 0.45F};
    public static final float[] MORE = {0.15F, 0.25F, 0.35F, 0.45F};
    public static final float[] GOLDEN = {0.01F, 0.02F, 0.03F, 0.04F};
    /** Chance of a neighbouring plant to grow a step with a wetting harvest. */
    public static final float[] QUICKEN = {0.25F, 0.5F, 0.75F, 1.0F};
    /** Chance of a harvest to give a point of experience. */
    public static final float[] EXPERIENCE = {0.25F, 0.4F, 0.55F, 0.7F};
    public static final float[] THRIFT = {0.2F, 0.35F, 0.5F, 0.6F};

    private final ComplexRune rune;

    ReaperRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "reaper";
    }

    @Nullable
    public static ReaperRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int radius(int strength) {
        return RADIUS[Mth.clamp(strength, 0, 4)];
    }

    public static int interval(int strength) {
        return INTERVAL[Mth.clamp(strength, 0, 4)];
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(radius(i));
            case MOTOR -> String.format(Locale.ROOT, "%.2f", interval(i) / 20.0F);
            case FERTILITY -> HolderRune.percent(HolderRune.at(BONEMEAL, i));
            case POLLEN -> HolderRune.percent(HolderRune.at(MORE, i));
            case PHOTOSYNTHESIS -> HolderRune.percent(HolderRune.at(GOLDEN, i));
            case SPRING -> HolderRune.percent(HolderRune.at(QUICKEN, i));
            case RADIANCE -> HolderRune.percent(HolderRune.at(EXPERIENCE, i));
            case DAM -> HolderRune.percent(HolderRune.at(THRIFT, i));
        };
    }
}

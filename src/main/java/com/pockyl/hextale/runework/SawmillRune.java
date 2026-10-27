package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hex Sawmill takes - burned in at the Hextech Altar like a reaper's, up to {@link #SLOTS}. Strength =
 * stars (1-3); index 0 of the radius and interval tables is the sawmill without the rune. Besides the grove and the
 * pace, runes make every tree give more: a sapling that shoots up, richer leaves, more logs, a rare golden apple, bee
 * nests taken whole, or wind saved.
 */
public enum SawmillRune implements HolderRune {
    /** A wider grove. */
    SHIFT("air_earth"),
    /** Saws fly out more often. */
    MOTOR("metal_spark"),
    /** A planted sapling may grow a stage at once. */
    GROWTH("life_earth"),
    /** Leaves drop as with Fortune: more saplings, sticks and apples. */
    POLLEN("life_air"),
    /** A log may give one more. */
    BLADES("metal_air"),
    /** A felled tree may leave a golden apple. */
    PHOTOSYNTHESIS("light_life"),
    /** Bee nests and hives on a felled tree come down whole, bees and honey inside. */
    HUSH("air_shadow"),
    /** A tree may cost no wind. */
    DAM("earth_water");

    public static final int SLOTS = 3;
    /** Blocks around the sawmill it fells, by strength. */
    private static final int[] RADIUS = {6, 8, 10, 12, 14};
    /** Ticks between two saws, by strength. */
    private static final int[] INTERVAL = {60, 44, 32, 22, 14};
    public static final float[] GROW = {0.2F, 0.35F, 0.5F, 0.65F};
    /** Fortune level the leaves fall with. */
    public static final int[] LEAF_FORTUNE = {1, 2, 3, 4};
    public static final float[] MORE_LOGS = {0.1F, 0.2F, 0.3F, 0.4F};
    public static final float[] GOLDEN = {0.03F, 0.05F, 0.07F, 0.09F};
    public static final float[] THRIFT = {0.2F, 0.35F, 0.5F, 0.6F};

    private final ComplexRune rune;

    SawmillRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "sawmill";
    }

    @Nullable
    public static SawmillRune of(ComplexRune rune) {
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
            case HUSH -> "";
            case GROWTH -> HolderRune.percent(HolderRune.at(GROW, i));
            case POLLEN -> HolderRune.roman(HolderRune.at(LEAF_FORTUNE, i));
            case BLADES -> HolderRune.percent(HolderRune.at(MORE_LOGS, i));
            case PHOTOSYNTHESIS -> HolderRune.percent(HolderRune.at(GOLDEN, i));
            case DAM -> HolderRune.percent(HolderRune.at(THRIFT, i));
        };
    }
}

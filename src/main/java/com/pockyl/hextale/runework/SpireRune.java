package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hex Arc Spire takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars (1-3); index
 * 0 of the radius and interval tables is the spire without the rune. Besides the reach and the pace, runes make the
 * arcs fork to more creatures, hit the unhurt harder, or cost no charge.
 */
public enum SpireRune implements HolderRune {
    /** It reaches creatures farther away. */
    SHIFT("air_earth"),
    /** Arcs leave more often. */
    MOTOR("metal_spark"),
    /** Every arc forks to more creatures. */
    CHAIN_LIGHTNING("spark_air"),
    /** An arc hits an unhurt creature harder. */
    FLASH("spark_light"),
    /** An arc may cost no charge. */
    DAM("earth_water");

    public static final int SLOTS = 2;
    private static final int[] RADIUS = {4, 5, 6, 7, 8};
    /** Ticks between two arcs, by strength. */
    private static final int[] INTERVAL = {40, 30, 22, 15, 10};
    /** More creatures struck by one arc. */
    public static final int[] FORKS = {1, 2, 3, 4};
    /** Extra damage on a creature at full health. */
    public static final float[] BRIGHT = {0.3F, 0.5F, 0.75F, 1.0F};
    public static final float[] THRIFT = {0.2F, 0.35F, 0.5F, 0.6F};

    private final ComplexRune rune;

    SpireRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "spire";
    }

    @Nullable
    public static SpireRune of(ComplexRune rune) {
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
            case CHAIN_LIGHTNING -> "+" + HolderRune.at(FORKS, i);
            case FLASH -> "+" + HolderRune.percent(HolderRune.at(BRIGHT, i));
            case DAM -> HolderRune.percent(HolderRune.at(THRIFT, i));
        };
    }
}

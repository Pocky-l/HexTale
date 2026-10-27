package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a World Anchor takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars (1-3). They
 * hold more of the world, quicken what grows in it and now and then hold it for free.
 */
public enum AnchorWorldRune implements HolderRune {
    /** It holds the chunks around its own too (3x3, then 5x5). */
    SHIFT("air_earth"),
    /** Time runs faster in the held chunks: extra random ticks. */
    HASTE("spark_life"),
    /** Now and then a plant in the held chunks gets a pinch of growth. */
    FERTILITY("earth_life"),
    /** Now and then a minute of holding costs no earth essence. */
    DAM("earth_water");

    public static final int SLOTS = 3;
    /** Chance a minute of holding is free (Dam), by strength. */
    public static final float[] FREE = {0.2F, 0.35F, 0.5F, 0.65F};
    /** Extra random ticks per section a tick, by Haste's strength. */
    public static final int[] EXTRA_TICKS = {1, 2, 3, 5};
    /** Plants given growth every few seconds, by Fertility's strength. */
    public static final int[] GROWTHS = {1, 2, 3, 4};

    private final ComplexRune rune;

    AnchorWorldRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "world_anchor";
    }

    @Nullable
    public static AnchorWorldRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    /** Chunks held every way around its own, by Shift's strength (0 = only its own). */
    public static int radius(int strength) {
        return strength <= 0 ? 0 : strength <= 2 ? 1 : 2;
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> (radius(i) * 2 + 1) + "x" + (radius(i) * 2 + 1);
            case HASTE -> "+" + HolderRune.at(EXTRA_TICKS, i);
            case FERTILITY -> Integer.toString(HolderRune.at(GROWTHS, i));
            case DAM -> HolderRune.percent(HolderRune.at(FREE, i));
        };
    }
}

package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hex Sawmill takes - burned in at the Hextech Altar like a reaper's, up to {@link #SLOTS}. Strength = stars,
 * one more with a Lens (1-4); index 0 of every table is the sawmill without the rune.
 */
public enum SawmillRune {
    /** A wider grove. */
    SHIFT("air_earth"),
    /** Saws fly out more often. */
    MOTOR("metal_spark"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 2;
    /** Blocks around the sawmill it fells, by strength. */
    private static final int[] RADIUS = {6, 8, 10, 12, 14};
    /** Ticks between two saws, by strength. */
    private static final int[] INTERVAL = {60, 44, 32, 22, 14};
    /** mB of the rune's own essence per star, by its chapter (I, II, III). */
    private static final int[] MB_PER_STAR = {12, 18, 24};

    private final ComplexRune rune;

    SawmillRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    public ComplexRune rune() {
        return rune;
    }

    @Nullable
    public static SawmillRune of(ComplexRune rune) {
        for (SawmillRune value : values()) {
            if (value.rune == rune) {
                return value;
            }
        }
        return null;
    }

    /** What burning this rune in with these stars costs: its own essence (the compound of its primals). */
    public EssenceMix cost(int stars) {
        Essence pair = Objects.requireNonNull(Essence.pair(rune.action(), rune.target()), rune.key());
        return EssenceMix.pure(pair, MB_PER_STAR[ResearchBook.chapter(rune) - 1] * stars);
    }

    public static int radius(int strength) {
        return RADIUS[Mth.clamp(strength, 0, 4)];
    }

    public static int interval(int strength) {
        return INTERVAL[Mth.clamp(strength, 0, 4)];
    }

    public String descriptionKey() {
        return "sawmill_rune.hextale." + name().toLowerCase(Locale.ROOT) + ".desc";
    }

    public String valueKey() {
        return "sawmill_rune.hextale." + name().toLowerCase(Locale.ROOT) + ".value";
    }

    /** The number behind a strength, for {@link #valueKey()}. */
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(radius(i));
            case MOTOR -> String.format(Locale.ROOT, "%.2f", interval(i) / 20.0F);
            case LENS -> "";
        };
    }
}

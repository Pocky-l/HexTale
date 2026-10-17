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
 * The runes a hex pylon takes - burned in at the Hextech Altar like a smelter's, up to {@link #SLOTS}. Strength =
 * stars, one more with a Lens (1-4); index 0 of every table is the pylon without the rune.
 */
public enum PylonRune {
    /** A wider reach: the gathering radius and how far a link may go. */
    SHIFT("air_earth"),
    /** Moves more often. */
    MOTOR("metal_spark"),
    /** Moves more items at once. */
    LIFT("air_metal"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Blocks around the pylon it gathers from, by strength. */
    private static final int[] RADIUS = {3, 5, 7, 9, 12};
    /** Blocks a link may span, gem to gem, by strength. */
    private static final int[] RANGE = {8, 12, 16, 24, 32};
    /** Ticks between moves, by strength. */
    private static final int[] INTERVAL = {30, 20, 12, 8, 4};
    /** Items per move, by strength. */
    private static final int[] AMOUNT = {8, 16, 32, 48, 64};
    /** mB of the rune's own essence per star, by its chapter (I, II, III). */
    private static final int[] MB_PER_STAR = {12, 18, 24};

    private final ComplexRune rune;

    PylonRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    public ComplexRune rune() {
        return rune;
    }

    @Nullable
    public static PylonRune of(ComplexRune rune) {
        for (PylonRune value : values()) {
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

    public static int range(int strength) {
        return RANGE[Mth.clamp(strength, 0, 4)];
    }

    public static int interval(int strength) {
        return INTERVAL[Mth.clamp(strength, 0, 4)];
    }

    public static int amount(int strength) {
        return AMOUNT[Mth.clamp(strength, 0, 4)];
    }

    public String descriptionKey() {
        return "pylon_rune.hextale." + name().toLowerCase(Locale.ROOT) + ".desc";
    }

    public String valueKey() {
        return "pylon_rune.hextale." + name().toLowerCase(Locale.ROOT) + ".value";
    }

    /** The number behind a strength, for {@link #valueKey()}. */
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> radius(i) + " / " + range(i);
            case MOTOR -> String.format(Locale.ROOT, "%.1f", interval(i) / 20.0F);
            case LIFT -> Integer.toString(amount(i));
            case LENS -> "";
        };
    }
}

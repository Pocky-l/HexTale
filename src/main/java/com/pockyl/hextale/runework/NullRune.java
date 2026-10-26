package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Null Lantern takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more with
 * a Lens (1-4). They widen its quiet, keep phantoms off the sleepless and make it bite what came in from outside.
 */
public enum NullRune implements HolderRune {
    /** The quiet reaches farther. */
    SHIFT("air_earth"),
    /** No phantoms come down on players within its quiet. */
    HUSH("air_shadow"),
    /** Hostile creatures inside its quiet take a slow burn. */
    BANISHING("light_shadow"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Blocks of quiet without Shift. */
    public static final int RADIUS = 24;
    public static final int[] EXTRA_RADIUS = {8, 16, 24, 40};
    /** Ticks between two bites of Banishing, by strength. */
    public static final int[] BURN_TICKS = {80, 60, 40, 30};

    private final ComplexRune rune;

    NullRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "null";
    }

    @Nullable
    public static NullRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int radius(int strength) {
        return strength <= 0 ? RADIUS : RADIUS + HolderRune.at(EXTRA_RADIUS, strength);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(radius(i));
            case HUSH, LENS -> "";
            case BANISHING -> HolderRune.seconds(HolderRune.at(BURN_TICKS, i));
        };
    }
}

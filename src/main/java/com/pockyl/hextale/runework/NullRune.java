package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Null Lantern takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars (1-3). They
 * widen its quiet, keep phantoms off the sleepless, make it bite what came in from outside, and swallow the sounds
 * inside it (deep enough, the vibrations too).
 */
public enum NullRune implements HolderRune {
    /** The quiet reaches farther. */
    SHIFT("air_earth"),
    /** No phantoms come down on players within its quiet. */
    HUSH("air_shadow"),
    /** Hostile creatures inside its quiet take a slow burn. */
    BANISHING("light_shadow"),
    /** Sounds inside its quiet fade; from {@link #INSULATING} stars no vibration inside reaches sculk. */
    SILENCE("shadow_spark");

    public static final int SLOTS = 3;
    /** Blocks of quiet without Shift. */
    public static final int RADIUS = 24;
    public static final int[] EXTRA_RADIUS = {8, 16, 24, 40};
    /** How much of a sound is left inside, by Silence's strength. */
    public static final float[] HUSHED = {0.3F, 0.15F, 0.05F, 0};
    /** Silence's strength from which vibrations inside are swallowed too. */
    public static final int INSULATING = 3;
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
            case HUSH -> "";
            case BANISHING -> HolderRune.seconds(HolderRune.at(BURN_TICKS, i));
            case SILENCE -> HolderRune.percent(1 - HolderRune.at(HUSHED, i));
        };
    }
}

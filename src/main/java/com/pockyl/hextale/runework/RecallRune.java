package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Recall Charm takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more with a
 * Lens (1-4). They reach further back, rest shorter and mend a little on the way.
 */
public enum RecallRune implements HolderRune {
    /** It reaches further back. */
    SHIFT("air_earth"),
    /** It rests shorter between two recalls. */
    HASTE("spark_life"),
    /** The way back mends a little. */
    MENDING("life_metal"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Seconds back without Shift, and with it by strength. */
    public static final int SECONDS = 5;
    public static final int[] MORE_SECONDS = {3, 7, 11, 19};
    /** Rest between recalls in ticks, by Haste's strength (index 0 = without it). */
    private static final int[] REST = {600, 440, 320, 220, 140};
    /** Health mended on a recall, by strength. */
    public static final float[] MEND = {2, 3, 4, 6};

    private final ComplexRune rune;

    RecallRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "recall";
    }

    @Nullable
    public static RecallRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int seconds(int strength) {
        return strength <= 0 ? SECONDS : SECONDS + HolderRune.at(MORE_SECONDS, strength);
    }

    public static int rest(int strength) {
        return REST[Mth.clamp(strength, 0, 4)];
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> HolderRune.seconds(seconds(i) * 20);
            case HASTE -> HolderRune.seconds(rest(i));
            case MENDING -> Float.toString(HolderRune.at(MEND, i) / 2);
            case LENS -> "";
        };
    }
}

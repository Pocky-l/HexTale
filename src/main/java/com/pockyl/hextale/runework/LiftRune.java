package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hex Lift takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more with a
 * Lens (1-4). The lift a player leaves from decides: how far it reaches, and whether it takes company along.
 */
public enum LiftRune implements HolderRune {
    /** It reaches a lift farther away. */
    SHIFT("air_earth"),
    /** Creatures standing on the lift go along. */
    BLINK("spark_shadow"),
    /** I step off the lift quick on my feet (Speed for a few seconds). */
    HASTE("spark_life"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Blocks a lift reaches without Shift. */
    public static final int REACH = 16;
    public static final int[] EXTRA_REACH = {8, 16, 32, 48};
    /** Ticks of Speed after a ride, by Haste's strength. */
    public static final int[] SPEED_TICKS = {60, 100, 160, 240};

    private final ComplexRune rune;

    LiftRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "lift";
    }

    @Nullable
    public static LiftRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int reach(int strength) {
        return strength <= 0 ? REACH : REACH + HolderRune.at(EXTRA_REACH, Mth.clamp(strength, 1, 4));
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(reach(i));
            case HASTE -> HolderRune.seconds(HolderRune.at(SPEED_TICKS, i));
            case BLINK, LENS -> "";
        };
    }
}

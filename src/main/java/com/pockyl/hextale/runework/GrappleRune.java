package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Grapple Gauntlet takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more
 * with a Lens (1-4). They throw the tendril farther, pull harder and soften the landing.
 */
public enum GrappleRune implements HolderRune {
    /** The tendril reaches farther. */
    SHIFT("air_earth"),
    /** It pulls faster. */
    MOTOR("metal_spark"),
    /** No fall damage for a while after a pull. */
    BREATH("air_life"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Blocks the tendril reaches without Shift. */
    public static final int REACH = 32;
    public static final int[] REACHES = {40, 48, 56, 64};
    /** Blocks per tick of the pull without Motor. */
    public static final float SPEED = 0.9F;
    public static final float[] SPEEDS = {1.1F, 1.3F, 1.5F, 1.8F};

    private final ComplexRune rune;

    GrappleRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "grapple";
    }

    @Nullable
    public static GrappleRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int reach(int strength) {
        return strength <= 0 ? REACH : HolderRune.at(REACHES, strength);
    }

    public static float speed(int strength) {
        return strength <= 0 ? SPEED : HolderRune.at(SPEEDS, strength);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(reach(i));
            case MOTOR -> HolderRune.factor(speed(i) * 20);
            case BREATH, LENS -> "";
        };
    }
}

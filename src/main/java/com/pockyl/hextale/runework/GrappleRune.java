package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Grapple Gauntlet takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars (1-3).
 * They throw the claw farther, reel in faster and soften the landing, and let the claw yank and cut creatures.
 */
public enum GrappleRune implements HolderRune {
    /** The tendril reaches farther. */
    SHIFT("air_earth"),
    /** It reels in faster. */
    MOTOR("metal_spark"),
    /** No fall damage for a while after a pull. */
    BREATH("air_life"),
    /** The claw bites into a creature too and yanks it to the glove. */
    GRAVITY("earth_air"),
    /** The claw cuts the creature it bites. */
    BLADES("metal_air");

    public static final int SLOTS = 3;
    /** Speed a hooked creature is yanked towards the wearer with (Gravity), by strength. */
    public static final float[] PULL = {0.8F, 1.1F, 1.4F, 1.7F};
    /** Damage the claw deals to the creature it bites (Blades), by strength. */
    public static final float[] CUT = {3, 5, 7, 9};
    /** Blocks the tendril reaches without Shift. */
    public static final int REACH = 32;
    public static final int[] REACHES = {40, 48, 56, 64};
    /** Blocks a tick the rope reels in without Motor. */
    public static final float REEL = 0.35F;
    public static final float[] REELS = {0.45F, 0.55F, 0.65F, 0.8F};

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

    public static float reel(int strength) {
        return strength <= 0 ? REEL : HolderRune.at(REELS, strength);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(reach(i));
            case MOTOR -> HolderRune.factor(reel(i) * 20);
            case BREATH -> "";
            case GRAVITY -> HolderRune.factor(HolderRune.at(PULL, i) / PULL[0]);
            case BLADES -> HolderRune.factor(HolderRune.at(CUT, i) / 2);
        };
    }
}

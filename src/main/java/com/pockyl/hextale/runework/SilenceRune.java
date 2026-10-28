package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Silence Ward takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more with
 * a Lens (1-4). They widen its hush, deepen it, and keep vibrations inside from reaching sculk.
 */
public enum SilenceRune implements HolderRune {
    /** The hush reaches farther. */
    SHIFT("air_earth"),
    /** Sounds inside fade further, down to nothing. */
    HUSH("air_shadow"),
    /** No vibration inside reaches a sculk sensor, a shrieker or a warden. */
    INSULATOR("shadow_spark"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Blocks of hush without Shift. */
    public static final int RADIUS = 8;
    public static final int[] EXTRA_RADIUS = {4, 8, 12, 20};
    /** How much of a sound is left inside without Hush. */
    public static final float LEFT = 0.3F;
    /** How much of a sound is left inside, by Hush's strength. */
    public static final float[] HUSHED = {0.15F, 0.08F, 0.03F, 0};

    private final ComplexRune rune;

    SilenceRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "silence_ward";
    }

    @Nullable
    public static SilenceRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int radius(int strength) {
        return strength <= 0 ? RADIUS : RADIUS + HolderRune.at(EXTRA_RADIUS, strength);
    }

    /** The share of a sound left inside, by Hush's strength (0 = none). */
    public static float left(int strength) {
        return strength <= 0 ? LEFT : HolderRune.at(HUSHED, strength);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(radius(i));
            case HUSH -> HolderRune.percent(1 - left(i));
            case INSULATOR, LENS -> "";
        };
    }
}

package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hextech Magnet takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more
 * with a Lens (1-4). They widen its pull, sweeten the experience it draws and put things straight into the pocket.
 */
public enum MagnetRune implements HolderRune {
    /** It pulls from farther. */
    SHIFT("air_earth"),
    /** Experience it draws in is worth more. */
    RADIANCE("life_light"),
    /** Things land in the pocket at once instead of flying over. */
    CACHE("earth_shadow"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Blocks of pull without Shift. */
    public static final int RADIUS = 6;
    public static final int[] RADII = {8, 10, 12, 16};
    /** Extra share of every experience orb, by Radiance's strength. */
    public static final float[] BONUS = {0.2F, 0.35F, 0.5F, 0.75F};

    private final ComplexRune rune;

    MagnetRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "magnet";
    }

    @Nullable
    public static MagnetRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int radius(int strength) {
        return strength <= 0 ? RADIUS : HolderRune.at(RADII, strength);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(radius(i));
            case RADIANCE -> "+" + HolderRune.percent(HolderRune.at(BONUS, i));
            case CACHE, LENS -> "";
        };
    }
}

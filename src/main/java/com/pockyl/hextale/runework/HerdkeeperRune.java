package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Herdkeeper takes - burned in at the Hextech Altar like a sawmill's, up to {@link #SLOTS}. Strength = stars
 * (1-3); index 0 of the radius, interval and rest tables is the Herdkeeper without the rune. Besides the pasture and the
 * pace, runes make the herd give more: a second helping, a shorter rest, experience, or essence saved.
 */
public enum HerdkeeperRune implements HolderRune {
    /** A wider pasture. */
    SHIFT("air_earth"),
    /** Animals are tended more often. */
    MOTOR("metal_spark"),
    /** A tended animal may give a second helping. */
    POLLEN("life_air"),
    /** Animals rest for less time. */
    SPRING("life_water"),
    /** Tending may give experience, gathered over the trough. */
    RADIANCE("life_light"),
    /** A tended animal may cost no essence. */
    DAM("earth_water");

    public static final int SLOTS = 3;
    /** Blocks around the Herdkeeper it reaches, by strength. */
    private static final int[] RADIUS = {6, 8, 10, 12, 14};
    /** Ticks between two tended animals, by strength. */
    private static final int[] INTERVAL = {60, 48, 38, 30, 24};
    /** Ticks an animal rests after it was tended, by strength. */
    private static final int[] REST = {6000, 4800, 3900, 3000, 2400};
    public static final float[] MORE = {0.15F, 0.25F, 0.35F, 0.45F};
    public static final float[] EXPERIENCE = {0.25F, 0.4F, 0.55F, 0.7F};
    public static final float[] THRIFT = {0.2F, 0.35F, 0.5F, 0.6F};

    private final ComplexRune rune;

    HerdkeeperRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "herdkeeper";
    }

    @Nullable
    public static HerdkeeperRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int radius(int strength) {
        return RADIUS[Mth.clamp(strength, 0, 4)];
    }

    public static int interval(int strength) {
        return INTERVAL[Mth.clamp(strength, 0, 4)];
    }

    public static int rest(int strength) {
        return REST[Mth.clamp(strength, 0, 4)];
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(radius(i));
            case MOTOR -> String.format(Locale.ROOT, "%.2f", interval(i) / 20.0F);
            case SPRING -> Integer.toString(rest(i) / 20);
            case POLLEN -> HolderRune.percent(HolderRune.at(MORE, i));
            case RADIANCE -> HolderRune.percent(HolderRune.at(EXPERIENCE, i));
            case DAM -> HolderRune.percent(HolderRune.at(THRIFT, i));
        };
    }
}

package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hextech Purifier takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars (1-3);
 * index 0 of the tables is the purifier without the rune. It reaches more land, draws faster, its crystal holds more
 * before it clouds, the land it heals may bloom, and a draw may cost no light.
 */
public enum PurifierRune implements HolderRune {
    /** It draws on land farther away. */
    SHIFT("air_earth"),
    /** It draws more often. */
    MOTOR("metal_spark"),
    /** Its crystal takes in more before it clouds over. */
    PURIFICATION("light_water"),
    /** Land it heals may sprout grass and flowers. */
    FERTILITY("earth_life"),
    /** Now and then a draw costs no light essence. */
    DAM("earth_water");

    public static final int SLOTS = 3;
    /** Chance a draw costs no light (Dam), by strength. */
    public static final float[] FREE = {0.2F, 0.35F, 0.5F, 0.65F};
    /** Chunks every way it draws on, by strength. */
    private static final int[] RADIUS = {1, 2, 3, 4, 5};
    /** Ticks between two draws, by strength. */
    private static final int[] INTERVAL = {20, 15, 11, 8, 5};
    /** Share the crystal holds more. */
    public static final float[] ROOM = {0.25F, 0.5F, 0.75F, 1.0F};

    private final ComplexRune rune;

    PurifierRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "purifier";
    }

    @Nullable
    public static PurifierRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int radius(int strength) {
        return RADIUS[Mth.clamp(strength, 0, 4)];
    }

    public static int interval(int strength) {
        return INTERVAL[Mth.clamp(strength, 0, 4)];
    }

    /** Chance that a healed grass spot blooms, by strength. */
    public static float bloomChance(int strength) {
        return 0.2F + 0.15F * Mth.clamp(strength, 0, 4);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(radius(i) * 2 + 1);
            case MOTOR -> String.format(Locale.ROOT, "%.2f", interval(i) / 20.0F);
            case PURIFICATION -> "+" + HolderRune.percent(HolderRune.at(ROOM, i));
            case FERTILITY -> HolderRune.percent(bloomChance(i));
            case DAM -> HolderRune.percent(HolderRune.at(FREE, i));
        };
    }
}

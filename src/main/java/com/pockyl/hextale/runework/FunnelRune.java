package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Greedy Funnel takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars (1-3); index
 * 0 of the radius table is the funnel without the rune. Besides the reach, runes make every death it swallows give
 * more: experience, spoils, now and then a head, decay saved, the creature's essence.
 */
public enum FunnelRune implements HolderRune {
    /** It reaches deaths farther away. */
    SHIFT("air_earth"),
    /** More experience from every death. */
    RADIANCE("life_light"),
    /** One more of a stackable drop now and then. */
    SPOILS("earth_shadow"),
    /** Now and then the creature's head. */
    ENGRAVING("light_metal"),
    /** A death may cost no decay. */
    DAM("earth_water"),
    /** A death may leave its essence: what its drops are made of soaks into a jar beside the funnel. */
    SPONGE("shadow_water");

    public static final int SLOTS = 2;
    /** Blocks around the funnel it swallows deaths in, by strength. */
    private static final int[] RADIUS = {4, 5, 6, 7, 8};
    /** The largest radius any funnel has (the index looks this far). */
    public static final int MAX_RADIUS = 8;
    public static final float[] EXPERIENCE = {1.25F, 1.5F, 1.75F, 2.0F};
    /** Chance of each stackable drop to come one more. */
    public static final float[] MORE = {0.1F, 0.2F, 0.3F, 0.4F};
    public static final float[] HEAD = {0.03F, 0.05F, 0.08F, 0.11F};
    public static final float[] THRIFT = {0.2F, 0.35F, 0.5F, 0.6F};
    /** Chance a death soaks its essence into a jar. */
    public static final float[] SOAK = {0.25F, 0.4F, 0.55F, 0.7F};

    private final ComplexRune rune;

    FunnelRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "funnel";
    }

    @Nullable
    public static FunnelRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int radius(int strength) {
        return RADIUS[Mth.clamp(strength, 0, 4)];
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(radius(i));
            case RADIANCE -> HolderRune.factor(HolderRune.at(EXPERIENCE, i));
            case SPOILS -> HolderRune.percent(HolderRune.at(MORE, i));
            case ENGRAVING -> HolderRune.percent(HolderRune.at(HEAD, i));
            case DAM -> HolderRune.percent(HolderRune.at(THRIFT, i));
            case SPONGE -> HolderRune.percent(HolderRune.at(SOAK, i));
        };
    }
}

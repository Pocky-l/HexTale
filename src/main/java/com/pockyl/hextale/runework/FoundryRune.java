package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hex Foundry takes - burned into its Foundry Hatch at the Hextech Altar (the hatch alone on it), up to
 * {@link #SLOTS}; the hatch keeps them whether the kiln stands or not. Strength = stars, one more with a Lens (1-4). They
 * make the kiln hotter and quicker to heat, its melts richer (ore, experience), its fire thriftier, and leave grains of
 * metal in the slag of molten stone.
 */
public enum FoundryRune implements HolderRune {
    /** Ore may give one more ingot. */
    FORGE("metal_fire"),
    /** More experience from every melt. */
    RADIANCE("life_light"),
    /** A tick of burning may cost no fire. */
    ETERNAL_FLAME("fire_light"),
    /** Heats up faster. */
    BELLOWS("air_fire"),
    /** Burns hotter than the bricks allow: everything melts faster. */
    TEMPERING("earth_fire"),
    /** Stone, sand and gravel may leave a nugget of metal in the slag. */
    SMELTING("fire_earth"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 4;
    /** Chance of each ore to give one ingot more. */
    public static final float[] MORE_INGOTS = {0.15F, 0.25F, 0.35F, 0.45F};
    public static final float[] EXPERIENCE = {1.5F, 2F, 3F, 4F};
    /** Chance of a burning tick to cost no fire. */
    public static final float[] FREE_FIRE = {0.15F, 0.25F, 0.35F, 0.45F};
    /** How many times faster the kiln heats. */
    public static final float[] HEATING = {2F, 3F, 4F, 5F};
    /** Melting speed at full heat. */
    public static final float[] TOP_SPEED = {1.25F, 1.5F, 1.75F, 2F};
    /** Chance of a stone-like melt to leave a nugget. */
    public static final float[] NUGGET = {0.05F, 0.08F, 0.12F, 0.16F};

    private final ComplexRune rune;

    FoundryRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "foundry";
    }

    @Nullable
    public static FoundryRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case FORGE -> HolderRune.percent(HolderRune.at(MORE_INGOTS, i));
            case RADIANCE -> HolderRune.factor(HolderRune.at(EXPERIENCE, i));
            case ETERNAL_FLAME -> HolderRune.percent(HolderRune.at(FREE_FIRE, i));
            case BELLOWS -> HolderRune.factor(HolderRune.at(HEATING, i));
            case TEMPERING -> HolderRune.factor(HolderRune.at(TOP_SPEED, i));
            case SMELTING -> HolderRune.percent(HolderRune.at(NUGGET, i));
            case LENS -> "";
        };
    }
}

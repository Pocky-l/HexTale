package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hex Furnace takes - burned in at the Hextech Altar (the furnace alone on it), up to {@link #SLOTS}; a broken
 * furnace keeps them. Strength = stars, one more with a Lens (1-4). They make its melts richer: more ingots from ore,
 * more experience, fuel spared, a fire that speeds up the longer it burns, and old metal tools and armour melted back
 * into ingots.
 */
public enum FurnaceRune implements HolderRune {
    /** Ore may give one more ingot. */
    FORGE("metal_fire"),
    /** More experience from every melt. */
    RADIANCE("life_light"),
    /** A melt may burn no fuel. */
    ETERNAL_FLAME("fire_light"),
    /** The longer it works without a pause, the faster it melts. */
    BELLOWS("air_fire"),
    /** Iron, gold and copper tools and armour melt into ingots instead of a nugget. */
    REFORGING("fire_metal"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Chance of each ore to give one ingot more. */
    public static final float[] MORE_INGOTS = {0.15F, 0.25F, 0.35F, 0.45F};
    public static final float[] EXPERIENCE = {1.5F, 2F, 3F, 4F};
    public static final float[] FREE_FUEL = {0.15F, 0.25F, 0.35F, 0.45F};
    /** Top speed once fully heated. */
    public static final float[] TOP_SPEED = {1.5F, 2F, 2.5F, 3F};
    /** Ingots an old tool or armour piece melts into. */
    public static final int[] INGOTS_BACK = {1, 2, 3, 4};

    private final ComplexRune rune;

    FurnaceRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "furnace";
    }

    @Nullable
    public static FurnaceRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case FORGE -> HolderRune.percent(HolderRune.at(MORE_INGOTS, i));
            case RADIANCE -> HolderRune.factor(HolderRune.at(EXPERIENCE, i));
            case ETERNAL_FLAME -> HolderRune.percent(HolderRune.at(FREE_FUEL, i));
            case BELLOWS -> HolderRune.factor(HolderRune.at(TOP_SPEED, i));
            case REFORGING -> Integer.toString(HolderRune.at(INGOTS_BACK, i));
            case LENS -> "";
        };
    }
}

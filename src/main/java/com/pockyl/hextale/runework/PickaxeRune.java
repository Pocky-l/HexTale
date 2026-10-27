package com.pockyl.hextale.runework;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a hextech pickaxe takes - like enchantments, burned in at the Hextech Altar from rune steles, up to
 * {@link PickaxeRunes#SLOTS} on any pickaxe. A rune's strength is its stars (1-3). Runes of one
 * {@link Clash} group cannot share a pickaxe (one shape of digging, one right-click power). The effects live in
 * {@link PickaxeEffects}; the book and JEI describe them in one short line ({@link #descriptionKey()}) with the amount
 * per star ({@link #valueKey()}, {@link PickaxeEffects#amount}).
 */
public enum PickaxeRune implements HolderRune {
    // Chapter I
    SMELTING("fire_earth", null),
    TEMPERING("earth_fire", null),
    EROSION("water_earth", Clash.SHAPE),
    INSIGHT("light_earth", Clash.POWER),
    // Chapter II
    DRILL("metal_earth", Clash.SHAPE),
    FORGE("metal_fire", null),
    GEMSTONE("earth_light", null),
    MOTOR("metal_spark", null),
    // Chapter III
    MAGNET("spark_metal", null),
    CACHE("earth_shadow", null),
    PASSAGE("shadow_earth", Clash.POWER),
    QUAKE("spark_earth", Clash.POWER),
    // Passive gifts while digging (2026-10-20)
    /** A chance of Haste while digging. */
    HASTE("spark_life", null),
    /** Drops fall as with Fortune. */
    FERTILITY("earth_life", null),
    /** More experience from ores; stone now and then gives some too. */
    RADIANCE("life_light", null),
    /** A chance to be fed a little; under water every block gives breath back. */
    BREATH("air_life", null),
    /** Digging in the dark gives Night Vision. */
    BANISHING("light_shadow", null),
    /** Opening lava gives Fire Resistance at once. */
    FIREPROOFING("life_fire", null),
    /** In the dark a hex wisp may stay where the block was: a lasting light. */
    ETERNAL_FLAME("fire_light", null),
    /** Wear may be shrugged off and mended a point. */
    MENDING("life_metal", null);

    private final ComplexRune rune;
    private final @Nullable Clash clash;

    PickaxeRune(String key, @Nullable Clash clash) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
        this.clash = clash;
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    public @Nullable Clash clash() {
        return clash;
    }

    @Override
    public String prefix() {
        return "pickaxe";
    }

    @Override
    public String amountText(int strength) {
        return PickaxeEffects.amount(this, strength);
    }

    /** Whether this rune works on right click (only one such rune per pickaxe). */
    public boolean active() {
        return clash == Clash.POWER;
    }

    /** Whether the two runes cannot share a pickaxe. */
    public boolean clashes(PickaxeRune other) {
        return other != this && clash != null && clash == other.clash;
    }

    @Nullable
    public static PickaxeRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    /** Groups of runes that exclude each other on one pickaxe. */
    public enum Clash {
        /** How a pickaxe takes more than one block: an area or a vein. */
        SHAPE,
        /** What it does on right click. */
        POWER
    }
}

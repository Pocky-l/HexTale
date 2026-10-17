package com.pockyl.hextale.runework;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a hextech pickaxe takes - like enchantments, burned in at the Hextech Altar from rune steles, up to
 * {@link PickaxeRunes#SLOTS} on any pickaxe. A rune's strength is its stars (a Lens adds one, up to four). Runes of one
 * {@link Clash} group cannot share a pickaxe (one shape of digging, one right-click power). The effects live in
 * {@link PickaxeEffects}; the book and JEI describe them in one short line ({@link #descriptionKey()}) with the amount
 * per star ({@link #valueKey()}, {@link PickaxeEffects#amount}).
 */
public enum PickaxeRune {
    // Chapter I
    SMELTING("fire_earth", null),
    TEMPERING("earth_fire", null),
    EROSION("water_earth", Clash.SHAPE),
    INSIGHT("light_earth", Clash.POWER),
    LENS("water_light", null),
    // Chapter II
    DRILL("metal_earth", Clash.SHAPE),
    FORGE("metal_fire", null),
    GEMSTONE("earth_light", null),
    MOTOR("metal_spark", null),
    // Chapter III
    MAGNET("spark_metal", null),
    CACHE("earth_shadow", null),
    PASSAGE("shadow_earth", Clash.POWER),
    QUAKE("spark_earth", Clash.POWER);

    /** mB of a rune's own essence per star, by its chapter (I, II, III). */
    private static final int[] MB_PER_STAR = {12, 18, 24};

    private final ComplexRune rune;
    private final @Nullable Clash clash;

    PickaxeRune(String key, @Nullable Clash clash) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
        this.clash = clash;
    }

    public ComplexRune rune() {
        return rune;
    }

    public @Nullable Clash clash() {
        return clash;
    }

    /** What the rune does, one short line for the book and JEI. */
    public String descriptionKey() {
        return "pickaxe_rune.hextale." + name().toLowerCase(java.util.Locale.ROOT) + ".desc";
    }

    /** How much it does at a strength: a pattern for {@link PickaxeEffects#amount}. */
    public String valueKey() {
        return "pickaxe_rune.hextale." + name().toLowerCase(java.util.Locale.ROOT) + ".value";
    }

    /** Whether this rune works on right click (only one such rune per pickaxe). */
    public boolean active() {
        return clash == Clash.POWER;
    }

    /** Whether the two runes cannot share a pickaxe. */
    public boolean clashes(PickaxeRune other) {
        return other != this && clash != null && clash == other.clash;
    }

    /** What burning this rune in with these stars costs at the altar: its own essence (the compound of its primals). */
    public EssenceMix cost(int stars) {
        Essence pair = Objects.requireNonNull(Essence.pair(rune.action(), rune.target()), rune.key());
        return EssenceMix.pure(pair, MB_PER_STAR[ResearchBook.chapter(rune) - 1] * stars);
    }

    @Nullable
    public static PickaxeRune of(ComplexRune rune) {
        for (PickaxeRune value : values()) {
            if (value.rune == rune) {
                return value;
            }
        }
        return null;
    }

    /** Groups of runes that exclude each other on one pickaxe. */
    public enum Clash {
        /** How a pickaxe takes more than one block: an area or a vein. */
        SHAPE,
        /** What it does on right click. */
        POWER
    }
}

package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Soul Flask takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars (1-3). They act
 * on the creature stepping out of the flask.
 */
public enum SoulRune implements HolderRune {
    /** It steps out healed whole. */
    HEALING("water_life"),
    /** A young one steps out grown. */
    GROWTH("life_earth"),
    /** A hostile one steps out dazed for a while. */
    SLUMBER("life_shadow");

    public static final int SLOTS = 3;
    /** Ticks a hostile creature stays dazed, by Slumber's strength. */
    public static final int[] SLUMBER_TICKS = {100, 160, 240, 360};

    private final ComplexRune rune;

    SoulRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "soul";
    }

    @Nullable
    public static SoulRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        return this == SLUMBER ? HolderRune.seconds(HolderRune.at(SLUMBER_TICKS, Mth.clamp(strength, 1, 4))) : "";
    }
}

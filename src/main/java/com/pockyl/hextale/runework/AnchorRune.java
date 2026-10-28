package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Void Anchor takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more with a
 * Lens (1-4). Its crystal clouds slower, it strikes the Void's creatures that come near with arcs, or holds them slow and
 * shining so they cannot creep up on anyone.
 */
public enum AnchorRune implements HolderRune {
    /** Its crystal clouds slower. */
    DAM("earth_water"),
    /** Arcs leap from its crystal into the Void's creatures near it. */
    CHAIN_LIGHTNING("spark_air"),
    /** The Void's creatures near it slow down and shine. */
    BANISHING("light_shadow"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 2;
    /** Share of the clouding it saves. */
    public static final float[] THRIFT = {0.2F, 0.35F, 0.5F, 0.6F};
    /** Damage of one arc. */
    public static final float[] ARC = {4, 6, 8, 10};
    /** Reach of the arcs and of the banishing light, blocks. */
    public static final int REACH = 8;

    private final ComplexRune rune;

    AnchorRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "anchor";
    }

    @Nullable
    public static AnchorRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case DAM -> HolderRune.percent(HolderRune.at(THRIFT, i));
            case CHAIN_LIGHTNING -> HolderRune.factor(HolderRune.at(ARC, i));
            case BANISHING -> HolderRune.roman(i);
            case LENS -> "";
        };
    }
}

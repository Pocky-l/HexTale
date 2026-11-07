package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hex Vault takes, up to {@link #SLOTS}, burned into it as an item at the Rune Circle; a lifted vault keeps
 * them. Strength = stars (1-3). They work wherever it is reached from: more room, free distant takes, tools kept mended,
 * experience for new heights.
 */
public enum VaultRune implements HolderRune {
    /** It holds more. */
    CACHE("earth_shadow"),
    /** A take through a terminal may cost nothing. */
    THRIFT("earth_water"),
    /** Worn tools inside slowly mend. */
    MENDING("life_metal"),
    /** It gives experience whenever it holds more than ever before. */
    RADIANCE("life_light");

    public static final int SLOTS = 3;
    /** Share of room added, by strength. */
    public static final float[] ROOM = {0.25F, 0.5F, 1.0F, 1.5F};
    /** Chance of a free take, by strength. */
    public static final float[] FREE_TAKE = {0.25F, 0.4F, 0.6F, 0.75F};
    /** Durability back per round (5 s), by strength. */
    public static final int[] MEND_POINTS = {2, 4, 8, 12};
    /** Points of new height per point of experience, by strength. */
    public static final int[] POINTS_PER_XP = {512, 256, 128, 64};

    private final ComplexRune rune;

    VaultRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "vault";
    }

    @Nullable
    public static VaultRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case CACHE -> HolderRune.percent(HolderRune.at(ROOM, i));
            case THRIFT -> HolderRune.percent(HolderRune.at(FREE_TAKE, i));
            case MENDING -> Integer.toString(HolderRune.at(MEND_POINTS, i));
            case RADIANCE -> Integer.toString(HolderRune.at(POINTS_PER_XP, i));
        };
    }
}

package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Magic Wand takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one more with a
 * Lens (1-4). The wand wears out as it wakes blocks and starts rituals; its runes keep it whole and pay the researcher back.
 */
public enum WandRune implements HolderRune {
    /** Points of wear are often mended instead. */
    MENDING("life_metal"),
    /** A touch may cost the wand nothing. */
    DAM("earth_water"),
    /** A block wakes faster under the wand. */
    HASTE("spark_life"),
    /** A ritual started with the wand leaves experience when it ends well. */
    RADIANCE("life_light"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 2;
    public static final float[] THRIFT = {0.2F, 0.35F, 0.5F, 0.6F};
    /** Share of the waking time taken away, by strength. */
    public static final float[] QUICKER = {0.25F, 0.4F, 0.55F, 0.7F};
    public static final int[] EXPERIENCE = {4, 8, 12, 18};

    private final ComplexRune rune;

    WandRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "wand";
    }

    @Nullable
    public static WandRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case MENDING -> HolderRune.percent(HolderRune.at(ToolRune.MEND, i));
            case DAM -> HolderRune.percent(HolderRune.at(THRIFT, i));
            case HASTE -> HolderRune.percent(HolderRune.at(QUICKER, i));
            case RADIANCE -> Integer.toString(HolderRune.at(EXPERIENCE, i));
            case LENS -> "";
        };
    }
}

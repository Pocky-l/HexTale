package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Chem Planter takes - burned in at the Hextech Altar (the planter alone on it), up to {@link #SLOTS}; a
 * broken planter keeps them. Strength = stars (1-3). It feeds its plant sooner, a ripe harvest may come double, its
 * growth spills over to the plants beside it, and a dose may not be spent at all.
 */
public enum PlanterRune implements HolderRune {
    /** The planter feeds its plant sooner. */
    GROWTH("life_earth"),
    /** A ripe plant broken on it may drop its harvest twice. */
    FERTILITY("earth_life"),
    /** When the plant grows on the dose, a plant beside the planter may grow too. */
    POLLEN("life_air"),
    /** A dose may not be spent. */
    DAM("earth_water");

    public static final int SLOTS = 3;
    /** Share of the feeding time left, by strength. */
    public static final float[] SOONER = {0.8F, 0.65F, 0.5F, 0.4F};
    public static final float[] DOUBLE_HARVEST = {0.15F, 0.25F, 0.35F, 0.5F};
    public static final float[] SPREAD = {0.25F, 0.35F, 0.45F, 0.6F};
    public static final float[] THRIFT = {0.15F, 0.25F, 0.35F, 0.5F};

    private final ComplexRune rune;

    PlanterRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "planter";
    }

    @Nullable
    public static PlanterRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case GROWTH -> "x" + HolderRune.factor(Math.round(10 / HolderRune.at(SOONER, i)) / 10F);
            case FERTILITY -> HolderRune.percent(HolderRune.at(DOUBLE_HARVEST, i));
            case POLLEN -> HolderRune.percent(HolderRune.at(SPREAD, i));
            case DAM -> HolderRune.percent(HolderRune.at(THRIFT, i));
        };
    }
}

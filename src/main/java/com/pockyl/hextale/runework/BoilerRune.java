package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Chem Boiler takes - burned in in a Rune Circle (the boiler alone in it), up to {@link #SLOTS}; a broken
 * boiler keeps them. Strength = stars (1-3). They make its crude cracking less crude: the pressure comes back sooner,
 * less goes up as fumes, less burns down to the unknown, and now and then a whole batch comes through clean.
 */
public enum BoilerRune implements HolderRune {
    /** The pressure comes back sooner: more batches. */
    STEAM("fire_water"),
    /** Less of each batch goes up as fumes. */
    DAM("earth_water"),
    /** Less of each batch burns down to unknown essence. */
    PURIFICATION("light_water"),
    /** Now and then a whole batch comes through without any loss. */
    PURIFYING_FIRE("fire_shadow");

    public static final int SLOTS = 3;
    /** How much faster the pressure rises. */
    public static final float[] FASTER = {1.3F, 1.6F, 2.0F, 2.4F};
    /** Share of the fumes kept in. */
    public static final float[] FUMES_KEPT = {0.3F, 0.5F, 0.7F, 0.8F};
    /** Share of the burnt part saved from turning unknown. */
    public static final float[] UNKNOWN_SAVED = {0.3F, 0.5F, 0.7F, 0.8F};
    /** Chance that a batch comes through clean. */
    public static final float[] CLEAN_BATCH = {0.1F, 0.18F, 0.27F, 0.35F};

    private final ComplexRune rune;

    BoilerRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "boiler";
    }

    @Nullable
    public static BoilerRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case STEAM -> HolderRune.percent(HolderRune.at(FASTER, i) - 1);
            case DAM -> HolderRune.percent(HolderRune.at(FUMES_KEPT, i));
            case PURIFICATION -> HolderRune.percent(HolderRune.at(UNKNOWN_SAVED, i));
            case PURIFYING_FIRE -> HolderRune.percent(HolderRune.at(CLEAN_BATCH, i));
        };
    }
}

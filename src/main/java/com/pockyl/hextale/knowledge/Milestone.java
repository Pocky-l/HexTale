package com.pockyl.hextale.knowledge;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

/**
 * Progress through the codex. Each milestone will unlock content of later updates; for now they mark progress, write
 * a diary note and grant an advancement. {@link #featured()} are the runes its future content builds on: the diary
 * suggests them first. Texts: {@code diary.hextale.milestone.<count>} and {@code .unlocks}.
 */
public enum Milestone {
    FIRST_DEVICES(3, "fire_light", "air_water", "air_earth"),
    RUNE_TOOLS(12, "fire_earth", "metal_earth", "water_earth", "fire_shadow", "earth_fire",
            "life_metal", "earth_light", "fire_life", "shadow_life"),
    MECHANISMS(24, "metal_fire", "air_spark", "metal_water", "metal_spark", "spark_metal",
            "air_fire", "light_spark", "life_water", "air_metal"),
    RUNE_CHAINS(36, "spark_air", "fire_spark", "spark_shadow", "shadow_earth", "light_fire",
            "water_spark", "earth_spark", "life_spark"),
    HEXCORE(54, "light_metal", "metal_life", "life_light", "light_air", "metal_light",
            "metal_shadow"),
    FULL_CODEX(72);

    private final int count;
    private final List<ComplexRune> featured;

    Milestone(int count, String... featured) {
        this.count = count;
        this.featured = Arrays.stream(featured).map(ComplexRunes::byKey).map(Objects::requireNonNull).toList();
    }

    public int count() {
        return count;
    }

    public List<ComplexRune> featured() {
        return featured;
    }

    public String translationKey() {
        return "diary.hextale.milestone." + count;
    }

    /** The first milestone not reached with this many runes, or null when the codex is complete. */
    @Nullable
    public static Milestone next(int learned) {
        for (Milestone milestone : values()) {
            if (learned < milestone.count) {
                return milestone;
            }
        }
        return null;
    }
}

package com.pockyl.hextale.client.screen.diary;

import net.minecraft.network.chat.Component;

import com.pockyl.hextale.rune.ComplexRune;

import java.util.Set;

/**
 * Hints for runes not found yet. Key runes have a hand-written hint ({@code rune.hextale.<key>.hint}); every other rune
 * gets a riddle composed from its elements: what the action element does, aimed at what the target element is.
 */
final class RuneHints {
    private static final Set<String> HAND_WRITTEN = Set.of(
            "fire_earth", "earth_fire", "fire_light", "air_water", "air_earth", "water_life", "life_metal", "metal_earth",
            "spark_air", "light_earth", "shadow_air", "air_spark", "metal_spark", "fire_spark", "life_earth", "spark_metal",
            "water_fire", "earth_water");
    private static final int RIDDLES = 4;

    private RuneHints() {
    }

    static Component hint(ComplexRune rune) {
        if (HAND_WRITTEN.contains(rune.key())) {
            return Component.translatable(rune.translationKey() + ".hint");
        }
        return Component.translatable("diary.hextale.hint.riddle." + (rune.index() * 7 % RIDDLES),
                Component.translatable(rune.action().translationKey() + ".force"),
                Component.translatable(rune.target().translationKey() + ".object"));
    }

    /** What the two elements mean as action and as target, the dry part of every hint. */
    static Component elements(ComplexRune rune) {
        return Component.translatable("diary.hextale.hint.elements",
                Component.translatable(rune.action().translationKey()), Component.translatable(rune.action().translationKey() + ".action"),
                Component.translatable(rune.target().translationKey()), Component.translatable(rune.target().translationKey() + ".target"));
    }
}

package com.pockyl.hextale.research;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.List;
import java.util.stream.Stream;
import java.util.Objects;

/**
 * The runes' chapters = how rare and costly they are: chapter I is Smelting and the five runes around it, chapter III
 * every rune acting with shadow or lightning, or acting on shadow, chapter II the rest. Runes are found as scrolls in
 * the world's treasure chests: chapter I in plain dungeons, chapter III in the deepest places (see the rune scroll loot
 * modifiers). A chapter also sets a rune's essence cost at the altar.
 */
public final class ResearchBook {
    public static final ComplexRune FIRST = Objects.requireNonNull(ComplexRunes.byKey("fire_earth"));

    /** The five runes of chapter I around Smelting. */
    public static final List<ComplexRune> FIRST_RING = Stream.of("water_light", "light_earth", "water_earth", "earth_spark", "earth_fire")
            .map(ComplexRunes::byKey).toList();

    private ResearchBook() {
    }

    /** Chapter = difficulty: 1 simple, 2 medium, 3 hard. */
    public static int chapter(ComplexRune rune) {
        if (rune == FIRST) {
            return 1;
        }
        if (FIRST_RING.contains(rune)) {
            return 1;
        }
        Essence action = rune.action();
        return action == Essence.SHADOW || action == Essence.SPARK || rune.target() == Essence.SHADOW ? 3 : 2;
    }

    public static List<ComplexRune> runes(int chapter) {
        return ComplexRunes.all().stream().filter(rune -> chapter(rune) == chapter).toList();
    }

    /** The runes of a chapter that can be found: the ones in use. */
    public static List<ComplexRune> findable(int chapter) {
        return runes(chapter).stream().filter(ComplexRune::inUse).toList();
    }
}

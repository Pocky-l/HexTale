package com.pockyl.hextale.research;

import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.List;
import java.util.stream.Stream;
import java.util.Objects;

/**
 * The runes' chapters = how hard their parchments are: chapter I is Smelting and the five runes around it, chapter III
 * every rune acting with shadow or lightning, or acting on shadow, chapter II the rest. Which rune may be studied is
 * decided by the guide ({@link Guide}): a rune opens when the steps before it are done.
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

    /** Whether the researcher may study this rune at the table: its step on the guide is open. */
    public static boolean open(RuneKnowledge knowledge, ComplexRune rune) {
        return Guide.canStudy(rune, knowledge);
    }
}

package com.pockyl.hextale.research;

import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.List;
import java.util.stream.Stream;
import java.util.Objects;

/**
 * The research book's chapters. Chapter I ("First signs") holds Smelting and the five tool runes; chapter II ("Craft")
 * opens after four runes of chapter I, chapter III ("Depths": every rune acting with shadow or lightning, or acting on
 * shadow) after ten runes of chapter II. Inside chapter I Smelting comes first.
 */
public final class ResearchBook {
    public static final ComplexRune FIRST = Objects.requireNonNull(ComplexRunes.byKey("fire_earth"));
    public static final int CHAPTER_II_AFTER = 4;
    public static final int CHAPTER_III_AFTER = 10;

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

    public static int learnedIn(RuneKnowledge knowledge, int chapter) {
        return (int) runes(chapter).stream().filter(knowledge::knows).count();
    }

    public static boolean chapterOpen(RuneKnowledge knowledge, int chapter) {
        return switch (chapter) {
            case 1 -> true;
            case 2 -> learnedIn(knowledge, 1) >= CHAPTER_II_AFTER;
            default -> learnedIn(knowledge, 2) >= CHAPTER_III_AFTER;
        };
    }

    /** Whether the researcher may study this rune at the table. */
    public static boolean open(RuneKnowledge knowledge, ComplexRune rune) {
        int chapter = chapter(rune);
        return chapterOpen(knowledge, chapter) && (chapter > 1 || rune == FIRST || knowledge.knows(FIRST));
    }
}

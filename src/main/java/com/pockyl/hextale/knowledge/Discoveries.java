package com.pockyl.hextale.knowledge;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Picks the runes the diary suggests to discover next: unknown runes, preferring those its next milestone builds on
 * and those made of elements the player has already worked with. A per-player seed breaks ties, so friends get
 * different suggestions, but the list stays stable between visits.
 */
public final class Discoveries {
    private static final double FEATURED = 3;
    private static final double KNOWN_ELEMENT = 1.5;

    private Discoveries() {
    }

    public static List<ComplexRune> suggest(RuneKnowledge knowledge, long seed, int count) {
        Milestone next = Milestone.next(knowledge.runeCount());
        Set<SimpleRune> used = knowledge.usedElements();
        List<Scored> candidates = new ArrayList<>();
        for (ComplexRune rune : ComplexRunes.all()) {
            if (knowledge.knows(rune)) {
                continue;
            }
            double score = 0;
            if (next != null && next.featured().contains(rune)) {
                score += FEATURED;
            }
            if (used.contains(rune.action())) {
                score += KNOWN_ELEMENT;
            }
            if (used.contains(rune.target())) {
                score += KNOWN_ELEMENT;
            }
            score += jitter(seed, rune.index());
            candidates.add(new Scored(rune, score));
        }
        candidates.sort(Comparator.comparingDouble(Scored::score).reversed());
        return candidates.stream().limit(count).map(Scored::rune).toList();
    }

    /** Deterministic pseudo-random value in [0, 1) per player and rune (a SplitMix64 step). */
    private static double jitter(long seed, int index) {
        long z = seed + (index + 1) * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 11) * 0x1.0p-53;
    }

    private record Scored(ComplexRune rune, double score) {
    }
}

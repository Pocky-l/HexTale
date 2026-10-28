package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * One rune a {@link RuneHolder} takes: the codex rune it is, what it does there (one short line, {@link
 * #descriptionKey()}) and the number behind each strength ({@link #valueKey()} with {@link #amountText}). Strength =
 * stars (1-3). Burning it in costs its own essence (the compound of its primals), more for a later chapter.
 */
public interface HolderRune {
    /** mB of a rune's own essence per star, by its chapter (I, II, III). */
    int[] MB_PER_STAR = {12, 18, 24};

    ComplexRune rune();

    /** The enum constant's name. */
    String name();

    /** The holder's prefix in translation keys, e.g. {@code "reaper"}. */
    String prefix();

    /** The number behind a strength (1-4), for {@link #valueKey()}; "" when it has none. */
    String amountText(int strength);

    /** What the rune does, one short line for the book and JEI. */
    default String descriptionKey() {
        return prefix() + "_rune.hextale." + name().toLowerCase(Locale.ROOT) + ".desc";
    }

    /** How much it does at a strength: a pattern for {@link #amountText}. */
    default String valueKey() {
        return prefix() + "_rune.hextale." + name().toLowerCase(Locale.ROOT) + ".value";
    }

    /** What burning this rune in with these stars costs: the compound of its primals (or both primals). */
    default EssenceMix cost(int stars) {
        ComplexRune rune = rune();
        return EssenceMix.ofRune(rune, MB_PER_STAR[ResearchBook.chapter(rune) - 1] * stars);
    }

    /** The value of a table indexed by strength 1-4 (index 0-3). */
    static float at(float[] table, int strength) {
        return table[Mth.clamp(strength, 1, 4) - 1];
    }

    static int at(int[] table, int strength) {
        return table[Mth.clamp(strength, 1, 4) - 1];
    }

    /** A share as a percentage: 0.15 -> "15%", 0.025 -> "2.5%". */
    static String percent(float share) {
        float value = share * 100;
        return (value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value)) + "%";
    }

    /** A factor: 2 -> "2", 1.5 -> "1.5". */
    static String factor(float value) {
        return value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }

    /** Seconds from ticks: 160 -> "8". */
    static String seconds(int ticks) {
        return factor(ticks / 20.0F);
    }

    /** A roman level: 1 -> "I". */
    static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> Integer.toString(level);
        };
    }

    /** The holder rune of a codex rune among {@code values}, or null. */
    @Nullable
    static <R extends HolderRune> R find(R[] values, ComplexRune rune) {
        for (R value : values) {
            if (value.rune() == rune) {
                return value;
            }
        }
        return null;
    }
}

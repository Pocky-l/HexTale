package com.pockyl.hextale.runework;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

/**
 * The runes that direct hextech tools. Every rune of the codex works on a tool (effects in {@link ToolRuneEffects});
 * a few have meaning for the other runes. What a rune does on a tool is never written anywhere: the player finds it out.
 */
public final class ToolRunes {
    /** Fire > earth: mined drops come out smelted. */
    public static final ComplexRune SMELTING = rune("fire_earth");
    /** Earth > fire: the tool often shrugs off wear. */
    public static final ComplexRune TEMPERING = rune("earth_fire");
    /** Water > earth: breaking a loose block washes away the same blocks around it. */
    public static final ComplexRune EROSION = rune("water_earth");
    /** Water > light: focuses the rune in the next slot, one star stronger. */
    public static final ComplexRune LENS = rune("water_light");
    /** Earth > lightning: leads the excess away - the tool rests shorter after a right-click spell. */
    public static final ComplexRune GROUNDING = rune("earth_spark");

    private ToolRunes() {
    }

    /** Every rune of the codex works on a hextech tool ({@link ToolRuneEffects}). */
    public static boolean worksOnTools(ComplexRune rune) {
        return ToolRuneEffects.triggers(rune).length > 0;
    }

    private static ComplexRune rune(String key) {
        return Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }
}

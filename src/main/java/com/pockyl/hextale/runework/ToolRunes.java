package com.pockyl.hextale.runework;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Map;
import java.util.Objects;

/**
 * The runes that direct hextech tools and what one firing costs. A rune burns its action essence; the cost is in mB
 * per firing (per smelted item, per saved durability point, per washed block...). What a rune does on a tool is never
 * written anywhere: the player finds it out.
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
    /** Earth > lightning: leads the excess away - the tool's other runes often fire for free. */
    public static final ComplexRune GROUNDING = rune("earth_spark");

    /** Runes whose firing costs differ from what their moment suggests. */
    private static final Map<ComplexRune, Integer> COSTS = Map.of(SMELTING, 1, TEMPERING, 1, EROSION, 1, LENS, 1, GROUNDING, 0);

    private ToolRunes() {
    }

    /** Every rune of the codex works on a hextech tool ({@link ToolRuneEffects}). */
    public static boolean worksOnTools(ComplexRune rune) {
        return ToolRuneEffects.triggers(rune).length > 0;
    }

    /**
     * mB of the rune's action essence per firing: a mined block or a stroke costs little, a right-click spell more, a
     * held effect pays every few seconds.
     */
    public static int cost(ComplexRune rune) {
        Integer fixed = COSTS.get(rune);
        if (fixed != null) {
            return fixed;
        }
        ToolRuneEffects.Trigger[] triggers = ToolRuneEffects.triggers(rune);
        if (triggers.length == 0) {
            return 1;
        }
        return switch (triggers[0]) {
            case BREAK, WEAR, PASSIVE -> 1;
            case HIT, KILL, HURT, HOLD -> 2;
            case USE -> 4;
        };
    }

    private static ComplexRune rune(String key) {
        return Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }
}

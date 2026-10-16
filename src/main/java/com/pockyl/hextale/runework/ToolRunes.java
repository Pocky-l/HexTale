package com.pockyl.hextale.runework;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Map;
import java.util.Objects;

/**
 * The runes that direct hextech tools and what one firing costs. A rune burns its action essence; the cost is in mB
 * per firing (per smelted item, per saved durability point, per washed block...). Runes missing here cannot be
 * engraved into a tool yet. Texts: {@code runework.hextale.<rune key>} (what it does on a tool).
 */
public final class ToolRunes {
    /** Fire > earth: mined drops come out smelted. */
    public static final ComplexRune SMELTING = rune("fire_earth");

    private static final Map<ComplexRune, Integer> COSTS = Map.of(SMELTING, 1);

    private ToolRunes() {
    }

    public static boolean worksOnTools(ComplexRune rune) {
        return COSTS.containsKey(rune);
    }

    /** mB of the rune's action essence per firing. */
    public static int cost(ComplexRune rune) {
        return COSTS.getOrDefault(rune, 1);
    }

    public static String translationKey(ComplexRune rune) {
        return "runework.hextale." + rune.key();
    }

    private static ComplexRune rune(String key) {
        return Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }
}

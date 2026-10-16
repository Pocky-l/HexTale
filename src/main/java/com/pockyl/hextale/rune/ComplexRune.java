package com.pockyl.hextale.rune;

/**
 * An ordered pair "action -> target" of two different elements: the first says what to do, the second what to act
 * on. Its texts live in the language files under {@code rune.hextale.<key>}: the name, {@code .effect} (what the rune
 * tells a tool or mechanism to do) and {@code .use} (an example device).
 *
 * @param index position in {@link ComplexRunes#all()}, stable: action-major, the action's own element skipped
 */
public record ComplexRune(int index, Essence action, Essence target, RuneCategory category) {
    /** Persistent id, e.g. {@code fire_earth}. */
    public String key() {
        return action.id() + "_" + target.id();
    }

    public String translationKey() {
        return "rune.hextale." + key();
    }

    public int color() {
        return category.color();
    }

    /** The same elements in the other order: always a different rune. */
    public ComplexRune reversed() {
        return ComplexRunes.of(target, action);
    }
}

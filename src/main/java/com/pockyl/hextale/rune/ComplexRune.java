package com.pockyl.hextale.rune;

/**
 * An ordered pair "action -> target" of two different elements: the first says what to do, the second what to act
 * on. Its texts live in the language files under {@code rune.hextale.<key>}: the name, {@code .effect} (what the rune
 * tells a tool or mechanism to do) and {@code .use} (an example device).
 *
 * @param index position in {@link ComplexRunes#all()}, stable: action-major, the action's own element skipped
 */
public record ComplexRune(int index, Essence action, Essence target, RuneCategory category) {
    /** The name (and {@code .effect}) of a rune not in use. */
    public static final String UNKNOWN_KEY = "rune.hextale.unknown";

    /** Persistent id, e.g. {@code fire_earth}. */
    public String key() {
        return action.id() + "_" + target.id();
    }

    /**
     * The key of the name; a rune no holder takes yet ({@link #inUse()}) reads "???" everywhere, its own texts wait in
     * the language files under {@link #codexKey()}.
     */
    public String translationKey() {
        return inUse() ? codexKey() : UNKNOWN_KEY;
    }

    /** The key of the rune's own name, whether or not it is in use. */
    public String codexKey() {
        return "rune.hextale." + key();
    }

    /** Whether some rune holder takes the rune. The others are not in the game yet: never found, never learned. */
    public boolean inUse() {
        return com.pockyl.hextale.runework.RuneHolder.anyTakes(this);
    }

    public int color() {
        return category.color();
    }

    /** The same elements in the other order: always a different rune. */
    public ComplexRune reversed() {
        return ComplexRunes.of(target, action);
    }
}

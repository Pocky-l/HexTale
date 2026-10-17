package com.pockyl.hextale.runework;

import java.util.Locale;

/**
 * The three hextech pickaxes: the plain one (iron strength), the refined one (diamond) and the radiant one
 * (netherite), each made at the Hextech Altar from the one before (runes carried over). Any of them takes up to
 * {@link PickaxeRunes#SLOTS} {@link PickaxeRune}s; the tier sets only what the pickaxe is as a tool.
 */
public enum HextechPickaxe {
    HEXTECH("hextech_pickaxe"),
    REFINED("refined_hextech_pickaxe"),
    RADIANT("radiant_hextech_pickaxe");

    private final String id;

    HextechPickaxe(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** 1 (iron) to 3 (netherite). */
    public int tier() {
        return ordinal() + 1;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}

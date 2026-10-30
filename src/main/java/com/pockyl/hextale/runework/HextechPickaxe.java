package com.pockyl.hextale.runework;

import java.util.Locale;

/**
 * The hextech pickaxes: the plain one (iron strength), the refined one (diamond), the radiant one (netherite) and the
 * riftforged one (past netherite), each made from the one before (runes carried over). The tier sets what the
 * pickaxe is as a tool and how many {@link PickaxeRune}s it holds ({@link RuneTier}: 1, 2, 3).
 */
public enum HextechPickaxe {
    HEXTECH("hextech_pickaxe"),
    REFINED("refined_hextech_pickaxe"),
    RADIANT("radiant_hextech_pickaxe"),
    /** Level 4, forged with the Void on the Hexcore (owner 2026-10-30). */
    RIFTFORGED("riftforged_pickaxe");

    private final String id;

    HextechPickaxe(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** 1 (iron) to 3 (netherite), 4 past netherite. */
    public int tier() {
        return ordinal() + 1;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}

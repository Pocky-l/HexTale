package com.pockyl.hextale.research;

import net.minecraft.world.item.Item;

import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Research tools: products of the first runes. A tool works while it lies in its slot of the Research Table, and it is
 * as strong as the stars the researcher has on its rune (0-3), so studying the rune again makes the tool better.
 * Texts: {@code research.hextale.tool.<id>} and {@code .level.<1-3>}.
 */
public enum ResearchTool {
    /** Shows the elements of dark runes farther from the ink; at 3 stars all of them. */
    LENS("water_light", ModItems.RESEARCHERS_LENS),
    /** 1-3 times per sheet: the next symbol starts from the orb (from 2 stars also from a filled point). */
    FORK("light_earth", ModItems.FORK_QUILL),
    /** Grinds items up to its strength in tiers: a ground item writes its symbol half as far. */
    MORTAR("water_earth", ModItems.MORTAR),
    /** 1-3 times per sheet: scrapes off the last symbol and gives its place back. */
    SCRAPER("earth_spark", ModItems.BRASS_SCRAPER),
    /** Each sheet holds 1-3 more symbols. */
    STRETCHER("earth_fire", ModItems.PARCHMENT_STRETCHER);

    private final ComplexRune rune;
    private final Supplier<? extends Item> item;

    ResearchTool(String rune, Supplier<? extends Item> item) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(rune));
        this.item = item;
    }

    public ComplexRune rune() {
        return rune;
    }

    public Item item() {
        return item.get();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "research.hextale.tool." + id();
    }
}

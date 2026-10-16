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
    /** Fog lifts farther around the thread: dark runes show sooner; at 3 stars the whole parchment is clear. */
    LENS("water_light", ModItems.RESEARCHERS_LENS),
    /** The thread may cross itself 1-3 times. */
    FORK("light_earth", ModItems.FORK_QUILL),
    /** Ground ink goes further: every 4th, 3rd or 2nd paid section of the thread costs no ink. */
    MORTAR("water_earth", ModItems.MORTAR),
    /** The thread may pass through 1-3 dark runes. */
    SCRAPER("earth_spark", ModItems.BRASS_SCRAPER),
    /** The first 4, 8 or 12 sections of every thread cost no ink. */
    STRETCHER("earth_fire", ModItems.PARCHMENT_STRETCHER);

    private final ComplexRune rune;
    private final Supplier<? extends Item> item;

    ResearchTool(String rune, Supplier<? extends Item> item) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(rune));
        this.item = item;
    }

    /** Fog sight around the thread in sections, by lens strength 0-2 (3 sees everything). */
    public static final int[] SIGHT = {2, 3, 5};

    /** What the fork, scraper, stretcher and mortar of these strengths allow on a thread. */
    public static ResearchSheet.Allowance allowance(int fork, int scraper, int stretcher, int mortar) {
        return new ResearchSheet.Allowance(fork, scraper, stretcher * 4, mortar);
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

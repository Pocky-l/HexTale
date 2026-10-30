package com.pockyl.hextale.runework;

import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.registry.ModTags;

/**
 * How many runes a tool or a worn piece holds grows with its level (owner, 2026-10-30): a first tool holds one rune, a
 * refined one two, a radiant one three and a riftforged one, made from the Void, four. The level comes from item tags
 * ({@link ModTags#REFINED_TIER}, {@link ModTags#RADIANT_TIER}, {@link ModTags#RIFTFORGED_TIER}), so modpacks can place
 * their own items on the ladder. Runes already burned above the limit stay; only new ones are refused.
 */
public final class RuneTier {
    /** The most runes any laddered piece holds: a riftforged one. */
    public static final int MAX = 4;

    private RuneTier() {
    }

    /** 1 (first), 2 (refined), 3 (radiant) or 4 (riftforged). */
    public static int tier(ItemStack stack) {
        if (stack.is(ModTags.RIFTFORGED_TIER)) {
            return 4;
        }
        if (stack.is(ModTags.RADIANT_TIER)) {
            return 3;
        }
        return stack.is(ModTags.REFINED_TIER) ? 2 : 1;
    }

    /** The rune places of this very piece. */
    public static int slots(ItemStack stack) {
        return tier(stack);
    }
}

package com.pockyl.hextale.runework;

import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModTags;

/**
 * How many runes a tool or a worn piece holds grows with how far the researcher has come (owner, 2026-10-30): a first
 * tool holds one rune, a refined one two, a radiant one three, and a radiant piece attuned at the Hexcore with the Void
 * a fourth. The tier comes from item tags ({@link ModTags#REFINED_TIER}, {@link ModTags#RADIANT_TIER}), so modpacks can
 * place their own items on the ladder. Runes already burned above the limit stay; only new ones are refused.
 */
public final class RuneTier {
    /** The most runes any laddered piece holds: a radiant one, attuned. */
    public static final int MAX = 4;

    private RuneTier() {
    }

    /** 1 (first), 2 (refined) or 3 (radiant). */
    public static int tier(ItemStack stack) {
        if (stack.is(ModTags.RADIANT_TIER)) {
            return 3;
        }
        return stack.is(ModTags.REFINED_TIER) ? 2 : 1;
    }

    public static boolean attuned(ItemStack stack) {
        return stack.has(ModDataComponents.ATTUNED.get());
    }

    /** The rune places of this very piece. */
    public static int slots(ItemStack stack) {
        return Math.min(MAX, tier(stack) + (attuned(stack) ? 1 : 0));
    }
}

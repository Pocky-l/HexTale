package com.pockyl.hextale.runework;

import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.rune.ComplexRune;

import org.jspecify.annotations.Nullable;

/** Hextech tools: a tool with rune slots. The runes direct the tool and cost nothing to fire. */
public final class HextechTools {
    /** Rune slots of a hextech tool. */
    public static final int SLOTS = 4;
    /** Runes a tool carries without overloading. */
    public static final int STABLE_RUNES = 2;

    private HextechTools() {
    }

    public static boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof HextechToolItem;
    }

    public static RuneSlots runes(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.RUNE_SLOTS.get(), RuneSlots.EMPTY);
    }

    public static void setRunes(ItemStack stack, RuneSlots runes) {
        if (runes.isEmpty()) {
            stack.remove(ModDataComponents.RUNE_SLOTS.get());
        } else {
            stack.set(ModDataComponents.RUNE_SLOTS.get(), runes);
        }
    }

    /** The engraving of that rune on a hextech tool, or null (also for any other item). */
    public static RuneSlots.@Nullable Engraving engraving(ItemStack stack, ComplexRune rune) {
        return isTool(stack) ? runes(stack).find(rune) : null;
    }

    /**
     * The strength a rune fires with on this tool: its stars, one more when a Lens sits in the slot before it (a
     * focused rune can reach a fourth star).
     */
    public static int strength(ItemStack stack, RuneSlots.Engraving engraving) {
        RuneSlots.Engraving before = runes(stack).get(engraving.slot() - 1);
        return engraving.stars() + (before != null && before.rune() == ToolRunes.LENS ? 1 : 0);
    }
}

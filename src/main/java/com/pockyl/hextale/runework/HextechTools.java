package com.pockyl.hextale.runework;

import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.Essence;

import org.jspecify.annotations.Nullable;

/**
 * Hextech tools: a tool with rune slots and an essence cell. The runes direct the tool, the cell feeds them - every
 * firing of a rune burns its action essence (Smelting = fire > earth burns fire). A rune whose essence ran out simply
 * does not fire; the tool keeps working as a plain tool.
 */
public final class HextechTools {
    /** Rune slots of a hextech tool. */
    public static final int SLOTS = 4;
    /** Runes a tool carries without overloading. */
    public static final int STABLE_RUNES = 2;
    /** Essence cell size, mB. */
    public static final int CELL = 64;

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

    public static EssenceMix cell(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ESSENCE.get(), EssenceMix.EMPTY);
    }

    public static void setCell(ItemStack stack, EssenceMix contents) {
        if (contents.isEmpty()) {
            stack.remove(ModDataComponents.ESSENCE.get());
        } else {
            stack.set(ModDataComponents.ESSENCE.get(), contents);
        }
    }

    public static int space(ItemStack stack) {
        return CELL - cell(stack).total();
    }

    /** How many firings of {@code cost} mB each the cell can pay in that essence. */
    public static int affordable(ItemStack stack, Essence essence, int cost) {
        return cost <= 0 ? Integer.MAX_VALUE : cell(stack).get(essence) / cost;
    }

    /**
     * Burns {@code amount} mB of one essence from the cell.
     *
     * @return false (and nothing spent) when the cell holds less
     */
    public static boolean spend(ItemStack stack, Essence essence, int amount) {
        if (amount <= 0) {
            return true;
        }
        EssenceMix cell = cell(stack);
        if (cell.get(essence) < amount) {
            return false;
        }
        setCell(stack, cell.minus(EssenceMix.pure(essence, amount)));
        return true;
    }

    /**
     * Pours as much of {@code offer} into the cell as fits, keeping its proportions. Unknown essence never goes in: a
     * rune cannot burn what nobody can name.
     *
     * @return the mB taken
     */
    public static int fill(ItemStack stack, EssenceMix offer) {
        if (offer.unknown() > 0) {
            return 0;
        }
        EssenceMix taken = offer.part(Math.min(space(stack), offer.total()));
        if (!taken.isEmpty()) {
            setCell(stack, cell(stack).plus(taken));
        }
        return taken.total();
    }

    /**
     * Fills the cell from a tank.
     *
     * @return the mB taken, -1 when the tank holds unknown essence
     */
    public static int fillFrom(ItemStack stack, EssenceTank tank) {
        if (tank.contents().unknown() > 0) {
            return -1;
        }
        EssenceMix drawn = tank.drain(Math.min(space(stack), tank.amount()), true);
        int taken = fill(stack, drawn);
        if (taken > 0) {
            tank.remove(drawn.part(taken));
        }
        return taken;
    }
}

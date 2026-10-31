package com.pockyl.hextale.port;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Item stack helpers of Minecraft 26.x that 1.21.1 lacks. */
public final class PortItems {
    private PortItems() {
    }

    /** {@code ItemStack#nextDamageWillBreak}: one more point of wear breaks it. */
    public static boolean nextDamageWillBreak(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    /** {@code ItemStack#hurtWithoutBreaking}: wear that stops one point short of breaking (none in creative). */
    public static void hurtWithoutBreaking(ItemStack stack, int amount, Player player) {
        if (!player.hasInfiniteMaterials() && stack.isDamageableItem()) {
            stack.setDamageValue(Math.min(stack.getDamageValue() + amount, stack.getMaxDamage() - 1));
        }
    }
}

package com.pockyl.hextale.port;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;

/** Entity helpers of Minecraft 26.x. */
public final class PortEntities {
    private PortEntities() {
    }

    /** {@code LivingEntity.canGlideUsing(stack, slot)}: the stack glides when worn in that slot. */
    public static boolean canGlideUsing(ItemStack stack, EquipmentSlot slot) {
        return stack.getItem() instanceof Equipable equipable && equipable.getEquipmentSlot() == slot
                && stack.getItem().canElytraFly(stack, null);
    }
}

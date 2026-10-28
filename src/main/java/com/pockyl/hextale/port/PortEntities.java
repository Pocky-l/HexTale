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

    /**
     * 26.x {@code Entity#canSimulateMovement}: whether this side moves the entity itself - a player on its own client,
     * anything else where it is controlled (the server, or the client of the player riding it).
     */
    public static boolean canSimulateMovement(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player player ? player.isLocalPlayer() : entity.isControlledByLocalInstance();
    }
}

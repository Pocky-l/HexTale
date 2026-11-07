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
     * 26.x {@code LivingEntity#dropFromLootTable(level, source, hitByPlayer, table, consumer)}: what the table gives for
     * this entity, handed back instead of dropped (1.21.1's own method only drops).
     */
    public static java.util.List<ItemStack> loot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> table, net.minecraft.world.damagesource.DamageSource source,
            boolean hitByPlayer) {
        var builder = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, entity)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, entity.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
        if (hitByPlayer && source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
        }
        return level.getServer().reloadableRegistries().getLootTable(table)
                .getRandomItems(builder.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY), entity.getLootTableSeed());
    }

    /**
     * 26.x {@code Entity#canSimulateMovement}: whether this side moves the entity itself - a player on its own client,
     * anything else where it is controlled (the server, or the client of the player riding it).
     */
    public static boolean canSimulateMovement(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player player ? player.isLocalPlayer() : entity.isControlledByLocalInstance();
    }
}

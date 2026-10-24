package com.pockyl.hextale.port.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Fills {@link ItemStackRenderState}s like the 26.x resolver; the model itself is looked up when it is drawn.
 *
 * <p>port-of: net.minecraft.client.renderer.item.ItemModelResolver
 */
public class ItemModelResolver {
    public static final ItemModelResolver INSTANCE = new ItemModelResolver();

    public void updateForTopItem(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext,
                                 @Nullable Level level, @Nullable LivingEntity owner, int seed) {
        output.stack = item.copy();
        output.displayContext = displayContext;
        output.level = level;
        output.owner = owner;
        output.seed = seed;
    }

    public void updateForLiving(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, LivingEntity entity) {
        updateForTopItem(output, item, displayContext, entity.level(), entity, entity.getId() + displayContext.ordinal());
    }

    public void updateForNonLiving(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, Entity entity) {
        updateForTopItem(output, item, displayContext, entity.level(), null, entity.getId());
    }
}

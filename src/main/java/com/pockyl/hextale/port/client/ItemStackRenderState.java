package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A 26.x resolved item model; on 1.21.1 it remembers the stack and how to draw it, and {@link #submit} renders it
 * through the item renderer.
 *
 * <p>port-of: net.minecraft.client.renderer.item.ItemStackRenderState
 */
public class ItemStackRenderState {
    ItemStack stack = ItemStack.EMPTY;
    ItemDisplayContext displayContext = ItemDisplayContext.NONE;
    @Nullable Level level;
    @Nullable LivingEntity owner;
    int seed;

    public void clear() {
        stack = ItemStack.EMPTY;
        level = null;
        owner = null;
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public ItemStack stack() {
        return stack;
    }

    public ItemDisplayContext displayContext() {
        return displayContext;
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, int outlineColor) {
        if (stack.isEmpty()) {
            return;
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(owner, stack, displayContext, false, poseStack, collector.buffers(),
                level, lightCoords, overlayCoords, seed);
    }
}

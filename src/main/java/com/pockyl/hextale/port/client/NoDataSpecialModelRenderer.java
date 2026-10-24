package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A special item model that draws the same for every stack.
 *
 * <p>port-of: net.minecraft.client.renderer.special.NoDataSpecialModelRenderer
 */
public interface NoDataSpecialModelRenderer extends SpecialModelRenderer<Void> {
    void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor);

    @Override
    default void submit(@Nullable Void argument, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil,
                        int outlineColor) {
        submit(poseStack, collector, light, overlay, hasFoil, outlineColor);
    }

    @Override
    default @Nullable Void extractArgument(ItemStack stack) {
        return null;
    }

    interface Unbaked extends SpecialModelRenderer.Unbaked<Void> {
        @Override
        @Nullable NoDataSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context);
    }
}

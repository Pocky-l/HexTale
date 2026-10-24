package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A 26.x special item model (code-drawn part of an item model definition); {@link SpecialItemModels} draws it from a
 * block entity without level renderer on 1.21.1.
 *
 * <p>port-of: net.minecraft.client.renderer.special.SpecialModelRenderer
 */
public interface SpecialModelRenderer<T> {
    void submit(@Nullable T argument, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil,
                int outlineColor);

    default void getExtents(Consumer<Vector3fc> output) {
    }

    @Nullable T extractArgument(ItemStack stack);

    interface Unbaked<T> {
        @Nullable SpecialModelRenderer<T> bake(BakingContext context);

        MapCodec<? extends Unbaked<T>> type();
    }

    record BakingContext(EntityModelSet entityModelSet) {
    }
}

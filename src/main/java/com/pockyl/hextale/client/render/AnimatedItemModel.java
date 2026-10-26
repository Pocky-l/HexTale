package com.pockyl.hextale.client.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;

/**
 * Marks an item as animated, so the GUI draws its slot anew every frame instead of keeping the first picture: the
 * GUI caches a special model's drawing until its arguments change, and a moving model has none that change
 * ({@code {"type": "hextale:animated", "model": {...}}} in an item model definition).
 */
public record AnimatedItemModel(ItemModel model) implements ItemModel {
    public static final Identifier ID = Hextale.id("animated");

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext,
                       @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        model.update(output, item, resolver, displayContext, level, owner, seed);
        output.setAnimated();
    }

    public record Unbaked(ItemModel.Unbaked model) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = ItemModels.CODEC.fieldOf("model").xmap(Unbaked::new, Unbaked::model);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new AnimatedItemModel(model.bake(context, transformation));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            model.resolveDependencies(resolver);
        }
    }
}

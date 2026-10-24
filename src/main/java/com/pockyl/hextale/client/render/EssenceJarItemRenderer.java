package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.rune.Essence;

import java.util.function.Consumer;

/**
 * An Essence Jar item shows what it carries: the liquid inside its glass, as high as the jar was full, and its sticker on
 * the sides - the same drawing as the placed jar. The glass is the item's plain model; the item model definition
 * ({@code items/essence_tank.json}) puts this special renderer on top of it.
 */
public final class EssenceJarItemRenderer implements SpecialModelRenderer<EssenceJarItemRenderer.Contents> {
    public static final ResourceLocation ID = Hextale.id("essence_jar");

    /** What the item carries: how full (0-1), the liquid's colour and the sticker. */
    public record Contents(float fill, int color, @Nullable Essence sticker) {
    }

    @Override
    public @Nullable Contents extractArgument(ItemStack stack) {
        EssenceMix essence = stack.get(ModDataComponents.ESSENCE.get());
        Essence sticker = stack.get(ModDataComponents.STICKER.get());
        boolean liquid = essence != null && !essence.isEmpty();
        if (!liquid && sticker == null) {
            return null;
        }
        return new Contents(liquid ? Math.min(1, essence.total() / (float) EssenceTankBlockEntity.CAPACITY) : 0, liquid ? essence.color() : 0,
                sticker);
    }

    @Override
    public void submit(@Nullable Contents contents, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil,
                       int outlineColor) {
        if (contents == null) {
            return;
        }
        @Nullable Essence[] labels = new Essence[Direction.values().length];
        for (Direction face : Direction.Plane.HORIZONTAL) {
            labels[face.get3DDataValue()] = contents.sticker();
        }
        EssenceTankRenderer.submitContents(poseStack, collector, contents.fill(), contents.color(), 0, 0, labels, light);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(2 / 16F, 0, 2 / 16F));
        output.accept(new Vector3f(14 / 16F, 15 / 16F, 14 / 16F));
    }

    /** The JSON side: {@code {"type": "hextale:essence_jar"}} in an item model definition. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Contents> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<Contents> bake(SpecialModelRenderer.BakingContext context) {
            return new EssenceJarItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}

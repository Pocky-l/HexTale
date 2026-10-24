package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.ItemStackRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Draws an item see-through: the quads of its model with a translucent item render type and the alpha folded into
 * every vertex colour (vanilla item rendering is always opaque). Items with a special renderer are drawn as usual.
 *
 * <p>1.21.1: the quads come from the item's baked model and its render passes (26.x reads the resolved layers).
 */
public final class GhostItems {
    private GhostItems() {
    }

    /** Submits the item at the current pose, {@code alpha} 0-1. */
    public static void submit(ItemStackRenderState item, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, float alpha) {
        ItemStack stack = item.stack();
        if (stack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, null, 0);
        if (model.isCustomRenderer()) {
            item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
            return;
        }
        float a = Math.max(0, Math.min(1, alpha));
        VertexConsumer buffer = collector.buffer(RenderType.itemEntityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
        poseStack.pushPose();
        model = ClientHooks.handleCameraTransforms(poseStack, model, item.displayContext(), false);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        RandomSource random = RandomSource.create();
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (Direction side : SIDES) {
                random.setSeed(42L);
                for (BakedQuad quad : pass.getQuads(null, side, random)) {
                    int tint = quad.isTinted() ? minecraft.getItemColors().getColor(stack, quad.getTintIndex()) : -1;
                    buffer.putBulkData(poseStack.last(), quad, (tint >> 16 & 0xFF) / 255F, (tint >> 8 & 0xFF) / 255F,
                            (tint & 0xFF) / 255F, a, lightCoords, OverlayTexture.NO_OVERLAY, true);
                }
            }
        }
        poseStack.popPose();
    }

    private static final Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
}

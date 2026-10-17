package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.BigEssenceJarBlockEntity;

import org.jspecify.annotations.Nullable;

/**
 * The Big Essence Jar: the Essence Jar's own model, liquid and sticker, four times as large. The jar's glass (pixels 2-14
 * of its block) spans the three blocks around the controller, its lid ends 12 pixels into the fourth layer.
 */
public class BigEssenceJarRenderer implements BlockEntityRenderer<BigEssenceJarBlockEntity, EssenceTankRenderer.State> {
    private static final float SCALE = 4;
    /** Moves the small jar's glass (2/16 from its block's corner) onto the big jar's corner, a block beside the controller. */
    private static final float SHIFT = -1 - SCALE * 2 / 16F;

    private final EssenceTankRenderer jar;

    public BigEssenceJarRenderer(BlockEntityRendererProvider.Context context) {
        jar = new EssenceTankRenderer(context);
    }

    @Override
    public EssenceTankRenderer.State createRenderState() {
        return jar.createRenderState();
    }

    @Override
    public void extractRenderState(BigEssenceJarBlockEntity entity, EssenceTankRenderer.State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        jar.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
        // A big jar barely sways when pushed.
        state.tilt *= 0.3F;
    }

    @Override
    public void submit(EssenceTankRenderer.State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(SHIFT, 0, SHIFT);
        poseStack.scale(SCALE, SCALE, SCALE);
        jar.submit(state, poseStack, collector, camera);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BigEssenceJarBlockEntity entity) {
        BlockPos pos = entity.getBlockPos();
        return new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 4, pos.getZ() + 2);
    }
}

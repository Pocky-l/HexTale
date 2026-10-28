package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.BigEssenceJarBlockEntity;

import org.jspecify.annotations.Nullable;

/**
 * The Big Essence Jar: an octagonal glass vat standing in a walnut tub with a brass band, a brass hoop round its middle,
 * frosted shoulders under a wide walnut lid, the glowing liquid inside as high as it is full and the sticker as a paper
 * tag tied under the lid. The vat is centred on the controller (the middle of the 3 x 3 floor) and stands four blocks
 * tall, so its model comes in two halves (a model ends 32 px over its block): the upper one is drawn two blocks up.
 */
public class BigEssenceJarRenderer implements BlockEntityRenderer<BigEssenceJarBlockEntity, EssenceTankRenderer.State> {
    /** The liquid: an octagon a quarter pixel inside the glass (apothem 20 px round the controller's middle), over the tub floor. */
    private static final float APOTHEM = 19.75F / 16;
    private static final float MIN = 0.5F - APOTHEM;
    private static final float MAX = 0.5F + APOTHEM;
    /** Corner cut of a regular octagon: apothem * (1 - tan 22.5 deg). */
    private static final float CUT = APOTHEM * (1 - 0.41421356F);
    private static final float BOTTOM = 4.25F / 16;
    private static final float TOP = 43.75F / 16;
    private static final int BUBBLES = 8;
    /**
     * The tag: 2.5 texels per pixel (8 px wide), hanging 1 px in front of the glass with its eyelet under the shoulder;
     * its twine runs up over the frosted shoulder to the tie under the lid.
     */
    private static final EssenceTankRenderer.Tag TAG = new EssenceTankRenderer.Tag(2.5F, 21F / 16, 43.5F / 16, 13.2F / 16, 49.4F / 16);
    /** Where the model's upper half stands. */
    private static final float UPPER_Y = 2;

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
        EssenceTankRenderer.rock(poseStack, state);
        TableModels.submit(poseStack, collector, TableModels.BIG_JAR_LOWER, state.lightCoords);
        poseStack.pushPose();
        poseStack.translate(0, UPPER_Y, 0);
        TableModels.submit(poseStack, collector, TableModels.BIG_JAR_UPPER, state.lightCoords);
        poseStack.popPose();
        if (state.fill > 0) {
            // The surface breathes a little, unless the vat is brim-full.
            float wave = state.fill < 0.98F ? Mth.sin(state.time * 0.06F + state.seed % 7) * 0.2F / 16 : 0;
            float top = Math.min(TOP, BOTTOM + (TOP - BOTTOM) * Math.min(1, state.fill) + wave);
            LiquidBox.submitCutCorners(poseStack, collector, MIN, MIN, MAX, MAX, CUT, BOTTOM, top,
                    RuneDrawing.argb(EssenceTankRenderer.vivid(state.color), 0.9F), state.time, state.seed, BUBBLES);
        }
        EssenceTankRenderer.stickers(poseStack, collector, state.labels, state.lightCoords, TAG);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BigEssenceJarBlockEntity entity) {
        BlockPos pos = entity.getBlockPos();
        return new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 4, pos.getZ() + 2);
    }
}

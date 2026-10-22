package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexcoreBlockEntity;

/**
 * The Hexcore's rune matrix floats a block and a half over its cradle, slowly turning and tumbling, with the crystal's
 * light showing between the tiles.
 */
public class HexcoreRenderer implements BlockEntityRenderer<HexcoreBlockEntity, HexcoreRenderer.State> {
    /** Height of the matrix centre over the block's floor, in blocks (model y 24). */
    public static final float CENTRE = 1.5F;

    public HexcoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HexcoreBlockEntity core, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(core, state, partialTick, cameraPosition, breakProgress);
        state.time = HexFx.time(core.getLevel(), partialTick) + (int) (core.getBlockPos().asLong() & 255);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        float breath = 0.5F + 0.5F * Mth.sin(time * 0.06F);
        poseStack.pushPose();
        poseStack.translate(0.5, CENTRE + Mth.sin(time * 0.045F) * 0.04F, 0.5);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.9F + 0.15F * breath, 0xBFEFFF, 0.9F);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 2.4F + 0.3F * breath, 0x5FA8F0, 0.18F + 0.08F * breath);
        poseStack.rotate(Axis.YP.rotationDegrees(time * 0.5F));
        poseStack.rotate(Axis.XP.rotationDegrees(14 + Mth.sin(time * 0.013F) * 10));
        poseStack.rotate(Axis.ZP.rotationDegrees(Mth.sin(time * 0.009F) * 12));
        poseStack.translate(-0.5, -0.5, -0.5);
        TableModels.submit(poseStack, collector, TableModels.HEXCORE_MATRIX, LightCoordsUtil.FULL_BRIGHT);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HexcoreBlockEntity core) {
        return new AABB(core.getBlockPos()).inflate(0.3, 0, 0.3).expandTowards(0, 1.3, 0);
    }

    public static final class State extends BlockEntityRenderState {
        float time;
    }
}

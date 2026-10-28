package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.LevitationPlateBlockEntity;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.PlateRune;

/**
 * The Levitation Plate's floating ring: it turns slowly over the dais and bobs; while it carries someone it turns
 * faster, glows, and rings of light rise from it up the column to the height it lifts to, where a ring marks the top.
 */
public class LevitationPlateRenderer implements BlockEntityRenderer<LevitationPlateBlockEntity, LevitationPlateRenderer.State> {
    /** The ring's pivot (8, 6, 8) px. */
    private static final float PIVOT_Y = 6 / 16F;
    private static final float BOB = 0.5F / 16;
    /** Rings rising up the column at once while it carries someone. */
    private static final int RISING = 3;

    public LevitationPlateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LevitationPlateBlockEntity plate, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(plate, state, partialTick, cameraPosition, breakProgress);
        state.time = HexFx.time(plate.getLevel(), partialTick) + (plate.getBlockPos().asLong() & 63);
        state.spin = Mth.lerp(partialTick, plate.spinO, plate.spin);
        state.lift = Mth.lerp(partialTick, plate.liftO, plate.lift);
        state.reach = plate.getLevel() == null ? PlateRune.HEIGHT : plate.reach(plate.getLevel());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float bob = Mth.sin(state.time * 0.08F) * BOB * (1 + state.lift);
        poseStack.pushPose();
        poseStack.translate(0.5F, PIVOT_Y + bob + state.lift * 0.03F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        float pulse = 1 + state.lift * 0.04F * Mth.sin(state.time * 0.4F);
        poseStack.scale(pulse, 1, pulse);
        poseStack.translate(-0.5F, -PIVOT_Y, -0.5F);
        TableModels.submit(poseStack, collector, TableModels.LEVITATION_RING, state.lightCoords);
        poseStack.popPose();
        if (state.lift < 0.02F) {
            return;
        }
        // A glow in the ring, and rings of light climbing the column to its top.
        poseStack.pushPose();
        poseStack.translate(0.5F, PIVOT_Y + bob, 0.5F);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.9F, ModParticles.HEXTECH, 0.35F * state.lift);
        poseStack.popPose();
        float height = state.reach + 0.4F;
        for (int i = 0; i < RISING; i++) {
            float t = ((state.time * 0.02F) + i / (float) RISING) % 1;
            float y = PIVOT_Y + t * height;
            float alpha = state.lift * 0.5F * Mth.sin(t * Mth.PI);
            poseStack.pushPose();
            poseStack.translate(0.5F, y, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.spin * 0.5F + i * 40));
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            HexFx.sigilRing(poseStack, collector, 0.42F - t * 0.08F, 6, ModParticles.HEXTECH, alpha);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 1 + state.reach - 0.05F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.spin * 0.7F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        HexFx.sigilRing(poseStack, collector, 0.36F, 8, ModParticles.HEX_LIGHT, 0.45F * state.lift);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LevitationPlateBlockEntity plate) {
        return new AABB(plate.getBlockPos()).expandTowards(0, PlateRune.HEIGHT + 19, 0);
    }

    public static final class State extends BlockEntityRenderState {
        float time;
        float spin;
        float lift;
        int reach;
    }
}

package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.ChemBoilerBlock;
import com.pockyl.hextale.block.ChemBoilerBlockEntity;

import org.jspecify.annotations.Nullable;

/**
 * The moving parts of a Chem Boiler: the gauge needle on its front sweeps from empty (left) to full (right) with the
 * pressure and trembles while the steam drives, and the safety valve on top jumps on every knock. Both are standalone
 * models built in the north-facing block's space and turned with the block.
 */
public class ChemBoilerRenderer implements BlockEntityRenderer<ChemBoilerBlockEntity, ChemBoilerRenderer.State> {
    /** Where the needle turns, in model pixels (the gauge's centre, just in front of its face). */
    public static final float[] NEEDLE_PIVOT = {8, 10, 1.5F};
    /** The needle's sweep either way from straight up, in degrees. */
    private static final float SWEEP = 120;
    /** How high the valve jumps on a knock, in blocks, and for how many ticks. */
    private static final float VALVE_LIFT = 1.5F / 16;
    private static final float KNOCK_TICKS = 8;

    public static final class State extends BlockEntityRenderState {
        float needle;
        float lift;
        float time;
        boolean driving;
        Direction facing = Direction.NORTH;
    }

    public ChemBoilerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChemBoilerBlockEntity boiler, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(boiler, state, partialTick, cameraPosition, breakProgress);
        Level level = boiler.getLevel();
        state.time = level == null ? 0 : level.getGameTime() + partialTick;
        state.needle = Mth.lerp(partialTick, boiler.needleBefore, boiler.needle);
        float since = level == null ? KNOCK_TICKS : level.getGameTime() - boiler.knockedAt + partialTick;
        // Up at once, settling back over the next ticks.
        state.lift = since >= KNOCK_TICKS ? 0 : VALVE_LIFT * (1 - since / KNOCK_TICKS) * (1 - since / KNOCK_TICKS);
        state.driving = boiler.getBlockState().getValue(ChemBoilerBlock.DRIVING);
        state.facing = boiler.getBlockState().getValue(ChemBoilerBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, state.facing);

        poseStack.pushPose();
        float angle = -SWEEP + 2 * SWEEP * Mth.clamp(state.needle, 0, 1);
        if (state.driving) {
            angle += Mth.sin(state.time * 2.3F) * 2.5F + Mth.sin(state.time * 5.1F) * 1.2F;
        }
        poseStack.translate(NEEDLE_PIVOT[0] / 16, NEEDLE_PIVOT[1] / 16, NEEDLE_PIVOT[2] / 16);
        poseStack.rotate(Axis.ZP.rotationDegrees(angle));
        poseStack.translate(-NEEDLE_PIVOT[0] / 16, -NEEDLE_PIVOT[1] / 16, -NEEDLE_PIVOT[2] / 16);
        TableModels.submit(poseStack, collector, TableModels.CHEM_BOILER_NEEDLE, state.lightCoords);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, state.lift, 0);
        TableModels.submit(poseStack, collector, TableModels.CHEM_BOILER_VALVE, state.lightCoords);
        poseStack.popPose();

        poseStack.popPose();
    }
}

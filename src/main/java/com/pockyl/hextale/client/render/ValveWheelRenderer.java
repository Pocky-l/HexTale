package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pockyl.hextale.block.ValveWheelBlock;
import com.pockyl.hextale.block.ValveWheelBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/** The Wall Valve Wheel's wheel, turned about the spindle through (8, 8) px of the north-facing model. */
public class ValveWheelRenderer implements BlockEntityRenderer<ValveWheelBlockEntity, ValveWheelRenderer.State> {
    private static final float CENTRE = 0.5F;

    public static final class State extends BlockEntityRenderState {
        float angle;
        Direction facing = Direction.NORTH;
    }

    public ValveWheelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ValveWheelBlockEntity wheel, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(wheel, state, partialTick, cameraPosition, breakProgress);
        state.angle = wheel.angle(partialTick);
        state.facing = wheel.getBlockState().getValue(ValveWheelBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, state.facing);
        poseStack.translate(CENTRE, CENTRE, 0);
        poseStack.rotate(Axis.ZP.rotationDegrees(state.angle));
        poseStack.translate(-CENTRE, -CENTRE, 0);
        TableModels.submit(poseStack, collector, TableModels.VALVE_WHEEL_HANDLE, state.lightCoords);
        poseStack.popPose();
    }
}

package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.LapidaryBlock;
import com.pockyl.hextale.block.LapidaryBlockEntity;

/**
 * The lapidary at work: the lap disc (its own model) turns on its spindle while someone works at the bench, and the
 * crystal in the clamp hangs just over it, turning slowly.
 */
public class LapidaryRenderer implements BlockEntityRenderer<LapidaryBlockEntity, LapidaryRenderer.State> {
    /** The disc's spindle in model pixels (the disc model is built around it). */
    private static final double SPINDLE_X = 8 / 16.0;
    private static final double SPINDLE_Z = 7 / 16.0;
    private static final double CRYSTAL_Y = 15.5 / 16.0;
    private final ItemModelResolver itemModelResolver;

    public LapidaryRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LapidaryBlockEntity lapidary, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(lapidary, state, partialTick, cameraPosition, breakProgress);
        Level level = lapidary.getLevel();
        state.facing = lapidary.getBlockState().getValue(LapidaryBlock.FACING);
        state.working = lapidary.getBlockState().getValue(LapidaryBlock.WORKING);
        state.disc = !lapidary.getItem(LapidaryBlockEntity.DISC).isEmpty();
        state.time = HexFx.time(level, partialTick);
        itemModelResolver.updateForTopItem(state.crystal, lapidary.getItem(LapidaryBlockEntity.INPUT), ItemDisplayContext.GROUND, level, null,
                (int) lapidary.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, state.facing);
        if (state.disc) {
            poseStack.pushPose();
            poseStack.translate(SPINDLE_X, 0, SPINDLE_Z);
            poseStack.rotate(Axis.YP.rotationDegrees(state.working ? state.time * 18 : 0));
            poseStack.translate(-SPINDLE_X, 0, -SPINDLE_Z);
            TableModels.submit(poseStack, collector, TableModels.LAPIDARY_DISC, state.lightCoords);
            poseStack.popPose();
        }
        if (!state.crystal.isEmpty()) {
            poseStack.translate(SPINDLE_X, CRYSTAL_Y, SPINDLE_Z);
            poseStack.rotate(Axis.YP.rotationDegrees(state.time * 2));
            poseStack.scale(0.45F, 0.45F, 0.45F);
            state.crystal.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState crystal = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        boolean working;
        boolean disc;
        float time;
    }
}

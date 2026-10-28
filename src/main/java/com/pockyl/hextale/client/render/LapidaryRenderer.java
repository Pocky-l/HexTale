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
 * The lapidary at work: the grinding wheel on the bench (with the cutting disc mounted on its side when there is one)
 * and, under the bench, the treadle's flywheel that drives it by the belt (their own models) turn while someone works at
 * the bench; the crystal sits in the clamp in front of the wheel, turning slowly as its facets are ground.
 */
public class LapidaryRenderer implements BlockEntityRenderer<LapidaryBlockEntity, LapidaryRenderer.State> {
    /** The wheel's axle and the flywheel's, in model pixels (both run along x; the models are built round them). */
    private static final double WHEEL_Y = 17 / 16.0;
    private static final double WHEEL_Z = 9 / 16.0;
    private static final double FLYWHEEL_Y = 5 / 16.0;
    private static final double FLYWHEEL_Z = 9 / 16.0;
    /** Degrees a tick; the belt runs from the flywheel (5 px) to the wheel's pulley (3 px), so the wheel turns faster. */
    private static final float WHEEL_SPEED = 24;
    private static final float FLYWHEEL_SPEED = WHEEL_SPEED * 3 / 5;
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
        // The wheel's top runs towards the clamp in front of it.
        poseStack.pushPose();
        turnAboutX(poseStack, WHEEL_Y, WHEEL_Z, state.working ? -state.time * WHEEL_SPEED : 0);
        TableModels.submit(poseStack, collector, TableModels.LAPIDARY_WHEEL, state.lightCoords);
        if (state.disc) {
            TableModels.submit(poseStack, collector, TableModels.LAPIDARY_DISC, state.lightCoords);
        }
        poseStack.popPose();
        poseStack.pushPose();
        turnAboutX(poseStack, FLYWHEEL_Y, FLYWHEEL_Z, state.working ? -state.time * FLYWHEEL_SPEED : 0);
        TableModels.submit(poseStack, collector, TableModels.LAPIDARY_GEAR, state.lightCoords);
        poseStack.popPose();
        if (!state.crystal.isEmpty()) {
            Vec3 at = LapidaryBlockEntity.CRYSTAL;
            poseStack.translate(at.x / 16, at.y / 16, at.z / 16);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 2));
            poseStack.scale(0.3F, 0.3F, 0.3F);
            state.crystal.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        poseStack.popPose();
    }

    private static void turnAboutX(PoseStack poseStack, double y, double z, float degrees) {
        poseStack.translate(0, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(degrees));
        poseStack.translate(0, -y, -z);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState crystal = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        boolean working;
        boolean disc;
        float time;
    }
}

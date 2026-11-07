package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HerdkeeperBlock;
import com.pockyl.hextale.block.HerdkeeperBlockEntity;
import com.pockyl.hextale.rune.Essence;

/**
 * The Herdkeeper at work. The active model leaves the bell out, and this draws it swinging along Z about its hook; the idle
 * model holds a still bell, so nothing is drawn then. Tending an animal shows a stream of life running from it to the trough.
 */
public class HerdkeeperRenderer implements BlockEntityRenderer<HerdkeeperBlockEntity, HerdkeeperRenderer.State> {
    /** The bell's hook in block space, in the north-facing model. */
    private static final double PIVOT_X = 7 / 16.0;
    private static final double PIVOT_Y = 25 / 16.0;
    private static final double PIVOT_Z = 8 / 16.0;
    private static final float SWING_DEGREES = 15;
    private static final float SWING_TICKS = 36;

    public HerdkeeperRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HerdkeeperBlockEntity keeper, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(keeper, state, partialTick, cameraPosition, breakProgress);
        Level level = keeper.getLevel();
        BlockPos pos = keeper.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        int seed = (int) pos.asLong();
        state.time = HexFx.time(level, partialTick) + (seed & 127);
        state.active = keeper.getBlockState().getValue(HerdkeeperBlock.ACTIVE);
        state.facing = keeper.getBlockState().getValue(HerdkeeperBlock.FACING);
        state.stream = null;
        Vec3 animal = keeper.streamFrom();
        if (animal != null && level != null) {
            float age = level.getGameTime() + partialTick - keeper.tendedAt();
            state.stream = new JarStreamFx.Stream(animal.subtract(origin), HerdkeeperBlockEntity.trough(pos).subtract(origin), Essence.LIFE.color(),
                    age / 12F, (age - (HerdkeeperBlockEntity.TEND_TICKS - 12)) / 12F, seed);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.active) {
            float swing = Mth.sin(state.time * Mth.TWO_PI / SWING_TICKS) * SWING_DEGREES;
            poseStack.pushPose();
            TableModels.rotateToFacing(poseStack, state.facing);
            poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
            poseStack.mulPose(Axis.XP.rotationDegrees(swing));
            poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
            TableModels.submit(poseStack, collector, TableModels.HERDKEEPER_BELL, state.lightCoords);
            TableModels.submit(poseStack, collector, TableModels.HERDKEEPER_BELL_GLOW, LightCoordsUtil.FULL_BRIGHT);
            poseStack.popPose();
        }
        if (state.stream != null) {
            Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
            JarStreamFx.submit(poseStack, collector, camera.orientation, eye, state.stream, state.time);
        }
    }

    @Override
    public AABB getRenderBoundingBox(HerdkeeperBlockEntity keeper) {
        BlockPos pos = keeper.getBlockPos();
        AABB box = new AABB(pos).expandTowards(0, 1, 0).inflate(0.6);
        Vec3 animal = keeper.streamFrom();
        return animal == null ? box : box.minmax(new AABB(animal, animal).inflate(1));
    }

    public static final class State extends BlockEntityRenderState {
        float time;
        boolean active;
        Direction facing = Direction.NORTH;
        JarStreamFx.@Nullable Stream stream;
    }
}

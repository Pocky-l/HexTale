package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.ChemStillBlock;
import com.pockyl.hextale.block.ChemStillBlockEntity;
import com.pockyl.hextale.chem.Brew;

import org.jspecify.annotations.Nullable;

/**
 * The liquids of a Chem Still, seen through its glass: violet raw shimmer in the boiler's sight glass, falling as the
 * boiler empties, and magenta refined shimmer rising in the receiver as vials collect there. Coordinates are those of
 * the north-facing model, turned with the block.
 */
public class ChemStillRenderer implements BlockEntityRenderer<ChemStillBlockEntity, ChemStillRenderer.State> {
    // Receiver flask (inside the glass), front right of the north-facing model.
    private static final float RECEIVER_MIN_X = 2.6F / 16;
    private static final float RECEIVER_MAX_X = 5.4F / 16;
    private static final float RECEIVER_MIN_Z = 3.6F / 16;
    private static final float RECEIVER_MAX_Z = 6.4F / 16;
    private static final float RECEIVER_BOTTOM = 1.1F / 16;
    private static final float RECEIVER_TOP = 5.4F / 16;
    // The boiler's sight glass on its front.
    private static final float SIGHT_MIN_X = 8.4F / 16;
    private static final float SIGHT_MAX_X = 11.6F / 16;
    private static final float SIGHT_Z = 4.3F / 16;
    private static final float SIGHT_BOTTOM = 2.2F / 16;
    private static final float SIGHT_TOP = 7.8F / 16;
    private static final int RAW = 0xE0000000 | Brew.RAW_SHIMMER.color();
    private static final int REFINED = 0xE0000000 | Brew.REFINED_SHIMMER.color();

    public static final class State extends BlockEntityRenderState {
        float raw;
        float refined;
        float time;
        long seed;
        boolean working;
        Direction facing = Direction.NORTH;
    }

    public ChemStillRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChemStillBlockEntity still, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(still, state, partialTick, cameraPosition, breakProgress);
        Level level = still.getLevel();
        state.time = level == null ? 0 : level.getGameTime() + partialTick;
        state.seed = still.getBlockPos().asLong();
        state.raw = ChemStillBlockEntity.isRaw(still.input()) ? still.input().getCount() / 16.0F : 0;
        state.refined = still.output().getCount() / 16.0F;
        state.working = still.getBlockState().getValue(ChemStillBlock.WORKING);
        state.facing = still.getBlockState().getValue(ChemStillBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(state.facing.toYRot() + 180)));
        poseStack.translate(-0.5, 0, -0.5);
        if (state.refined > 0) {
            float top = RECEIVER_BOTTOM + (RECEIVER_TOP - RECEIVER_BOTTOM) * Math.min(1, 0.08F + state.refined);
            LiquidBox.submit(poseStack, collector, RECEIVER_MIN_X, RECEIVER_MIN_Z, RECEIVER_MAX_X, RECEIVER_MAX_Z, RECEIVER_BOTTOM, top,
                    REFINED, state.time, state.seed, state.working ? 2 : 0);
        }
        if (state.raw > 0) {
            // A thin slab just behind the sight glass: the boiler's level as a gauge.
            float top = SIGHT_BOTTOM + (SIGHT_TOP - SIGHT_BOTTOM) * Math.min(1, state.raw);
            LiquidBox.submit(poseStack, collector, SIGHT_MIN_X, SIGHT_Z, SIGHT_MAX_X, SIGHT_Z + 0.5F / 16, SIGHT_BOTTOM, top, RAW,
                    state.time, state.seed + 1, 0);
        }
        poseStack.popPose();
    }
}

package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.ChemBoilerBlock;
import com.pockyl.hextale.block.ChemBoilerBlockEntity;
import com.pockyl.hextale.essence.JarReach;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The moving parts of a Chem Boiler: the gauge needle on its front sweeps from empty (left) to full (right) with the
 * pressure and trembles while it works, and the safety valve on top jumps on every knock (both standalone models built
 * in the north-facing block's space and turned with the block). Streams of essence run from a jar it draws a mixture
 * out of into its top, and from its top into every jar it pours the cracked essences into, each in its own colour.
 */
public class ChemBoilerRenderer implements BlockEntityRenderer<ChemBoilerBlockEntity, ChemBoilerRenderer.State> {
    /** Where the needle turns, in model pixels (the gauge's centre, just in front of its face). */
    public static final float[] NEEDLE_PIVOT = {8.5F, 8.5F, 0.9F};
    /** The needle's sweep either way from straight up, in degrees. */
    private static final float SWEEP = 120;
    /** How high the valve jumps on a knock, in blocks, and for how many ticks. */
    private static final float VALVE_LIFT = 1.5F / 16;
    private static final float KNOCK_TICKS = 8;
    /** The inside of the sight tube on the east side (model pixels, north-facing block) and its liquid's alpha. */
    private static final float TUBE_MIN_X = 14.25F / 16;
    private static final float TUBE_MAX_X = 15.75F / 16;
    private static final float TUBE_MIN_Z = 7.25F / 16;
    private static final float TUBE_MAX_Z = 8.75F / 16;
    private static final float TUBE_BOTTOM = 6.05F / 16;
    private static final float TUBE_TOP = 10.95F / 16;
    private static final float TUBE_ALPHA = 0.85F;

    public static final class State extends BlockEntityRenderState {
        float needle;
        float lift;
        float time;
        boolean working;
        /** How full the vessel is (0-1) and the mixture's colour, shown in the sight tube. */
        float fill;
        int color;
        Direction facing = Direction.NORTH;
        final List<JarStreamFx.Stream> streams = new ArrayList<>();
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
        state.time = HexFx.time(level, partialTick);
        state.needle = Mth.lerp(partialTick, boiler.needleBefore, boiler.needle);
        float since = level == null ? KNOCK_TICKS : level.getGameTime() - boiler.knockedAt + partialTick;
        // Up at once, settling back over the next ticks.
        state.lift = since >= KNOCK_TICKS ? 0 : VALVE_LIFT * (1 - since / KNOCK_TICKS) * (1 - since / KNOCK_TICKS);
        state.working = boiler.getBlockState().getValue(ChemBoilerBlock.WORKING);
        state.facing = boiler.getBlockState().getValue(ChemBoilerBlock.FACING);
        state.fill = Mth.clamp(boiler.mixture().total() / (float) ChemBoilerBlockEntity.VESSEL, 0, 1);
        state.color = boiler.mixture().color();
        state.streams.clear();
        if (level == null) {
            return;
        }
        BlockPos pos = boiler.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        Vec3 inlet = ChemBoilerBlock.inlet(pos).subtract(origin);
        int seed = (int) pos.asLong();
        float now = level.getGameTime() + partialTick;
        BlockPos jar = boiler.drawing();
        if (jar != null) {
            int color = JarReach.color(level, new JarReach.Draw(jar, null));
            float age = now - boiler.drewAt();
            if (color != -1) {
                state.streams.add(new JarStreamFx.Stream(JarReach.mouth(jar).subtract(origin), inlet, color, age / 12F,
                        (age - (ChemBoilerBlockEntity.STREAM_TICKS - 14)) / 12F, seed));
            }
        }
        float poured = now - boiler.pouredAt();
        int i = 1;
        for (JarReach.Draw pour : boiler.pouring()) {
            int color = pour.essence() == null ? ChemBoilerBlock.STEAM : pour.essence().color();
            state.streams.add(new JarStreamFx.Stream(inlet, JarReach.mouth(pour.jar()).subtract(origin), color, poured / 12F,
                    (poured - (ChemBoilerBlockEntity.STREAM_TICKS - 14)) / 12F, seed + i++ * 977));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, state.facing);

        poseStack.pushPose();
        float angle = -SWEEP + 2 * SWEEP * Mth.clamp(state.needle, 0, 1);
        if (state.working) {
            angle += Mth.sin(state.time * 2.3F) * 2.5F + Mth.sin(state.time * 5.1F) * 1.2F;
        }
        poseStack.translate(NEEDLE_PIVOT[0] / 16, NEEDLE_PIVOT[1] / 16, NEEDLE_PIVOT[2] / 16);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.translate(-NEEDLE_PIVOT[0] / 16, -NEEDLE_PIVOT[1] / 16, -NEEDLE_PIVOT[2] / 16);
        TableModels.submit(poseStack, collector, TableModels.CHEM_BOILER_NEEDLE, state.lightCoords);
        poseStack.popPose();

        // The mixture stands in the sight tube as high as the vessel is full, trembling while it boils.
        if (state.fill > 0.01F) {
            float tremble = state.working ? Mth.sin(state.time * 0.9F) * 0.003F : 0;
            float top = TUBE_BOTTOM + (TUBE_TOP - TUBE_BOTTOM) * state.fill + tremble;
            LiquidBox.submit(poseStack, collector, TUBE_MIN_X, TUBE_MIN_Z, TUBE_MAX_X, TUBE_MAX_Z, TUBE_BOTTOM, top,
                    RuneDrawing.argb(state.color, TUBE_ALPHA), state.time, state.blockPos.asLong(), state.working ? 1 : 0);
        }

        poseStack.pushPose();
        poseStack.translate(0, state.lift, 0);
        TableModels.submit(poseStack, collector, TableModels.CHEM_BOILER_VALVE, state.lightCoords);
        poseStack.popPose();

        poseStack.popPose();

        if (!state.streams.isEmpty()) {
            Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
            for (JarStreamFx.Stream stream : state.streams) {
                JarStreamFx.submit(poseStack, collector, camera.orientation, eye, stream, state.time);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(ChemBoilerBlockEntity boiler) {
        // The streams reach the jars around it.
        return new AABB(boiler.getBlockPos()).inflate(JarReach.RADIUS + 1);
    }
}

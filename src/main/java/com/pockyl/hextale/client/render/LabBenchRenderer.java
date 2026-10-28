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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.LabBenchBlock;
import com.pockyl.hextale.block.LabBenchBlockEntity;
import com.pockyl.hextale.block.TableGeometry;
import com.pockyl.hextale.chem.Flask;
import com.pockyl.hextale.essence.JarReach;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The Lab Bench in the world: the liquid in its flask (its mixed colour, as full as the flask is, bubbling hot or while
 * something forms) and the streams of essence running from the jars around into the flask's mouth as drops are let in.
 * When a new compound begins to form, the flask flashes in its colour: a glow and a four-pointed glint flare in the
 * belly and a flat hexagon snaps open round it and fades.
 * Coordinates are those of the north-facing model, turned with the block.
 */
public class LabBenchRenderer implements BlockEntityRenderer<LabBenchBlockEntity, LabBenchRenderer.State> {
    // The inside of the flask's belly on the north-facing model.
    private static final float MIN_X = 4.4F / 16;
    private static final float MAX_X = 10.6F / 16;
    private static final float MIN_Z = 4.4F / 16;
    private static final float MAX_Z = 10.6F / 16;
    private static final float BOTTOM = 16.2F / 16;
    private static final float TOP = 19.8F / 16;
    /** The middle of the flask's belly on the north-facing model, in pixels. */
    private static final Vec3 BELLY = new Vec3(7.5, 17.6, 7.5);
    private static final float FLASH_TICKS = 26;

    public static final class State extends BlockEntityRenderState {
        float level;
        int color;
        int bubbles;
        float time;
        long seed;
        boolean flask;
        Direction facing = Direction.NORTH;
        float sinceFlash = -1;
        int flashColor;
        final List<JarStreamFx.Stream> streams = new ArrayList<>();
    }

    public LabBenchRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LabBenchBlockEntity bench, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(bench, state, partialTick, cameraPosition, breakProgress);
        Level level = bench.getLevel();
        state.time = level == null ? 0 : level.getGameTime() + partialTick;
        state.seed = bench.getBlockPos().asLong();
        state.facing = bench.getBlockState().getValue(LabBenchBlock.FACING);
        Flask flask = bench.flask();
        state.flask = bench.hasFlask();
        state.level = Math.min(1, flask.total() / Flask.CAPACITY);
        state.color = 0xE0000000 | flask.color();
        boolean forming = level != null && bench.formed() != null && level.getGameTime() - bench.formedAt() < 20;
        state.bubbles = forming ? 4 : flask.temperature() > 45 ? 2 : 0;
        state.sinceFlash = level == null || bench.formed() == null ? -1 : state.time - bench.flashAt();
        state.flashColor = bench.formed() == null ? 0 : JarStreamFx.vivid(bench.formed().color());
        state.streams.clear();
        float age = bench.sinceDrawn() + partialTick;
        Vec3 origin = Vec3.atLowerCornerOf(bench.getBlockPos());
        Vec3 mouth = bench.mouthPosition().subtract(origin);
        for (JarReach.Draw draw : bench.draws()) {
            int color = draw.essence() == null ? flask.color() : draw.essence().color();
            state.streams.add(new JarStreamFx.Stream(JarReach.mouth(draw.jar()).subtract(origin), mouth, color, age / 8,
                    (age - 22) / 12, (int) draw.jar().asLong()));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.flask && state.level > 0) {
            poseStack.pushPose();
            TableModels.rotateToFacing(poseStack, state.facing);
            float top = BOTTOM + (TOP - BOTTOM) * Math.max(0.06F, state.level);
            LiquidBox.submit(poseStack, collector, MIN_X, MIN_Z, MAX_X, MAX_Z, BOTTOM, top, state.color, state.time, state.seed, state.bubbles);
            poseStack.popPose();
        }
        if (state.sinceFlash >= 0 && state.sinceFlash < FLASH_TICKS) {
            flash(state, poseStack, collector, camera);
        }
        if (!state.streams.isEmpty()) {
            Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
            for (JarStreamFx.Stream stream : state.streams) {
                JarStreamFx.submit(poseStack, collector, camera.orientation, eye, stream, state.time);
            }
        }
    }

    /** A new compound: the belly flares in its colour (peak in the first beats), a hexagon snaps open round the flask. */
    private static void flash(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float age = state.sinceFlash;
        float peak = Math.max(0, 1 - Math.abs(age - 3) / 3);
        float fade = age < 3 ? age / 3 : 1 - (age - 3) / (FLASH_TICKS - 3);
        int color = state.flashColor;
        Vec3 belly = TableGeometry.toWorld(BlockPos.ZERO, state.facing, BELLY);
        poseStack.pushPose();
        poseStack.translate(belly.x, belly.y, belly.z);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.35F + 0.25F * peak, color, 0.25F + 0.5F * fade * (0.5F + peak));
        HexFx.sheet(poseStack, collector, camera.orientation, HexFx.GLINT, HexFx.GLINT_FRAMES, List.of(HexFx.quad(0, 0, 0,
                0.05F + 0.1F * peak, 0, age * 0.08F, Math.min(1, fade * 1.2F), RuneDrawing.mix(color, 0xFFFFFF, 0.6F))));
        float open = 1 - (float) Math.pow(1 - Math.min(1, age / 5), 3);
        poseStack.rotate(Axis.XP.rotationDegrees(90));
        poseStack.rotate(Axis.ZP.rotationDegrees(state.time * 1.5F));
        HexFx.hexRing(poseStack, collector, 0.25F + 0.2F * open, color, 0.8F * fade);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LabBenchBlockEntity bench) {
        BlockPos pos = bench.getBlockPos();
        return new AABB(pos).inflate(JarReach.RADIUS + 1);
    }
}

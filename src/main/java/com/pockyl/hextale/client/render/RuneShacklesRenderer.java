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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.RuneShacklesBlockEntity;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

/**
 * Rune Shackles holding creatures: under each held one a glyph of shadow turns on the plate, and four threads of light
 * run from the plate's brass studs up to its middle, pulsing slowly - the creature hangs still in them.
 */
public class RuneShacklesRenderer implements BlockEntityRenderer<RuneShacklesBlockEntity, RuneShacklesRenderer.State> {
    private static final int SHADOW = Essence.SHADOW.color();
    /** The brass studs on the top face the threads leave from (block space). */
    private static final double[][] STUDS = {{2 / 16.0, 2 / 16.0}, {14 / 16.0, 2 / 16.0}, {2 / 16.0, 14 / 16.0}, {14 / 16.0, 14 / 16.0}};
    /** The top face, a hair over it so the glyph never sinks into the stone. */
    private static final double TOP = 1.02;

    public RuneShacklesRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RuneShacklesBlockEntity plate, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(plate, state, partialTick, cameraPosition, breakProgress);
        Level level = plate.getLevel();
        BlockPos pos = plate.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        state.time = HexFx.time(level, partialTick) + ((int) pos.asLong() & 127);
        state.held.clear();
        if (level == null) {
            return;
        }
        for (int id : plate.held()) {
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive()) {
                Vec3 at = entity.getPosition(partialTick).subtract(origin);
                state.held.add(new Held(at, Math.max(entity.getBbWidth(), 0.6F), entity.getBbHeight()));
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
        for (Held held : state.held) {
            float pulse = 0.75F + 0.25F * Mth.sin(time * 0.15F);
            // The glyph under its feet, two rings turning against each other.
            poseStack.pushPose();
            poseStack.translate(held.at.x, TOP, held.at.z);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 2));
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            HexFx.sigilRing(poseStack, collector, held.width * 0.75F, 10, SHADOW, 0.7F * pulse);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-time * 3));
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            HexFx.sigilRing(poseStack, collector, held.width * 0.5F, 6, SHADOW, 0.5F * pulse);
            poseStack.popPose();
            poseStack.popPose();
            // Threads from the studs to the middle of the creature.
            Vec3 middle = held.at.add(0, held.height * 0.45, 0);
            for (double[] stud : STUDS) {
                Vec3 from = new Vec3(stud[0], TOP, stud[1]);
                Vec3 to = middle.subtract(from);
                poseStack.pushPose();
                poseStack.translate(from.x, from.y, from.z);
                Vec3 look = eye.subtract(from.add(to.scale(0.5))).normalize();
                HexFx.beam(poseStack, collector, (float) to.x, (float) to.y, (float) to.z, 0.03F, SHADOW, 0.55F * pulse,
                        new org.joml.Vector3f((float) look.x, (float) look.y, (float) look.z));
                poseStack.popPose();
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(RuneShacklesBlockEntity plate) {
        return RuneShacklesBlockEntity.space(plate.getBlockPos()).inflate(0.5);
    }

    record Held(Vec3 at, float width, float height) {
    }

    public static final class State extends BlockEntityRenderState {
        final List<Held> held = new ArrayList<>();
        float time;
    }
}

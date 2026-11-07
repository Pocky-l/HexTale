package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import com.pockyl.hextale.thevoid.boss.HexPlug;
import com.pockyl.hextale.thevoid.boss.Quill;

/** STAND-IN looks of the two small things of the fight: the lifted floor plug and the quills. */
public final class ProsectorProps {
    private static final int PLUG_SIDE = 0x3A2C48;
    private static final int PLUG_LIGHT = 0xE040C8;
    private static final int QUILL = 0x8CE0C0;

    private ProsectorProps() {
    }

    /** A hexagon of table: dark sides, a line of light round its underside so its fall can be judged. */
    public static final class Plug extends EntityRenderer<HexPlug, EntityRenderState> {
        private static final float RADIUS = 3.6F;
        private static final float HEIGHT = 1;

        public Plug(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public EntityRenderState createRenderState() {
            return new EntityRenderState();
        }

        @Override
        protected boolean affectedByCulling(HexPlug plug) {
            return false;
        }

        @Override
        public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            super.submit(state, poseStack, collector, camera);
            collector.submitCustomGeometry(poseStack, HexFx.glow(HexFx.WHITE), (pose, consumer) -> {
                for (int i = 0; i < 6; i++) {
                    float a0 = Mth.TWO_PI * i / 6;
                    float a1 = Mth.TWO_PI * (i + 1) / 6;
                    float x0 = Mth.cos(a0) * RADIUS;
                    float z0 = Mth.sin(a0) * RADIUS;
                    float x1 = Mth.cos(a1) * RADIUS;
                    float z1 = Mth.sin(a1) * RADIUS;
                    quad(consumer, pose, x0, 0, z0, x1, 0, z1, x1, HEIGHT, z1, x0, HEIGHT, z0);
                    quad(consumer, pose, 0, HEIGHT, 0, x0, HEIGHT, z0, x1, HEIGHT, z1, 0, HEIGHT, 0);
                    quad(consumer, pose, 0, 0, 0, x1, 0, z1, x0, 0, z0, 0, 0, 0);
                }
            });
            poseStack.pushPose();
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            HexFx.hexRing(poseStack, collector, RADIUS, PLUG_LIGHT, 0.9F);
            poseStack.popPose();
        }

        private static void quad(com.mojang.blaze3d.vertex.VertexConsumer consumer, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz,
                                 float cx, float cy, float cz, float dx, float dy, float dz) {
            HexFx.vertex(consumer, pose, ax, ay, az, 0, 1, PLUG_SIDE, 1);
            HexFx.vertex(consumer, pose, bx, by, bz, 1, 1, PLUG_SIDE, 1);
            HexFx.vertex(consumer, pose, cx, cy, cz, 1, 0, PLUG_SIDE, 1);
            HexFx.vertex(consumer, pose, dx, dy, dz, 0, 0, PLUG_SIDE, 1);
            HexFx.vertex(consumer, pose, dx, dy, dz, 0, 0, PLUG_SIDE, 1);
            HexFx.vertex(consumer, pose, cx, cy, cz, 1, 0, PLUG_SIDE, 1);
            HexFx.vertex(consumer, pose, bx, by, bz, 1, 1, PLUG_SIDE, 1);
            HexFx.vertex(consumer, pose, ax, ay, az, 0, 1, PLUG_SIDE, 1);
        }
    }

    /** A quill: a short stroke of light along its flight. */
    public static final class QuillLook extends EntityRenderer<Quill, QuillLook.State> {
        private static final float LENGTH = 1.4F;

        public static final class State extends EntityRenderState {
            Vec3 way = Vec3.ZERO;
        }

        public QuillLook(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(Quill quill, State state, float partialTick) {
            super.extractRenderState(quill, state, partialTick);
            state.way = quill.getDeltaMovement();
        }

        @Override
        public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            super.submit(state, poseStack, collector, camera);
            if (state.way.lengthSqr() < 1.0E-4) {
                return;
            }
            Vec3 tail = state.way.normalize().scale(-LENGTH);
            Vector3f viewer = new Vector3f((float) (camera.pos.x - state.x), (float) (camera.pos.y - state.y), (float) (camera.pos.z - state.z));
            HexFx.beam(poseStack, collector, (float) tail.x, (float) tail.y, (float) tail.z, 0.18F, QUILL, 0.9F, viewer);
        }
    }
}

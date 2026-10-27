package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.entity.GrappleHook;
import com.pockyl.hextale.registry.ModParticles;

/**
 * The Grapple Gauntlet's claw and its rope. The claw (a standalone model built pointing along -Z) points along its
 * flight, or into the face it bit. The rope runs from the claw's eye to the glove: a twisted brass-dark cord with a
 * sheath of hextech light. Slack, it sags in a curve as long as the rope really is (the length the wearer's client
 * reports); taut, it runs straight and its light brightens, with pulses running down to the glove.
 */
public class GrappleHookRenderer extends EntityRenderer<GrappleHook, GrappleHookRenderer.State> {
    private static final int SEGMENTS = 24;
    private static final float CORD_WIDTH = 0.05F;
    private static final float GLOW_WIDTH = 0.2F;
    private static final int CORD_DARK = 0x4A3520;
    private static final int CORD_LIGHT = 0x7A5C34;
    /** From the model's centre to its rope eye ((8, 8, 12) px), in blocks along +Z. */
    private static final float EYE = 4 / 16F;

    public GrappleHookRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    public static class State extends EntityRenderState {
        /** The glove, relative to the claw. */
        Vec3 hand = Vec3.ZERO;
        /** Where the claw points (unit). */
        Vec3 direction = new Vec3(0, 0, -1);
        float length = -1;
        byte phase;
        boolean visible;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(GrappleHook hook, Frustum frustum, double camX, double camY, double camZ, float partialTick) {
        return hook.owner() != null;
    }

    @Override
    protected boolean affectedByCulling(GrappleHook hook) {
        return false;
    }

    @Override
    public void extractRenderState(GrappleHook hook, State state, float partialTick) {
        super.extractRenderState(hook, state, partialTick);
        Player owner = hook.owner();
        state.visible = owner != null;
        if (owner == null) {
            return;
        }
        Vec3 at = hook.getPosition(partialTick);
        state.hand = handPos(owner, partialTick).subtract(at);
        state.phase = hook.state();
        state.length = (float) hook.length();
        if (hook.attached()) {
            state.direction = hook.face().getOpposite().getUnitVec3();
        } else {
            Vec3 away = state.hand.scale(-1);
            state.direction = away.lengthSqr() > 1.0E-4 ? away.normalize() : new Vec3(0, 0, -1);
        }
    }

    /** Where the glove holding the gauntlet is (as vanilla's fishing line finds the rod). */
    private Vec3 handPos(Player owner, float partialTick) {
        InteractionHand hand = owner.isUsingItem() ? owner.getUsedItemHand() : InteractionHand.MAIN_HAND;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? owner.getMainArm() : owner.getMainArm().getOpposite();
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && owner == net.minecraft.client.Minecraft.getInstance().player) {
            float fov = entityRenderDispatcher.options.fov().get().intValue();
            double scale = 960.0 / fov;
            Vec3 view = entityRenderDispatcher.camera.getNearPlane(fov).getPointOnPlane(invert * 0.525F, -0.1F).scale(scale);
            return owner.getEyePosition(partialTick).add(view);
        }
        float yaw = Mth.lerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        float playerScale = owner.getScale();
        double right = invert * 0.35 * playerScale;
        double forward = 0.6 * playerScale;
        double up = owner.isUsingItem() ? -0.1 : -0.45;
        return owner.getEyePosition(partialTick).add(-cos * right - sin * forward, up * playerScale, -sin * right + cos * forward);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) {
            return;
        }
        Vector3f dir = new Vector3f((float) state.direction.x, (float) state.direction.y, (float) state.direction.z);
        Quaternionf turn = new Quaternionf().rotationTo(new Vector3f(0, 0, -1), dir);
        // The claw: its centre a little behind the point it holds, so the prongs sink into the block.
        poseStack.pushPose();
        poseStack.translate(-dir.x * 0.25F, -dir.y * 0.25F, -dir.z * 0.25F);
        poseStack.rotate(turn);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        TableModels.submit(poseStack, collector, TableModels.GRAPPLE_HOOK, state.lightCoords);
        poseStack.popPose();

        Vec3 eye = state.direction.scale(-0.25 - EYE);
        Vec3 span = state.hand.subtract(eye);
        double distance = span.length();
        if (distance < 0.05) {
            return;
        }
        double sag = 0;
        boolean taut = false;
        if (state.phase == GrappleHook.ATTACHED && state.length > 0) {
            double slack = Math.max(0, state.length - distance);
            sag = Math.min(state.length * 0.5, Math.sqrt(3 * distance * slack / 8));
            taut = slack < 0.2;
        }
        Vec3[] points = new Vec3[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS;
            double wave = state.phase == GrappleHook.FLYING ? Mth.sin(t * 9 - state.ageInTicks * 2.2F) * 0.06 * t * (1 - t) * 4 : 0;
            points[i] = eye.add(span.scale(t)).add(0, -4 * sag * t * (1 - t) + wave, 0);
        }
        Vec3 toCamera = camera.pos.subtract(state.x, state.y, state.z);
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(HexFx.WHITE), (pose, consumer) -> {
            for (int i = 0; i < SEGMENTS; i++) {
                int color = i % 2 == 0 ? CORD_DARK : CORD_LIGHT;
                ribbon(consumer, pose, points[i], points[i + 1], toCamera, CORD_WIDTH, color, light);
            }
        });
        float time = state.ageInTicks;
        float base = taut ? 0.55F : state.phase == GrappleHook.ATTACHED ? 0.22F : 0.4F;
        for (int i = 0; i < SEGMENTS; i++) {
            float t = (i + 0.5F) / SEGMENTS;
            float pulse = taut ? Math.max(0, Mth.sin(t * 14 - time * 0.9F)) * 0.35F : 0;
            Vec3 from = points[i];
            Vec3 to = points[i + 1];
            Vec3 mid = from.add(to).scale(0.5);
            Vec3 look = toCamera.subtract(mid).normalize();
            poseStack.pushPose();
            poseStack.translate(from.x, from.y, from.z);
            HexFx.beam(poseStack, collector, (float) (to.x - from.x), (float) (to.y - from.y), (float) (to.z - from.z), GLOW_WIDTH,
                    ModParticles.HEXTECH, base + pulse, new Vector3f((float) look.x, (float) look.y, (float) look.z));
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }

    /** One stretch of cord as a flat strip turned to the camera, both windings. */
    private static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 camera, float width, int color, int light) {
        Vec3 along = b.subtract(a);
        Vec3 side = camera.subtract(a).cross(along);
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize().scale(width / 2);
        // Overlap the next stretch a hair so the bends close.
        Vec3 b2 = b.add(along.normalize().scale(width * 0.3));
        int argb = 0xFF000000 | color;
        Vec3[] quad = {a.subtract(side), a.add(side), b2.add(side), b2.subtract(side)};
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                Vec3 v = quad[pass == 0 ? k : 3 - k];
                consumer.addVertex(pose, (float) v.x, (float) v.y, (float) v.z).setColor(argb).setUv(k == 1 || k == 2 ? 1 : 0, k >= 2 ? 1 : 0)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
            }
        }
    }
}

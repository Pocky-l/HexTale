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
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(HexFx.WHITE), (pose, consumer) -> cord(consumer, pose, points, toCamera, light));
        // The sheath: one strip of additive light that writes no depth, lifted a hair towards the camera, so it neither
        // sorts against itself nor fights the cord for depth.
        float time = state.ageInTicks;
        float base = taut ? 0.55F : state.phase == GrappleHook.ATTACHED ? 0.22F : 0.4F;
        Vec3[] lifted = new Vec3[SEGMENTS + 1];
        float[] glow = new float[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS;
            float pulse = taut ? Math.max(0, Mth.sin(t * 14 - time * 0.9F)) * 0.35F : 0;
            glow[i] = base + pulse;
            lifted[i] = points[i].add(toCamera.subtract(points[i]).normalize().scale(0.02));
        }
        Vector3f viewer = new Vector3f((float) toCamera.x, (float) toCamera.y, (float) toCamera.z);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE,
                (pose, consumer) -> JarStreamFx.strip(consumer, pose, lifted, glow, viewer, GLOW_WIDTH, ModParticles.HEXTECH, 0.8F));
        super.submit(state, poseStack, collector, camera);
    }

    /**
     * The cord as one camera-facing strip, both windings. Neighbouring stretches share their joint's edge (mitred by
     * the direction through it), so no two stretches overlap and the alternating colours never fight for depth.
     */
    private static void cord(VertexConsumer consumer, PoseStack.Pose pose, Vec3[] points, Vec3 camera, int light) {
        Vec3[] side = new Vec3[points.length];
        for (int i = 0; i < points.length; i++) {
            Vec3 along = points[Math.min(points.length - 1, i + 1)].subtract(points[Math.max(0, i - 1)]);
            Vec3 across = camera.subtract(points[i]).cross(along);
            side[i] = across.lengthSqr() < 1.0E-10 ? (i > 0 ? side[i - 1] : new Vec3(0, CORD_WIDTH / 2, 0))
                    : across.normalize().scale(CORD_WIDTH / 2);
        }
        for (int i = 0; i < points.length - 1; i++) {
            int argb = 0xFF000000 | (i % 2 == 0 ? CORD_DARK : CORD_LIGHT);
            Vec3[] quad = {points[i].subtract(side[i]), points[i].add(side[i]), points[i + 1].add(side[i + 1]), points[i + 1].subtract(side[i + 1])};
            for (int pass = 0; pass < 2; pass++) {
                for (int k = 0; k < 4; k++) {
                    Vec3 v = quad[pass == 0 ? k : 3 - k];
                    consumer.addVertex(pose, (float) v.x, (float) v.y, (float) v.z).setColor(argb).setUv(k == 1 || k == 2 ? 1 : 0, k >= 2 ? 1 : 0)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
                }
            }
        }
    }
}

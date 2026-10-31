package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.pockyl.hextale.port.client.EntityRenderState;
import com.pockyl.hextale.port.client.ItemModelResolver;
import com.pockyl.hextale.port.client.ItemStackRenderState;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import com.pockyl.hextale.entity.ThrownDagger;
import com.pockyl.hextale.registry.ModParticles;

/**
 * A thrown Tendril Dagger: the dagger itself, spinning end over end in flight (still while it rests in a block), and the
 * tendril of hextech light that ties it to its thrower - a thin sheath that brightens and pulses while it comes back.
 */
public class ThrownDaggerRenderer extends EntityRenderer<ThrownDagger, ThrownDaggerRenderer.State> {
    private static final int SEGMENTS = 20;
    private static final float GLOW_WIDTH = 0.12F;
    private final ItemModelResolver itemModelResolver;

    public ThrownDaggerRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemModelResolver = com.pockyl.hextale.port.client.ItemModelResolver.INSTANCE;
        shadowRadius = 0;
    }

    public static class State extends EntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        /** The thrower's hand, relative to the dagger; null when nobody holds the other end. */
        Vec3 hand;
        float yaw;
        float pitch;
        boolean spinning;
        boolean returning;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ThrownDagger dagger, State state, float partialTick) {
        super.extractRenderState(dagger, state, partialTick);
        itemModelResolver.updateForNonLiving(state.item, dagger.dagger(), ItemDisplayContext.GROUND, dagger);
        state.yaw = dagger.getViewYRot(partialTick);
        state.pitch = dagger.getViewXRot(partialTick);
        state.spinning = !dagger.resting();
        state.returning = dagger.returning();
        Entity owner = dagger.getOwner();
        state.hand = owner == null ? null
                : owner.getEyePosition(partialTick).add(0, -0.45, 0).subtract(dagger.getPosition(partialTick));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch + (state.spinning ? state.ageInTicks * 45 : 45)));
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        if (state.hand != null && state.hand.lengthSqr() > 0.04) {
            Vec3[] points = new Vec3[SEGMENTS + 1];
            float[] glow = new float[SEGMENTS + 1];
            Vec3 toCamera = camera.pos.subtract(state.x, state.y, state.z);
            float base = state.returning ? 0.55F : 0.3F;
            for (int i = 0; i <= SEGMENTS; i++) {
                float t = i / (float) SEGMENTS;
                double wave = Mth.sin(t * 10 - state.ageInTicks * 1.8F) * 0.05 * t * (1 - t) * 4;
                points[i] = state.hand.scale(t).add(0, wave, 0);
                points[i] = points[i].add(toCamera.subtract(points[i]).normalize().scale(0.02));
                glow[i] = base + (state.returning ? Math.max(0, Mth.sin(t * 12 + state.ageInTicks * 0.9F)) * 0.3F : 0);
            }
            Vector3f viewer = new Vector3f((float) toCamera.x, (float) toCamera.y, (float) toCamera.z);
            collector.submitCustomGeometry(poseStack, HexGlow.TYPE,
                    (pose, consumer) -> JarStreamFx.strip(consumer, pose, points, glow, viewer, GLOW_WIDTH, ModParticles.HEXTECH, 0.8F));
        }
        super.submit(state, poseStack, collector, camera);
    }
}

package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.entity.BiterGrenade;

/**
 * A Biter Grenade. In flight the shut model, nose first and rolling; latched the open model with its mouth on the creature,
 * drawn at the creature's own place in the frame so it does not trail behind. The jaw snaps shut on each beat of the fuse,
 * and at the end of the fuse the grenade swells and shakes. The models have the mouth on their -Z end, centre (8, 7, 8) px.
 */
public class BiterGrenadeRenderer extends EntityRenderer<BiterGrenade, BiterGrenadeRenderer.State> {
    private static final float SCALE = 0.6F;
    /** From the model's centre to the teeth, a little short of the mouth so they sink in. */
    private static final float NOSE = 0.42F;
    private static final int SWELL_TICKS = 10;

    public BiterGrenadeRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    public static class State extends EntityRenderState {
        Vec3 facing = new Vec3(0, 0, -1);
        boolean armed;
        boolean held;
        boolean shut;
        float swell;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BiterGrenade grenade, State state, float partialTick) {
        super.extractRenderState(grenade, state, partialTick);
        int left = grenade.fuseLeft();
        LivingEntity target = grenade.target();
        state.armed = left >= 0;
        state.held = state.armed && target != null;
        state.shut = !state.armed || BiterGrenade.beat(left);
        state.swell = state.armed ? Mth.clamp(1 - (float) left / SWELL_TICKS, 0, 1) : 0;
        Vec3 facing = grenade.getDeltaMovement();
        if (state.held) {
            Vec3 at = grenade.heldAt(target, partialTick);
            state.x = at.x;
            state.y = at.y;
            state.z = at.z;
            facing = target.getPosition(partialTick).add(0, target.getBbHeight() / 2, 0).subtract(at);
        }
        if (facing.lengthSqr() > 1.0E-4) {
            state.facing = facing.normalize();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float scale = SCALE * (1 + 0.2F * state.swell * state.swell);
        Vector3f facing = state.facing.toVector3f();
        poseStack.pushPose();
        if (state.held) {
            poseStack.translate(-facing.x * NOSE * scale, -facing.y * NOSE * scale, -facing.z * NOSE * scale);
        }
        poseStack.rotate(new Quaternionf().rotationTo(new Vector3f(0, 0, -1), facing));
        float roll = state.armed ? Mth.sin(state.ageInTicks * 2.5F) * 8 * state.swell : state.ageInTicks * 18;
        poseStack.rotate(Axis.ZP.rotationDegrees(roll));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5F, -7 / 16F, -0.5F);
        TableModels.submit(poseStack, collector, state.shut ? TableModels.BITER_GRENADE : TableModels.BITER_GRENADE_OPEN, state.lightCoords);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}

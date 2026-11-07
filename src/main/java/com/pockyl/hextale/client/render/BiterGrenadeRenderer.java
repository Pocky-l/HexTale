package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.pockyl.hextale.port.client.EntityRenderState;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.entity.BiterGrenade;

/**
 * A Biter Grenade. In flight the shut model, nose first, rolling and drawn out by its speed; latched the open model with its
 * mouth on the creature, drawn at the creature's own place in the frame so it does not trail behind. On the ground it lands
 * flat, springs back, turns its mouth to what it hunts, works its jaw and crouches for the hop, all from the server's
 * {@link BiterGrenade#sitting() sitting} count. The jaw snaps shut on each beat of the fuse, and at the end of the fuse the
 * grenade swells and shakes. The models have the mouth on their -Z end, centre (8, 7, 8) px.
 */
public class BiterGrenadeRenderer extends EntityRenderer<BiterGrenade, BiterGrenadeRenderer.State> {
    private static final float SCALE = 0.6F;
    /** From the model's centre to the teeth, a little short of the mouth so they sink in. */
    private static final float NOSE = 0.42F;
    /** From the model's centre down to its underside, drawn at {@link #SCALE}. */
    private static final float BELLY = 0.225F;
    /** How flat it lands and crouches, and how far a hop draws it out, as shares of its size. */
    private static final float LANDING_SQUASH = 0.32F;
    private static final float CROUCH_SQUASH = 0.38F;
    private static final float HOP_STRETCH = 0.25F;
    /** The last ticks of the wind-up, when it crouches. */
    private static final float CROUCH_TICKS = 4;

    public BiterGrenadeRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    public static class State extends EntityRenderState {
        Vec3 facing = new Vec3(0, 0, -1);
        boolean armed;
        boolean held;
        boolean shut;
        boolean sitting;
        float swell;
        float squash;
        float stretch;
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
        int sitting = state.armed ? -1 : grenade.sitting();
        state.sitting = sitting >= 0;
        state.shut = state.sitting ? sitting > BiterGrenade.WINDUP || sitting % 4 < 2 : !state.armed || BiterGrenade.beat(left);
        state.swell = state.armed ? Mth.clamp(1 - (float) left / BiterGrenade.SWELL, 0, 1) : 0;
        Vec3 facing = grenade.getDeltaMovement();
        state.squash = 0;
        state.stretch = 0;
        if (state.sitting) {
            float landing = LANDING_SQUASH * (float) Math.exp(-(grenade.sat() + partialTick) * 0.45);
            float crouch = CROUCH_SQUASH * Mth.clamp(1 - (sitting - partialTick) / CROUCH_TICKS, 0, 1);
            state.squash = Math.max(landing, crouch);
            facing = grenade.shownLook(partialTick);
        } else if (!state.armed) {
            state.stretch = HOP_STRETCH * Mth.clamp((float) facing.length() / 0.7F, 0, 1);
        }
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
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, -1), facing));
        float roll = state.sitting ? 0 : state.armed ? Mth.sin(state.ageInTicks * 2.5F) * 8 * state.swell : state.ageInTicks * 18;
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        if (state.sitting) {
            // Flattens on its underside, so it stays on the ground.
            poseStack.translate(0, -BELLY * state.squash, 0);
        }
        float wide = 1 + state.squash * 0.5F - state.stretch * 0.4F;
        poseStack.scale(scale * wide, scale * (1 - state.squash - state.stretch * 0.4F), scale * (1 + state.stretch + state.squash * 0.5F));
        poseStack.translate(-0.5F, -7 / 16F, -0.5F);
        TableModels.submit(poseStack, collector, state.shut ? TableModels.BITER_GRENADE : TableModels.BITER_GRENADE_OPEN, state.lightCoords);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}

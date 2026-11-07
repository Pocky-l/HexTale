package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ShimmerMutations;
import com.pockyl.hextale.chem.ShimmerRide;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * How a mount's body leans, on top of the vanilla model: a Raptor in the air stretches out flat, beats its wings while it
 * climbs and holds them spread in a glide; a Panther on a wall rears up against it. Clients work it out from what they see of
 * the animal, so it needs no syncing: a rider's own client knows the wall, others read a Panther rising off the ground.
 * {@code LivingEntityRendererMixin} applies the lean, nose up in positive degrees, and the overlays follow.
 */
public final class ShimmerPose {
    private static final ContextKey<Float> PITCH = new ContextKey<>(Hextale.id("shimmer_pitch"));
    /** Degrees a body leans further in a tick. */
    private static final float STEP = 7;
    /** Raptor, nose down: less while it climbs than in {@link ShimmerRide#GLIDE_PITCH a glide}. */
    private static final float CLIMB_PITCH = -38;
    /** Vertical speed that counts as climbing (blocks a tick). */
    private static final double RISING = 0.02;
    /** Wing beat, in step with {@link ShimmerRide#FLAP_SOUND_TICKS}: radians a tick. */
    private static final float BEAT = Mth.TWO_PI / ShimmerRide.FLAP_SOUND_TICKS;
    private static final float BEAT_SWING = 1.05F;
    /** Wings held out to the side, in the model's flap angle. */
    private static final float SPREAD = 1.25F;
    private static final float WING_STEP = 0.2F;
    /** Panther on a wall: a little of the lean goes back into its head, so that it looks up the wall. */
    private static final float WALL_LOOK = 0.5F;
    /** Rise that reads as a Panther going up a wall on a client that cannot see the wall (blocks a tick). */
    private static final double WALL_RISE = 0.1;
    /** Per animal: pitch of the last tick, of this tick, the same for the wing beat. Client thread only. */
    private static final Map<Entity, float[]> LEANS = new WeakHashMap<>();

    private ShimmerPose() {
    }

    /** One client tick of a mutated animal. */
    public static void tick(LivingEntity animal, ShimmerMutations.Kind kind) {
        double rise = animal.getY() - animal.yo;
        boolean air = !animal.onGround();
        float pitch;
        float wing = 0;
        if (kind == ShimmerMutations.Kind.RAPTOR) {
            boolean climbing = air && rise > RISING;
            pitch = !air ? 0 : climbing ? CLIMB_PITCH : ShimmerRide.GLIDE_PITCH;
            wing = climbing ? 1 : 0;
        } else if (kind == ShimmerMutations.Kind.PANTHER) {
            pitch = air && (animal.onClimbable() || animal.isVehicle() && rise > WALL_RISE) ? ShimmerRide.WALL_PITCH : 0;
        } else {
            return;
        }
        float[] lean = LEANS.get(animal);
        if (lean == null) {
            if (pitch == 0) {
                return;
            }
            lean = new float[4];
            LEANS.put(animal, lean);
        }
        lean[0] = lean[1];
        lean[1] += Mth.clamp(pitch - lean[1], -STEP, STEP);
        lean[2] = lean[3];
        lean[3] += Mth.clamp(wing - lean[3], -WING_STEP, WING_STEP);
        if (lean[0] == 0 && lean[1] == 0) {
            LEANS.remove(animal);
        }
    }

    /** The lean of this tick, for the seat. Client thread only. */
    public static float pitch(Entity animal) {
        float[] lean = LEANS.get(animal);
        return lean == null ? 0 : lean[1];
    }

    private static float pitch(float[] lean, float partialTick) {
        return Mth.lerp(partialTick, lean[0], lean[1]);
    }

    /** The Raptor's lean, with the vanilla flying pose at a pace fit for its size and its head kept level. */
    public static void raptor(LivingEntity raptor, ParrotRenderState state) {
        float[] lean = LEANS.get(raptor);
        if (ShimmerMutations.of(raptor) == null || lean == null) {
            state.setRenderData(PITCH, null);
            return;
        }
        float pitch = pitch(lean, state.partialTick);
        float beating = Mth.lerp(state.partialTick, lean[2], lean[3]);
        state.setRenderData(PITCH, pitch);
        state.pose = ParrotModel.Pose.FLYING;
        state.flapAngle = Mth.lerp(beating, SPREAD, (Mth.sin(state.ageInTicks * BEAT) + 1) * BEAT_SWING);
        state.xRot += pitch;
    }

    /** The Panther's lean on a wall. */
    public static void panther(LivingEntity panther, CatRenderState state) {
        float[] lean = LEANS.get(panther);
        if (ShimmerMutations.of(panther) == null || lean == null) {
            state.setRenderData(PITCH, null);
            return;
        }
        float pitch = pitch(lean, state.partialTick);
        state.setRenderData(PITCH, pitch);
        state.xRot += pitch * WALL_LOOK;
    }

    /** At the end of the renderer's rotations: units are the unscaled model's in blocks, forward is -Z. */
    public static void apply(LivingEntityRenderState state, PoseStack pose) {
        Float pitch = state.getRenderData(PITCH);
        if (pitch == null) {
            return;
        }
        float pivot = state.boundingBoxHeight / state.scale * ShimmerRide.LEAN_PIVOT;
        float lift = pitch > 0 ? ShimmerRide.WALL_LIFT * pitch / ShimmerRide.WALL_PITCH : 0;
        pose.translate(0, pivot + lift, 0);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.translate(0, -pivot, 0);
    }
}

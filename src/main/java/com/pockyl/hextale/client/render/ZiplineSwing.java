package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.Zipline;
import com.pockyl.hextale.entity.ZiplineTrolley;

/**
 * A zipline rider's body swinging behind the grip when the speed changes. Drawn only: where the rider really is stays
 * the trolley's business. Kept apart from {@link ZiplinePose}, which loads with the arm pose enum and holds nothing else.
 */
public final class ZiplineSwing {
    /** Degrees, the line's direction x and z, and the grip's height over the feet in blocks. */
    public static final ContextKey<float[]> SWING = new ContextKey<>(Hextale.id("zipline_swing"));
    /** From the chain down to the bar the rider holds. */
    private static final double GRIP_BELOW_CHAIN = 0.35;
    private static final float TOO_SMALL = 0.05F;

    private ZiplineSwing() {
    }

    public static void extract(ZiplineTrolley trolley, AvatarRenderState state) {
        float degrees = trolley.swing(state.partialTick);
        if (Math.abs(degrees) < TOO_SMALL) {
            state.setRenderData(SWING, null);
            return;
        }
        Vec3 along = trolley.shownDirection(state.partialTick);
        float grip = (float) (Zipline.HANG - GRIP_BELOW_CHAIN - trolley.lift());
        state.setRenderData(SWING, new float[] {degrees, (float) along.x, (float) along.z, grip});
    }

    /** At the end of the renderer's rotations: turns the body round the grip, across the line, whichever way the rider looks. */
    public static void apply(LivingEntityRenderState state, PoseStack pose) {
        float[] swing = state.getRenderData(SWING);
        if (swing == null) {
            return;
        }
        // Across the line in the world; a positive angle moves the feet along it. The pose is already turned by the body's yaw.
        Vector3f axis = new Vector3f(-swing[2], 0, swing[1]).rotateY(-(180 - state.bodyRot) * Mth.DEG_TO_RAD);
        if (axis.lengthSquared() < 1.0E-6F) {
            return;
        }
        pose.translate(0, swing[3], 0);
        pose.rotate(new Quaternionf().rotationAxis(swing[0] * Mth.DEG_TO_RAD, axis.normalize()));
        pose.translate(0, -swing[3], 0);
    }
}

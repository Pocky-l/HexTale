package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.chem.Zipline;
import com.pockyl.hextale.entity.ZiplineTrolley;

/**
 * A zipline rider's body swinging behind the grip when the speed changes. Drawn only: where the rider really is stays
 * the trolley's business. Kept apart from {@link ZiplinePose}, which loads with the arm pose enum and holds nothing else.
 *
 * <p>1.21.1: read off the rider when it is drawn (26.x: render state data).
 */
public final class ZiplineSwing {
    /** From the chain down to the bar the rider holds. */
    private static final double GRIP_BELOW_CHAIN = 0.35;
    private static final float TOO_SMALL = 0.05F;

    private ZiplineSwing() {
    }

    /** At the end of the renderer's rotations: turns the body round the grip, across the line, whichever way the rider looks. */
    public static void apply(LivingEntity rider, PoseStack pose, float bodyRot, float partialTick) {
        if (!(rider.getVehicle() instanceof ZiplineTrolley trolley)) {
            return;
        }
        float degrees = trolley.swing(partialTick);
        if (Math.abs(degrees) < TOO_SMALL) {
            return;
        }
        Vec3 along = trolley.shownDirection(partialTick);
        float grip = (float) (Zipline.HANG - GRIP_BELOW_CHAIN - trolley.lift());
        // Across the line in the world; a positive angle moves the feet along it. The pose is already turned by the body's yaw.
        Vector3f axis = new Vector3f((float) -along.z, 0, (float) along.x).rotateY(-(180 - bodyRot) * Mth.DEG_TO_RAD);
        if (axis.lengthSquared() < 1.0E-6F) {
            return;
        }
        pose.translate(0, grip, 0);
        pose.mulPose(new Quaternionf().rotationAxis(degrees * Mth.DEG_TO_RAD, axis.normalize()));
        pose.translate(0, -grip, 0);
    }
}

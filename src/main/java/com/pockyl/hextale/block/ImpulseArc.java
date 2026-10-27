package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * The arc an aimed Impulse Plate throws along. Minecraft moves things in steps - add the speed, then take gravity and
 * air drag - and creatures and dropped items do it in a different order with different drag, so the throw is solved for
 * the thing being thrown: the speed that lands it on the target after exactly {@link #ticks} steps. Horizontal speed
 * decays by the same factor each step, so its sum is a geometric series; the vertical part is linear in the starting
 * speed, so one dry run without it gives the rest.
 */
public final class ImpulseArc {
    /** A creature's (and a player's) air drag: sideways, and up/down. */
    private static final double LIVING_DRAG = 0.91;
    private static final double LIVING_DRAG_Y = 0.98;
    private static final double ITEM_DRAG = 0.98;
    private static final double LIVING_GRAVITY = 0.08;
    /** The fastest throw the plate makes, blocks a tick. */
    private static final double MAX_SPEED = 4.0;

    private ImpulseArc() {
    }

    /** Where something thrown at {@code target} lands: the middle of the block's top. */
    public static Vec3 landing(BlockGetter level, BlockPos target) {
        BlockState state = level.getBlockState(target);
        VoxelShape shape = state.getCollisionShape(level, target);
        double top = shape.isEmpty() ? 0 : shape.max(net.minecraft.core.Direction.Axis.Y);
        return new Vec3(target.getX() + 0.5, target.getY() + top + 0.02, target.getZ() + 0.5);
    }

    /** Steps of flight for a throw over {@code distance} blocks: longer throws fly longer; Motor shortens them. */
    public static int ticks(double distance, int motor) {
        double base = 10 + 5 * Math.sqrt(distance);
        return Mth.clamp((int) Math.round(base * (1 - 0.08 * Mth.clamp(motor, 0, 4))), 8, 60);
    }

    /** The starting speed that carries {@code entity} from {@code from} to {@code to} in {@code ticks} steps. */
    public static Vec3 velocity(Entity entity, Vec3 from, Vec3 to, int ticks) {
        boolean living = !(entity instanceof ItemEntity) && entity instanceof LivingEntity;
        double gravity = entity.getGravity();
        if (entity instanceof LivingEntity creature && creature.hasEffect(MobEffects.SLOW_FALLING)) {
            gravity = Math.min(gravity, 0.01);
        }
        Vec3 speed = solve(from, to, ticks, gravity, living);
        if (entity instanceof ItemEntity item) {
            // An item's drag is taken once more in the very tick it is thrown (after the plate sets its speed), and on
            // the plate it is the ground's friction, not the air's.
            BlockPos below = BlockPos.containing(item.getX(), item.getY() - 0.5, item.getZ());
            double ground = item.onGround() ? ITEM_DRAG * item.level().getBlockState(below).getFriction(item.level(), below, item) : ITEM_DRAG;
            speed = new Vec3(speed.x / ground, speed.y / ITEM_DRAG, speed.z / ground);
        }
        return speed.length() > MAX_SPEED ? speed.normalize().scale(MAX_SPEED) : speed;
    }

    /** The speed for a living thing (true) or an item-like one (false) under {@code gravity}. */
    public static Vec3 solve(Vec3 from, Vec3 to, int ticks, double gravity, boolean living) {
        double drag = living ? LIVING_DRAG : ITEM_DRAG;
        double dragY = living ? LIVING_DRAG_Y : ITEM_DRAG;
        double sum = 0;
        double sumY = 0;
        double fall = 0;
        double vy = 0;
        for (int n = 0; n < ticks; n++) {
            sum += Math.pow(drag, n);
            sumY += Math.pow(dragY, n);
            if (living) {
                fall += vy;
                vy = (vy - gravity) * dragY;
            } else {
                vy -= gravity;
                fall += vy;
                vy *= dragY;
            }
        }
        Vec3 span = to.subtract(from);
        return new Vec3(span.x / sum, (span.y - fall) / sumY, span.z / sum);
    }

    /** The points a living thing passes through, one a step (for the preview and the trail). */
    public static List<Vec3> path(Vec3 from, Vec3 to, int ticks) {
        Vec3 speed = solve(from, to, ticks, LIVING_GRAVITY, true);
        List<Vec3> points = new ArrayList<>(ticks + 1);
        Vec3 at = from;
        points.add(at);
        double vx = speed.x;
        double vy = speed.y;
        double vz = speed.z;
        for (int n = 0; n < ticks; n++) {
            at = at.add(vx, vy, vz);
            points.add(at);
            vx *= LIVING_DRAG;
            vz *= LIVING_DRAG;
            vy = (vy - LIVING_GRAVITY) * LIVING_DRAG_Y;
        }
        return points;
    }
}

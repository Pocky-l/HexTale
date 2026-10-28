package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.block.SieveGlassBlock;
import com.pockyl.hextale.client.render.RuneFx;
import com.pockyl.hextale.network.RuneFxPayload;

/**
 * A barrier answers the one pushing against it: when the local player walks into Ward Glass (or Veil Glass that keeps
 * them out), a small hexagon of light ripples out on the glass where they touch it - at most every few ticks. Creatures
 * (and other players) pressing against glass that stops them ripple it too: other entities only move by interpolation on
 * this side, so a creature counts as pressing when its body touches the glass and it faces it; it ripples the glass
 * when it comes up against it, then only now and then while it stays there (a pen walled with Veil Glass stays calm).
 */
public final class WardRipple {
    private static final int GAP_TICKS = 8;
    /** The ward's amber, as its glass. */
    private static final int AMBER = 0xF0C060;
    /** The veil's pale mist. */
    private static final int MIST = 0xD6E2F2;
    /** Creatures are looked at every few ticks, within this many blocks of the player, each rippling at most so often. */
    private static final int CREATURE_EVERY = 4;
    private static final double CREATURE_RANGE = 24;
    private static final int CREATURE_GAP = 60;
    private static long next;
    /** Creatures touching glass at the last look, with the tick each may ripple it again. */
    private static java.util.Map<Integer, Long> touching = new java.util.HashMap<>();

    private WardRipple() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        if (now % CREATURE_EVERY == 0) {
            creatures(minecraft.level, player, now);
        }
        if (!player.horizontalCollision || now < next) {
            return;
        }
        Vec3 look = player.getLookAngle();
        double half = player.getBbWidth() / 2;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (look.x * direction.getStepX() + look.z * direction.getStepZ() < 0.3) {
                continue;
            }
            for (double up : new double[] {0.4, 1.4}) {
                Vec3 touch = player.position().add(direction.getStepX() * (half + 0.05), up, direction.getStepZ() * (half + 0.05));
                BlockState state = minecraft.level.getBlockState(BlockPos.containing(touch));
                if (state.getBlock() instanceof SieveGlassBlock glass && !glass.passes(player)) {
                    Vec3 at = player.position().add(direction.getStepX() * (half + 0.02), 1.1, direction.getStepZ() * (half + 0.02));
                    ripple(at, direction, 0.7F, glass);
                    next = minecraft.level.getGameTime() + GAP_TICKS;
                    return;
                }
            }
        }
    }

    /** Creatures and other players near the local one touching, and facing, glass that stops them. */
    private static void creatures(net.minecraft.world.level.Level level, Player me, long now) {
        java.util.Map<Integer, Long> still = new java.util.HashMap<>();
        for (net.minecraft.world.entity.LivingEntity entity : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                me.getBoundingBox().inflate(CREATURE_RANGE), entity -> entity != me && entity.isAlive() && !entity.isSpectator())) {
            float yaw = entity.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
            double facingX = -net.minecraft.util.Mth.sin(yaw);
            double facingZ = net.minecraft.util.Mth.cos(yaw);
            double half = entity.getBbWidth() / 2;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (facingX * direction.getStepX() + facingZ * direction.getStepZ() < 0.5) {
                    continue;
                }
                double up = Math.min(entity.getBbHeight() * 0.5, 1.2);
                Vec3 touch = entity.position().add(direction.getStepX() * (half + 0.08), up, direction.getStepZ() * (half + 0.08));
                BlockState state = level.getBlockState(BlockPos.containing(touch));
                if (state.getBlock() instanceof SieveGlassBlock glass && !glass.passes(entity)) {
                    // On the glass's face, where the body meets it.
                    BlockPos glassPos = BlockPos.containing(touch);
                    Direction face = direction.getOpposite();
                    double plane = direction.getStepX() != 0
                            ? glassPos.getX() + (face.getStepX() > 0 ? 1 : 0) + face.getStepX() * 0.02
                            : glassPos.getZ() + (face.getStepZ() > 0 ? 1 : 0) + face.getStepZ() * 0.02;
                    Vec3 at = direction.getStepX() != 0 ? new Vec3(plane, entity.getY() + up, entity.getZ())
                            : new Vec3(entity.getX(), entity.getY() + up, plane);
                    Long again = touching.get(entity.getId());
                    if (again == null || now >= again) {
                        ripple(at, direction, Math.max(0.5F, Math.min(1.1F, entity.getBbHeight() * 0.45F)), glass);
                        again = now + CREATURE_GAP;
                    }
                    still.put(entity.getId(), again);
                    break;
                }
            }
        }
        touching = still;
    }

    private static void ripple(Vec3 at, Direction direction, float size, SieveGlassBlock glass) {
        RuneFx.local(RuneFxPayload.ripple(at, direction, size, glass.stopsPlayers() ? AMBER : MIST));
    }
}
